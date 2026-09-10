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

package org.iromu.openfeature.boot.autoconfigure;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.OpenFeatureAPI;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for {@link Client}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@ConditionalOnBean(FeatureProvider.class)
@ConditionalOnMissingBean(name = "multiProvider")
public class ClientAutoConfiguration {

	/**
	 * Build the {@link Client} after binding the given {@link FeatureProvider}.
	 * @param featureProvider the provider to set on the OpenFeature API
	 * @param customizers provider of {@link ClientCustomizer} callbacks to apply to the
	 * client
	 * @return the configured {@link Client} instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public Client client(FeatureProvider featureProvider, ObjectProvider<ClientCustomizer> customizers) {
		OpenFeatureAPI.getInstance().setProviderAndWait(featureProvider);
		Client client = OpenFeatureAPI.getInstance().getClient();
		customizers.orderedStream().forEach((customizer) -> customizer.customize(client));
		return client;
	}

}
