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

package org.iromu.openfeature.boot.autoconfigure.ofrep;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.openfeature.contrib.providers.ofrep.OfrepProvider;
import dev.openfeature.contrib.providers.ofrep.OfrepProviderOptions;
import dev.openfeature.sdk.FeatureProvider;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.ofrep.OfrepCustomizer;
import org.iromu.openfeature.boot.ofrep.OfrepProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for {@link OfrepProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ OfrepProvider.class })
@ConditionalOnProperty(prefix = OfrepProperties.OFREP_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(OfrepProperties.class)
@Slf4j
public class OfrepAutoConfiguration {

	/**
	 * Creates the {@link OfrepProviderOptions} from the provided properties and
	 * customizers.
	 * @param customizers an ObjectProvider providing the OfrepCustomizer instances
	 * @param ofrepProperties the OFREP configuration properties
	 * @return the OfrepProviderOptions used to configure the provider
	 */
	@Bean
	@ConditionalOnMissingBean
	public OfrepProviderOptions ofrepOptions(ObjectProvider<OfrepCustomizer> customizers,
			OfrepProperties ofrepProperties) {
		OfrepProviderOptions.Builder builder = OfrepProviderOptions.builder();

		if (ofrepProperties.getBaseUrl() != null) {
			builder.baseUrl(ofrepProperties.getBaseUrl());
		}
		if (ofrepProperties.getConnectTimeout() != null) {
			builder.connectTimeout(ofrepProperties.getConnectTimeout());
		}
		if (ofrepProperties.getRequestTimeout() != null) {
			builder.requestTimeout(ofrepProperties.getRequestTimeout());
		}
		if (ofrepProperties.getHeaders() != null) {
			ImmutableMap.Builder<String, ImmutableList<String>> headers = ImmutableMap.builder();
			ofrepProperties.getHeaders().forEach((name, values) -> headers.put(name, ImmutableList.copyOf(values)));
			builder.headers(headers.build());
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return builder.build();
	}

	/**
	 * Creates the {@link FeatureProvider} backed by OFREP.
	 * @param ofrepOptions the OfrepProviderOptions used to configure the provider
	 * @return the OfrepProvider registered as the feature provider
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider ofrepProvider(OfrepProviderOptions ofrepOptions) {
		return OfrepProvider.constructProvider(ofrepOptions);
	}

}
