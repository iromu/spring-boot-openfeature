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

package org.iromu.openfeature.boot.autoconfigure.ageris;

import java.nio.file.FileSystems;

import dev.openfeature.sdk.FeatureProvider;
import io.github.lichtsinn.ageris.core.Ageris;
import io.github.lichtsinn.ageris.core.ConfigurationLoader;
import io.github.lichtsinn.ageris.core.FeatureFlagManager;
import io.github.lichtsinn.ageris.core.JsonConfigManager;
import io.github.lichtsinn.ageris.core.filesystem.ConfigurationFetcherMetrics;
import io.github.lichtsinn.ageris.core.filesystem.FilesystemConfigurationFetcher;
import io.github.lichtsinn.ageris.openfeature.AgerisProvider;
import org.iromu.openfeature.boot.ageris.AgerisCustomizer;
import org.iromu.openfeature.boot.ageris.AgerisProperties;
import org.iromu.openfeature.boot.ageris.AgerisProviderOptions;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;

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
 * Autoconfiguration for the Ageris {@link AgerisProvider}. Ageris is a file-based
 * platform: the provider reads feature flags and JSON configs from the configuration
 * files in the directory named by {@code spring.openfeature.ageris.path}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ AgerisProvider.class })
@ConditionalOnProperty(prefix = AgerisProperties.AGERIS_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(AgerisProperties.class)
public class AgerisAutoConfiguration {

	/**
	 * Creates the {@link AgerisProviderOptions} assembled from the configured
	 * {@link AgerisProperties}: the Ageris instance (after every available
	 * {@link AgerisCustomizer} ran), the feature-flag and JSON-config managers registered
	 * with their files, and the not-yet-started configuration loader.
	 * <p>
	 * Fails fast when the starter is enabled but a required property is missing.
	 * @param customizers the ordered provider of AgerisCustomizer beans
	 * @param properties the AgerisProperties holding the file-based settings
	 * @return the configured AgerisProviderOptions instance
	 */
	@Bean
	@ConditionalOnMissingBean({ AgerisProviderOptions.class, FeatureProvider.class })
	public AgerisProviderOptions agerisProviderOptions(ObjectProvider<AgerisCustomizer> customizers,
			AgerisProperties properties) {
		requireText(properties.getApplication(), AgerisProperties.AGERIS_PREFIX + ".application");
		requireText(properties.getPath(), AgerisProperties.AGERIS_PREFIX + ".path");
		if (properties.getFeatureFlagFiles().isEmpty()) {
			throw new IllegalStateException(AgerisProperties.AGERIS_PREFIX + ".feature-flag-files requires"
					+ " at least one feature-flag file name when '" + AgerisProperties.AGERIS_PREFIX
					+ ".enabled=true'");
		}

		Ageris ageris = new Ageris(new FilesystemConfigurationFetcher(FileSystems.getDefault(), properties.getPath(),
				false, new ConfigurationFetcherMetrics()));
		customizers.orderedStream().forEach((customizer) -> customizer.customize(ageris));

		Ageris.FeatureFlagManagerBuilder featureFlags = ageris.addFeatureFlagManager(properties.getApplication());
		properties.getFeatureFlagFiles().forEach(featureFlags::registerFile);
		FeatureFlagManager featureFlagManager = featureFlags.build();

		Ageris.JsonConfigManagerBuilder jsonConfigs = ageris.addJsonConfigManager(properties.getApplication());
		properties.getJsonConfigFiles().forEach(jsonConfigs::registerFile);
		JsonConfigManager jsonConfigManager = jsonConfigs.build();

		ConfigurationLoader configurationLoader = ageris.createLoader(properties.getRefreshInterval());

		return new AgerisProviderOptions(featureFlagManager, jsonConfigManager, configurationLoader,
				properties.getTargetingKeyType(), properties.getFirstLoadTimeout());
	}

	/**
	 * Creates the {@link FeatureProvider} that resolves flags from the assembled Ageris
	 * configuration. The provider starts and stops the configuration loader with
	 * OpenFeature.
	 * @param agerisProviderOptions the AgerisProviderOptions the provider is built from
	 * @return the Ageris feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider agerisProvider(AgerisProviderOptions agerisProviderOptions) {
		AgerisProvider.Builder builder = AgerisProvider
			.builder(agerisProviderOptions.getFeatureFlagManager(), agerisProviderOptions.getJsonConfigManager())
			.loader(agerisProviderOptions.getConfigurationLoader())
			.targetingKeyType(agerisProviderOptions.getTargetingKeyType())
			.firstLoadTimeout(agerisProviderOptions.getFirstLoadTimeout());
		return builder.build();
	}

	private static void requireText(String value, String property) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalStateException(
					"Ageris requires '" + property + "' when '" + AgerisProperties.AGERIS_PREFIX + ".enabled=true'");
		}
	}

}
