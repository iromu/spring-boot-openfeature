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

package org.iromu.openfeature.boot.autoconfigure.unleash;

import java.io.IOException;

import dev.openfeature.contrib.providers.unleash.UnleashProvider;
import dev.openfeature.contrib.providers.unleash.UnleashProviderConfig;
import dev.openfeature.sdk.FeatureProvider;
import io.getunleash.util.UnleashConfig;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.unleash.UnleashCustomizer;
import org.iromu.openfeature.boot.unleash.UnleashProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for {@link UnleashProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ UnleashProvider.class })
@ConditionalOnProperty(prefix = UnleashProperties.UNLEASH_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(UnleashProperties.class)
@Slf4j
public class UnleashAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public UnleashProviderConfig unleashProviderConfig(ObjectProvider<UnleashCustomizer> customizers,
			UnleashProperties unleashProperties) {
		UnleashConfig.Builder unleashConfigBuilder = UnleashConfig.builder()
			.unleashAPI(unleashProperties.getUnleashAPI())
			.appName(unleashProperties.getAppName());

		if (unleashProperties.getUnleashToken() != null) {
			unleashConfigBuilder.customHttpHeader("Authorization", unleashProperties.getUnleashToken());
		}

		if (unleashProperties.getBackupFile() != null) {
			try {
				// NOTE: Unleash uses FileWriter
				unleashConfigBuilder.backupFile(unleashProperties.getBackupFile().getFile().getAbsolutePath());
			}
			catch (IOException ex) {
				log.error(ex.getMessage());
			}
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(unleashConfigBuilder));

		return UnleashProviderConfig.builder().unleashConfigBuilder(unleashConfigBuilder).build();
	}

	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider unleashProvider(UnleashProviderConfig unleashProviderConfig) {
		return new UnleashProvider(unleashProviderConfig);
	}

}
