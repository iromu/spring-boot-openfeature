/*
 * Copyright 2025-2025 the original author or authors.
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

package org.iromu.openfeature.boot.autoconfigure.flipt;

import dev.openfeature.contrib.providers.flipt.FliptProvider;
import dev.openfeature.contrib.providers.flipt.FliptProviderConfig;
import dev.openfeature.sdk.FeatureProvider;
import io.flipt.api.FliptClient;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.flipt.FliptCustomizer;
import org.iromu.openfeature.boot.flipt.FliptProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for {@link FliptProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ FliptProvider.class })
@ConditionalOnProperty(prefix = FliptProperties.FLIPT_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(FliptProperties.class)
@Slf4j
public class FliptAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public FliptProviderConfig fliptProviderConfig(ObjectProvider<FliptCustomizer> customizers,
			FliptProperties properties) {
		FliptClient.FliptClientBuilder fliptClientBuilder = FliptClient.builder()
			.url((properties.getBaseURL() != null && properties.getBaseURL().endsWith("/"))
					? properties.getBaseURL().substring(0, properties.getBaseURL().length() - 1) : null)
			.timeout(properties.getTimeout())
			.headers(properties.getHeaders());

		FliptProviderConfig.FliptProviderConfigBuilder fliptProviderConfigBuilder = FliptProviderConfig.builder()
			.fliptClientBuilder(fliptClientBuilder)
			.namespace(properties.getNamespace());

		customizers.orderedStream().forEach((customizer) -> customizer.customize(fliptProviderConfigBuilder));

		return fliptProviderConfigBuilder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider fliptProvider(FliptProviderConfig fliptProviderConfig) {
		return new FliptProvider(fliptProviderConfig);
	}

}
