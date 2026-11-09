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

package org.iromu.openfeature.boot.autoconfigure.featureflip;

import dev.openfeature.sdk.FeatureProvider;
import io.featureflip.client.FeatureFlagConfig;
import io.featureflip.openfeature.FeatureflipProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.featureflip.FeatureflipCustomizer;
import org.iromu.openfeature.boot.featureflip.FeatureflipProperties;
import org.iromu.openfeature.boot.featureflip.FeatureflipProviderConfig;

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
 * Autoconfiguration for the Featureflip {@link FeatureflipProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ FeatureflipProvider.class })
@ConditionalOnProperty(prefix = FeatureflipProperties.FEATUREFLIP_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(FeatureflipProperties.class)
public class FeatureflipAutoConfiguration {

	/**
	 * Creates the {@link FeatureflipProviderConfig} assembled from the configured
	 * {@link FeatureflipProperties}, applying every available
	 * {@link FeatureflipCustomizer}.
	 * @param customizers the ordered provider of FeatureflipCustomizer beans
	 * @param properties the FeatureflipProperties holding the connection settings
	 * @return the assembled FeatureflipProviderConfig
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureflipProviderConfig featureflipProviderConfig(ObjectProvider<FeatureflipCustomizer> customizers,
			FeatureflipProperties properties) {
		FeatureflipProviderConfig config = new FeatureflipProviderConfig();
		config.setApiKey(properties.getApiKey());
		config.setBaseUrl(properties.getBaseUrl());

		customizers.orderedStream().forEach((customizer) -> customizer.customize(config));

		return config;
	}

	/**
	 * Creates the Featureflip {@link FeatureProvider} from the assembled configuration.
	 * Fails fast when the required API key is missing; backs off entirely when a
	 * consumer-supplied {@link FeatureProvider} is present, so the API key is only
	 * required for the starter's own provider.
	 * @param config the assembled FeatureflipProviderConfig
	 * @return the Featureflip feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider featureflipProvider(FeatureflipProviderConfig config) {
		if (!StringUtils.hasText(config.getApiKey())) {
			throw new IllegalStateException(
					"Featureflip feature flags require '" + FeatureflipProperties.FEATUREFLIP_PREFIX
							+ ".api-key' when '" + FeatureflipProperties.FEATUREFLIP_PREFIX + ".enabled=true'");
		}
		FeatureFlagConfig.Builder builder = FeatureFlagConfig.builder();
		if (StringUtils.hasText(config.getBaseUrl())) {
			builder.baseUrl(config.getBaseUrl());
		}
		return new FeatureflipProvider(config.getApiKey(), builder.build());
	}

}
