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

package org.iromu.openfeature.boot.autoconfigure.optimizely;

import java.util.concurrent.TimeUnit;

import com.optimizely.ab.config.HttpProjectConfigManager;
import dev.openfeature.contrib.providers.optimizely.OptimizelyProvider;
import dev.openfeature.contrib.providers.optimizely.OptimizelyProviderConfig;
import dev.openfeature.sdk.FeatureProvider;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.optimizely.OptimizelyCustomizer;
import org.iromu.openfeature.boot.optimizely.OptimizelyProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for {@link OptimizelyProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ OptimizelyProvider.class })
@ConditionalOnProperty(prefix = OptimizelyProperties.OPTIMIZELY_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(OptimizelyProperties.class)
@Slf4j
public class OptimizelyAutoConfiguration {

	/**
	 * Builds the {@link OptimizelyProviderConfig} used to create the Optimizely feature
	 * provider.
	 * @param customizers the customizers applied to the config builder
	 * @param optimizelyProperties the Optimizely configuration properties
	 * @return the configured Optimizely provider config
	 */
	@Bean
	@ConditionalOnMissingBean
	public OptimizelyProviderConfig optimizelyProviderConfig(ObjectProvider<OptimizelyCustomizer> customizers,
			OptimizelyProperties optimizelyProperties) {

		OptimizelyProviderConfig.OptimizelyProviderConfigBuilder builder = OptimizelyProviderConfig.builder();

		if (optimizelyProperties.getSdkKey() != null) {
			HttpProjectConfigManager.Builder managerBuilder = HttpProjectConfigManager.builder()
				.withSdkKey(optimizelyProperties.getSdkKey());
			if (optimizelyProperties.getDatafileAuthToken() != null) {
				managerBuilder.withDatafileAccessToken(optimizelyProperties.getDatafileAuthToken());
			}
			if (optimizelyProperties.getConfigUrl() != null) {
				managerBuilder.withUrl(optimizelyProperties.getConfigUrl());
			}
			if (optimizelyProperties.getPollInterval() != null) {
				managerBuilder.withPollingInterval(optimizelyProperties.getPollInterval(), TimeUnit.SECONDS);
			}
			builder.projectConfigManager(managerBuilder.build());
		}

		if (optimizelyProperties.getDatafile() != null) {
			builder.datafile(optimizelyProperties.getDatafile());
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return builder.build();
	}

	/**
	 * Creates the {@link FeatureProvider} backed by the supplied Optimizely
	 * configuration.
	 * @param optimizelyProviderConfig the Optimizely provider config to build the
	 * provider from
	 * @return the Optimizely feature provider
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider optimizelyProvider(OptimizelyProviderConfig optimizelyProviderConfig) {
		return new OptimizelyProvider(optimizelyProviderConfig);
	}

}
