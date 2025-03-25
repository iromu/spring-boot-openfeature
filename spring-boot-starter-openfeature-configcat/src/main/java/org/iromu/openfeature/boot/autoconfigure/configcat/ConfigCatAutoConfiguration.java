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

package org.iromu.openfeature.boot.autoconfigure.configcat;

import java.io.IOException;

import com.configcat.OverrideBehaviour;
import com.configcat.OverrideDataSourceBuilder;
import dev.openfeature.contrib.providers.configcat.ConfigCatProvider;
import dev.openfeature.contrib.providers.configcat.ConfigCatProviderConfig;
import dev.openfeature.sdk.FeatureProvider;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.configcat.ConfigCatCustomizer;
import org.iromu.openfeature.boot.configcat.ConfigCatProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Autoconfiguration for {@link ConfigCatProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ ConfigCatProvider.class })
@ConditionalOnProperty(prefix = ConfigCatProperties.CONFIGCAT_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(ConfigCatProperties.class)
@Slf4j
public class ConfigCatAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ConfigCatProviderConfig configCatProviderConfig(ObjectProvider<ConfigCatCustomizer> customizers,
			ConfigCatProperties properties) {

		ConfigCatProviderConfig.ConfigCatProviderConfigBuilder builder = ConfigCatProviderConfig.builder();

		Resource filename = properties.getFilename();
		if (filename != null) {
			if (filename instanceof ClassPathResource) {
				builder.options((options) -> options.flagOverrides(
						OverrideDataSourceBuilder.classPathResource(((ClassPathResource) filename).getPath()),
						OverrideBehaviour.LOCAL_ONLY));
			}
			else {
				builder.options((options) -> {
					try {
						options.flagOverrides(OverrideDataSourceBuilder.localFile(filename.getURI().getPath(), true),
								OverrideBehaviour.LOCAL_ONLY);
					}
					catch (IOException ex) {
						log.error(ex.getMessage(), ex);
					}
				});
			}
		}

		customizers.orderedStream().forEach((customizer) -> builder.options(customizer::customize));
		return builder.sdkKey(properties.getSdkKey()).build();
	}

	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider configCatProvider(ConfigCatProviderConfig configCatProviderConfig) {
		return new ConfigCatProvider(configCatProviderConfig);
	}

}
