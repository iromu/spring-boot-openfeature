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

package org.iromu.openfeature.boot.autoconfigure.flagsmith;

import dev.openfeature.contrib.providers.flagsmith.FlagsmithProvider;
import dev.openfeature.contrib.providers.flagsmith.FlagsmithProviderOptions;
import dev.openfeature.sdk.FeatureProvider;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.flagsmith.FlagsmithCustomizer;
import org.iromu.openfeature.boot.flagsmith.FlagsmithProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for {@link FlagsmithProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ FlagsmithProvider.class })
@ConditionalOnProperty(prefix = FlagsmithProperties.FLAGSMITH_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(FlagsmithProperties.class)
@Slf4j
public class FlagsmithAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public FlagsmithProviderOptions flagsmithProviderOptions(ObjectProvider<FlagsmithCustomizer> customizers,
			FlagsmithProperties flagsmithProperties) {
		FlagsmithProviderOptions.FlagsmithProviderOptionsBuilder builder = FlagsmithProviderOptions.builder()
			.apiKey(flagsmithProperties.getApiKey());

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider flagsmithProvider(FlagsmithProviderOptions flagsmithProviderOptions) {
		return new FlagsmithProvider(flagsmithProviderOptions);
	}

}
