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

package org.iromu.openfeature.boot.autoconfigure.octopusdeploy;

import com.octopus.openfeature.provider.OctopusFeatureConfiguration;
import com.octopus.openfeature.provider.OctopusFeatureProvider;
import com.octopus.openfeature.provider.ProductMetadata;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.octopusdeploy.OctopusDeployCustomizer;
import org.iromu.openfeature.boot.octopusdeploy.OctopusDeployProperties;

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
 * Autoconfiguration for the Octopus Deploy {@link OctopusFeatureProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ OctopusFeatureProvider.class })
@ConditionalOnProperty(prefix = OctopusDeployProperties.OCTOPUSDEPLOY_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(OctopusDeployProperties.class)
public class OctopusDeployAutoConfiguration {

	/**
	 * Creates the {@link OctopusFeatureConfiguration} built from the configured
	 * {@link OctopusDeployProperties}, applying every available
	 * {@link OctopusDeployCustomizer} last.
	 * @param customizers the ordered provider of OctopusDeployCustomizer beans
	 * @param properties the OctopusDeployProperties holding the provider settings
	 * @return the configured OctopusFeatureConfiguration instance
	 */
	@Bean
	@ConditionalOnMissingBean({ OctopusFeatureConfiguration.class, FeatureProvider.class })
	public OctopusFeatureConfiguration octopusDeployConfiguration(ObjectProvider<OctopusDeployCustomizer> customizers,
			OctopusDeployProperties properties) {
		validateProperties(properties);

		ProductMetadata metadata = new ProductMetadata(properties.getProductName(),
				(properties.getProductVersion() != null) ? properties.getProductVersion()
						: properties.getProductName());

		OctopusFeatureConfiguration config = new OctopusFeatureConfiguration(properties.getClientIdentifier(),
				metadata);
		config.setCacheDuration(properties.getCacheDuration());

		customizers.orderedStream().forEach((customizer) -> customizer.customize(config));

		return config;
	}

	/**
	 * Creates the {@link FeatureProvider} that resolves flags against the Octopus Deploy
	 * service.
	 * @param config the OctopusFeatureConfiguration the provider is built from
	 * @return the Octopus Deploy feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider octopusDeployProvider(OctopusFeatureConfiguration config) {
		return new OctopusFeatureProvider(config);
	}

	private void validateProperties(OctopusDeployProperties properties) {
		if (!StringUtils.hasText(properties.getClientIdentifier())) {
			throw new IllegalStateException("Octopus Deploy requires '" + OctopusDeployProperties.OCTOPUSDEPLOY_PREFIX
					+ ".client-identifier' when '" + OctopusDeployProperties.OCTOPUSDEPLOY_PREFIX + ".enabled=true'");
		}
		if (!StringUtils.hasText(properties.getProductName())) {
			throw new IllegalStateException("Octopus Deploy requires '" + OctopusDeployProperties.OCTOPUSDEPLOY_PREFIX
					+ ".product-name' when '" + OctopusDeployProperties.OCTOPUSDEPLOY_PREFIX + ".enabled=true'");
		}
	}

}
