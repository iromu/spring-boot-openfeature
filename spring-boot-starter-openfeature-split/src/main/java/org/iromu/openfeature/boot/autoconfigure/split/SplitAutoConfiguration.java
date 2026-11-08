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

package org.iromu.openfeature.boot.autoconfigure.split;

import java.io.IOException;
import java.net.URISyntaxException;

import dev.openfeature.sdk.FeatureProvider;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactory;
import io.split.client.SplitFactoryBuilder;
import io.split.openfeature.SplitProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.split.SplitCustomizer;
import org.iromu.openfeature.boot.split.SplitProperties;
import org.iromu.openfeature.boot.split.SplitProviderOptions;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for {@link SplitProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ SplitProvider.class })
@ConditionalOnProperty(prefix = SplitProperties.SPLIT_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(SplitProperties.class)
public class SplitAutoConfiguration {

	/**
	 * Creates the {@link SplitProviderOptions} built from the configured
	 * {@link SplitProperties}, applying every available {@link SplitCustomizer} to the
	 * underlying {@link SplitClientConfig.Builder}.
	 * <p>
	 * Options the consumer did not configure are left untouched so the Split SDK's own
	 * builder defaults keep applying.
	 * @param customizers the ordered provider of SplitCustomizer beans
	 * @param splitProperties the SplitProperties holding the connection settings
	 * @return the configured SplitProviderOptions instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public SplitProviderOptions splitProviderOptions(ObjectProvider<SplitCustomizer> customizers,
			SplitProperties splitProperties) {
		SplitClientConfig.Builder builder = SplitClientConfig.builder();

		if (splitProperties.getDomain() != null) {
			builder.endpoint(splitProperties.getDomain(), splitProperties.getDomain());
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return new SplitProviderOptions(splitProperties.getAuthorizationKey(), builder.build());
	}

	/**
	 * Creates the {@link SplitProvider} feature provider that resolves flags against the
	 * Split service using the given configuration. Fails fast when the starter is enabled
	 * but no authorization key is configured.
	 * @param splitProviderOptions the SplitProviderOptions the provider is built from
	 * @return the SplitProvider feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider splitProvider(SplitProviderOptions splitProviderOptions) {
		String authorizationKey = splitProviderOptions.getAuthorizationKey();
		if (authorizationKey == null || authorizationKey.isBlank()) {
			throw new IllegalStateException(
					SplitProperties.SPLIT_PREFIX + ".authorization-key must be set to use the Split starter");
		}
		try {
			SplitFactory splitFactory = SplitFactoryBuilder.build(authorizationKey,
					splitProviderOptions.getClientConfig());
			return new SplitProvider(splitFactory.client());
		}
		catch (IOException | URISyntaxException ex) {
			throw new IllegalStateException("Unable to create the Split factory", ex);
		}
	}

}
