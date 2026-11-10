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

package org.iromu.openfeature.boot.autoconfigure.configdirector;

import java.util.function.Consumer;

import com.configdirector.ClientOptions;
import com.configdirector.openfeature.ConfigDirectorProvider;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.configdirector.ConfigDirectorCustomizer;
import org.iromu.openfeature.boot.configdirector.ConfigDirectorProperties;
import org.iromu.openfeature.boot.configdirector.ConfigDirectorProviderOptions;

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
 * Autoconfiguration for the ConfigDirector {@link ConfigDirectorProvider}. ConfigDirector
 * is a remote configuration and feature-flag service: the provider connects to the
 * endpoint named by {@code spring.openfeature.configdirector.url} (the ConfigDirector
 * default when unset) using the server SDK key from
 * {@code spring.openfeature.configdirector.api-key}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ ConfigDirectorProvider.class })
@ConditionalOnProperty(prefix = ConfigDirectorProperties.CONFIGDIRECTOR_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(ConfigDirectorProperties.class)
public class ConfigDirectorAutoConfiguration {

	/**
	 * Creates the {@link ConfigDirectorProviderOptions} assembled from the configured
	 * {@link ConfigDirectorProperties}.
	 * <p>
	 * Fails fast when the starter is enabled but the API key is missing, before any
	 * vendor call.
	 * @param properties the ConfigDirectorProperties holding the API key and endpoint
	 * @return the configured ConfigDirectorProviderOptions instance
	 */
	@Bean
	@ConditionalOnMissingBean({ ConfigDirectorProviderOptions.class, FeatureProvider.class })
	public ConfigDirectorProviderOptions configDirectorProviderOptions(ConfigDirectorProperties properties) {
		requireText(properties.getApiKey(), ConfigDirectorProperties.CONFIGDIRECTOR_PREFIX + ".api-key");
		return new ConfigDirectorProviderOptions(properties.getApiKey(), properties.getUrl());
	}

	/**
	 * Creates the {@link FeatureProvider} that resolves flags from the assembled
	 * ConfigDirector configuration. The provider constructor is lazy: it connects to the
	 * ConfigDirector endpoint only when OpenFeature initializes it.
	 * @param configDirectorProviderOptions the ConfigDirectorProviderOptions the provider
	 * is built from
	 * @param customizers the ordered provider of ConfigDirectorCustomizer beans
	 * @return the ConfigDirector feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider configDirectorProvider(ConfigDirectorProviderOptions configDirectorProviderOptions,
			ObjectProvider<ConfigDirectorCustomizer> customizers) {
		Consumer<ClientOptions> optionsConsumer = (options) -> {
			if (configDirectorProviderOptions.getUrl() != null) {
				options.connection((connection) -> connection.url(configDirectorProviderOptions.getUrl()));
			}
			customizers.orderedStream().forEach((customizer) -> customizer.customize(options));
		};
		return new ConfigDirectorProvider(configDirectorProviderOptions.getApiKey(), optionsConsumer);
	}

	private static void requireText(String value, String property) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalStateException("ConfigDirector requires '" + property + "' when '"
					+ ConfigDirectorProperties.CONFIGDIRECTOR_PREFIX + ".enabled=true'");
		}
	}

}
