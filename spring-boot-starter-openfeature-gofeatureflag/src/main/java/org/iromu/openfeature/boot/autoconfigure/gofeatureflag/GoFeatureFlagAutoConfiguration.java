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

package org.iromu.openfeature.boot.autoconfigure.gofeatureflag;

import dev.openfeature.contrib.providers.gofeatureflag.GoFeatureFlagProvider;
import dev.openfeature.contrib.providers.gofeatureflag.GoFeatureFlagProviderOptions;
import dev.openfeature.sdk.FeatureProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.gofeatureflag.GoFeatureFlagCustomizer;
import org.iromu.openfeature.boot.gofeatureflag.GoFeatureFlagProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for {@link GoFeatureFlagProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ GoFeatureFlagProvider.class })
@ConditionalOnProperty(prefix = GoFeatureFlagProperties.GOFEATUREFLAG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(GoFeatureFlagProperties.class)
@Slf4j
public class GoFeatureFlagAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public GoFeatureFlagProviderOptions gofeatureflagProviderOptions(
			ObjectProvider<GoFeatureFlagCustomizer> customizers, GoFeatureFlagProperties gofeatureflagProperties) {
		GoFeatureFlagProviderOptions.GoFeatureFlagProviderOptionsBuilder builder = GoFeatureFlagProviderOptions
			.builder()
			.endpoint(gofeatureflagProperties.getEndpoint())
			.apiKey(gofeatureflagProperties.getApiKey());

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return builder.build();
	}

	@SneakyThrows
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider goFeatureFlagProvider(GoFeatureFlagProviderOptions gofeatureflagProviderOptions) {
		return new GoFeatureFlagProvider(gofeatureflagProviderOptions);
	}

}
