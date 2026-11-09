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

package org.iromu.openfeature.boot.autoconfigure.superposition;

import dev.openfeature.sdk.FeatureProvider;
import io.juspay.superposition.openfeature.SuperpositionOpenFeatureProvider;
import io.juspay.superposition.openfeature.SuperpositionProviderOptions;
import io.juspay.superposition.openfeature.options.RefreshStrategy;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.superposition.SuperpositionCustomizer;
import org.iromu.openfeature.boot.superposition.SuperpositionProperties;

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
 * Autoconfiguration for the Superposition {@link SuperpositionOpenFeatureProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ SuperpositionOpenFeatureProvider.class })
@ConditionalOnProperty(prefix = SuperpositionProperties.SUPERPOSITION_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(SuperpositionProperties.class)
public class SuperpositionAutoConfiguration {

	/**
	 * Creates the {@link SuperpositionProviderOptions} built from the configured
	 * {@link SuperpositionProperties}, applying every available
	 * {@link SuperpositionCustomizer} last.
	 * <p>
	 * The Superposition SDK requires an API token, an organization ID and a workspace ID,
	 * so a missing one fails fast with a message naming the missing properties. The
	 * validation runs before the builder because the vendor builder rejects null values
	 * with a bare {@link NullPointerException}.
	 * @param customizers the ordered provider of SuperpositionCustomizer beans
	 * @param properties the SuperpositionProperties holding the provider settings
	 * @return the configured SuperpositionProviderOptions instance
	 */
	@Bean
	@ConditionalOnMissingBean({ SuperpositionProviderOptions.class, FeatureProvider.class })
	public SuperpositionProviderOptions superpositionOptions(ObjectProvider<SuperpositionCustomizer> customizers,
			SuperpositionProperties properties) {
		validateCredentials(properties);

		SuperpositionProviderOptions.SuperpositionProviderOptionsBuilder builder = SuperpositionProviderOptions
			.builder()
			.endpoint(properties.getEndpoint())
			.orgId(properties.getOrgId())
			.workspaceId(properties.getWorkspaceId())
			.token(properties.getApiKey())
			.refreshStrategy(RefreshStrategy.Polling.of(properties.getRefreshTimeoutMillis(),
					properties.getRefreshIntervalMillis()));

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return builder.build();
	}

	/**
	 * Creates the {@link FeatureProvider} that resolves flags against the Superposition
	 * workspace selected by the configured credentials.
	 * @param superpositionOptions the SuperpositionProviderOptions the provider is built
	 * from
	 * @return the Superposition feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider superpositionProvider(SuperpositionProviderOptions superpositionOptions) {
		return new SuperpositionOpenFeatureProvider(superpositionOptions);
	}

	private void validateCredentials(SuperpositionProperties properties) {
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new IllegalStateException("Superposition requires '" + SuperpositionProperties.SUPERPOSITION_PREFIX
					+ ".api-key' when '" + SuperpositionProperties.SUPERPOSITION_PREFIX + ".enabled=true'");
		}
		if (!StringUtils.hasText(properties.getOrgId())) {
			throw new IllegalStateException("Superposition requires '" + SuperpositionProperties.SUPERPOSITION_PREFIX
					+ ".org-id' when '" + SuperpositionProperties.SUPERPOSITION_PREFIX + ".enabled=true'");
		}
		if (!StringUtils.hasText(properties.getWorkspaceId())) {
			throw new IllegalStateException("Superposition requires '" + SuperpositionProperties.SUPERPOSITION_PREFIX
					+ ".workspace-id' when '" + SuperpositionProperties.SUPERPOSITION_PREFIX + ".enabled=true'");
		}
	}

}
