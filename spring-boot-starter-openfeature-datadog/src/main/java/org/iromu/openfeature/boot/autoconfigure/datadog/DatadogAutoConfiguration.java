/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.iromu.openfeature.boot.autoconfigure.datadog;

import java.util.concurrent.TimeUnit;

import datadog.trace.api.openfeature.Provider;
import dev.openfeature.sdk.FeatureProvider;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.datadog.DatadogCustomizer;
import org.iromu.openfeature.boot.datadog.DatadogProperties;
import org.iromu.openfeature.boot.datadog.DatadogProviderConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * Autoconfiguration for the Datadog {@link Provider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ Provider.class })
@ConditionalOnProperty(prefix = DatadogProperties.DATADOG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(DatadogProperties.class)
@Slf4j
public class DatadogAutoConfiguration {

	/**
	 * Creates the {@link DatadogProviderConfig} assembled from the configured
	 * {@link DatadogProperties}, applying every available {@link DatadogCustomizer}.
	 * @param customizers the ordered provider of DatadogCustomizer beans
	 * @param properties the DatadogProperties holding the connection settings
	 * @return the assembled DatadogProviderConfig
	 */
	@Bean
	@ConditionalOnMissingBean
	public DatadogProviderConfig datadogProviderConfig(ObjectProvider<DatadogCustomizer> customizers,
			DatadogProperties properties) {
		DatadogProviderConfig config = new DatadogProviderConfig();
		config.setApiKey(properties.getApiKey());
		config.setAppKey(properties.getAppKey());
		config.setSite(properties.getSite());
		config.setEnvironment(properties.getEnvironment());
		config.setInitTimeout(properties.getInitTimeout());
		config.setEvaluationTelemetryOptOut(properties.isEvaluationTelemetryOptOut());

		customizers.orderedStream().forEach((customizer) -> customizer.customize(config));

		return config;
	}

	/**
	 * Creates the Datadog {@link FeatureProvider} from the assembled configuration. The
	 * bound credentials are published as the Datadog tracer settings ({@code DD_API_KEY},
	 * {@code DD_SITE}, {@code DD_ENV}) that the provider reads, without overriding values
	 * already present in the environment. Fails fast when the required credentials are
	 * missing; backs off entirely when a consumer-supplied {@link FeatureProvider} is
	 * present, so the credentials are only required for the starter's own provider.
	 * @param config the assembled DatadogProviderConfig
	 * @return the Datadog feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider datadogProvider(DatadogProviderConfig config) {
		validateCredentials(config);
		publishTracerSetting("DD_API_KEY", config.getApiKey());
		publishTracerSetting("DD_SITE", config.getSite());
		publishTracerSetting("DD_ENV", config.getEnvironment());

		Provider.Options options = new Provider.Options()
			.initTimeout(config.getInitTimeout().toSeconds(), TimeUnit.SECONDS)
			.enableEvaluationTelemetryOptOutParameter(config.isEvaluationTelemetryOptOut());
		log.info("Creating Datadog OpenFeature provider (site: {}, environment: {}, init timeout: {}s)",
				config.getSite(), config.getEnvironment(), config.getInitTimeout().toSeconds());
		return new Provider(options);
	}

	private void validateCredentials(DatadogProviderConfig config) {
		if (!StringUtils.hasText(config.getApiKey()) || !StringUtils.hasText(config.getAppKey())
				|| !StringUtils.hasText(config.getSite())) {
			throw new IllegalStateException(
					"Datadog feature flags require 'spring.openfeature.datadog.api-key', 'app-key' and 'site' "
							+ "when 'spring.openfeature.datadog.enabled=true'");
		}
	}

	private void publishTracerSetting(String name, String value) {
		if (value != null && System.getProperty(name) == null && System.getenv(name) == null) {
			System.setProperty(name, value);
		}
	}

}
