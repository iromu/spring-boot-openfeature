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

package org.iromu.openfeature.boot.autoconfigure.gcp;

import java.io.IOException;
import java.io.InputStream;

import com.google.auth.oauth2.GoogleCredentials;
import dev.openfeature.contrib.providers.gcp.GcpParameterManagerProvider;
import dev.openfeature.contrib.providers.gcp.GcpProviderOptions;
import dev.openfeature.contrib.providers.gcp.GcpSecretManagerProvider;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.gcp.GcpCustomizer;
import org.iromu.openfeature.boot.gcp.GcpProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for the GCP {@link GcpSecretManagerProvider} and
 * {@link GcpParameterManagerProvider} variants.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ GcpSecretManagerProvider.class, GcpParameterManagerProvider.class })
@ConditionalOnProperty(prefix = GcpProperties.GCP_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GcpProperties.class)
public class GcpAutoConfiguration {

	/**
	 * Creates the {@link GcpProviderOptions} built from the configured
	 * {@link GcpProperties}, applying every available {@link GcpCustomizer} last.
	 * <p>
	 * Options the consumer did not configure are left untouched so the provider's own
	 * builder defaults keep applying.
	 * @param customizers the ordered provider of GcpCustomizer beans
	 * @param gcpProperties the GcpProperties holding the provider settings
	 * @return the configured GcpProviderOptions instance
	 */
	@Bean
	@ConditionalOnMissingBean({ GcpProviderOptions.class, FeatureProvider.class })
	public GcpProviderOptions gcpProviderOptions(ObjectProvider<GcpCustomizer> customizers,
			GcpProperties gcpProperties) {
		GcpProviderOptions.GcpProviderOptionsBuilder builder = GcpProviderOptions.builder()
			.projectId(gcpProperties.getProjectId());

		if (gcpProperties.getVersion() != null) {
			builder.version(gcpProperties.getVersion());
		}
		if (gcpProperties.getLocationId() != null) {
			builder.locationId(gcpProperties.getLocationId());
		}
		if (gcpProperties.getCacheExpiry() != null) {
			builder.cacheExpiry(gcpProperties.getCacheExpiry());
		}
		if (gcpProperties.getCacheMaxSize() != null) {
			builder.cacheMaxSize(gcpProperties.getCacheMaxSize());
		}
		if (gcpProperties.getNamePrefix() != null) {
			builder.namePrefix(gcpProperties.getNamePrefix());
		}
		if (gcpProperties.getServiceAccountJson() != null) {
			builder.credentials(credentialsFrom(gcpProperties));
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return builder.build();
	}

	/**
	 * Creates the {@link FeatureProvider} for the variant named by
	 * {@code spring.openfeature.gcp.type}.
	 * @param gcpProviderOptions the options the provider is built from
	 * @param gcpProperties the GcpProperties holding the variant selector
	 * @return the selected GCP feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider gcpProvider(GcpProviderOptions gcpProviderOptions, GcpProperties gcpProperties) {
		String type = gcpProperties.getType();
		if (GcpProperties.SECRET_MANAGER_TYPE.equals(type)) {
			return new GcpSecretManagerProvider(gcpProviderOptions);
		}
		if (GcpProperties.PARAMETER_MANAGER_TYPE.equals(type)) {
			return new GcpParameterManagerProvider(gcpProviderOptions);
		}
		throw new IllegalArgumentException(
				"Unsupported " + GcpProperties.GCP_PREFIX + ".type '" + type + "'. Supported types are '"
						+ GcpProperties.SECRET_MANAGER_TYPE + "' and '" + GcpProperties.PARAMETER_MANAGER_TYPE + "'");
	}

	private GoogleCredentials credentialsFrom(GcpProperties gcpProperties) {
		try (InputStream inputStream = gcpProperties.getServiceAccountJson().getInputStream()) {
			return GoogleCredentials.fromStream(inputStream);
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Unable to read the service-account JSON at " + gcpProperties.getServiceAccountJson(), ex);
		}
	}

}
