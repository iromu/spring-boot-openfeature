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

package org.iromu.openfeature.boot.autoconfigure.confidence;

import com.spotify.confidence.Confidence;
import com.spotify.confidence.ConfidenceFeatureProvider;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.confidence.ConfidenceCustomizer;
import org.iromu.openfeature.boot.confidence.ConfidenceProperties;
import org.iromu.openfeature.boot.confidence.ConfidenceProviderOptions;

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
 * Autoconfiguration for the Confidence {@link ConfidenceFeatureProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ ConfidenceFeatureProvider.class })
@ConditionalOnProperty(prefix = ConfidenceProperties.CONFIDENCE_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(ConfidenceProperties.class)
public class ConfidenceAutoConfiguration {

	/**
	 * Creates the {@link ConfidenceProviderOptions} built from the configured
	 * {@link ConfidenceProperties}, applying every available {@link ConfidenceCustomizer}
	 * to the underlying {@link Confidence.Builder}.
	 * <p>
	 * Options the consumer did not configure are left untouched so the Confidence SDK's
	 * own builder defaults keep applying.
	 * @param customizers the ordered provider of ConfidenceCustomizer beans
	 * @param properties the ConfidenceProperties holding the connection settings
	 * @return the configured ConfidenceProviderOptions instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public ConfidenceProviderOptions confidenceProviderOptions(ObjectProvider<ConfidenceCustomizer> customizers,
			ConfidenceProperties properties) {
		Confidence.Builder builder = Confidence.builder(properties.getApiToken());

		if (properties.getHost() != null) {
			builder.flagResolverManagedChannel(properties.getHost(),
					(properties.getPort() != null) ? properties.getPort() : 443);
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return new ConfidenceProviderOptions(properties.getApiToken(), builder);
	}

	/**
	 * Creates the {@link ConfidenceFeatureProvider} that resolves flags against the
	 * Confidence service using the given configuration. Fails fast when the starter is
	 * enabled but no API token is configured.
	 * @param confidenceProviderOptions the ConfidenceProviderOptions the provider is
	 * built from
	 * @return the Confidence feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider confidenceProvider(ConfidenceProviderOptions confidenceProviderOptions) {
		String apiToken = confidenceProviderOptions.getApiToken();
		if (!StringUtils.hasText(apiToken)) {
			throw new IllegalStateException(
					"Confidence feature flags require '" + ConfidenceProperties.CONFIDENCE_PREFIX + ".api-token' when '"
							+ ConfidenceProperties.CONFIDENCE_PREFIX + ".enabled=true'");
		}
		return new ConfidenceFeatureProvider(confidenceProviderOptions.getBuilder());
	}

}
