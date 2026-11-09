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

package org.iromu.openfeature.boot.autoconfigure.featbit;

import co.featbit.openfeature.FBProvider;
import co.featbit.server.FBConfig;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.featbit.FeatBitCustomizer;
import org.iromu.openfeature.boot.featbit.FeatBitProperties;

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
 * Autoconfiguration for the FeatBit {@link FBProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ FBProvider.class })
@ConditionalOnProperty(prefix = FeatBitProperties.FEATBIT_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(FeatBitProperties.class)
public class FeatBitAutoConfiguration {

	/**
	 * Creates the {@link FBConfig} built from the configured {@link FeatBitProperties},
	 * applying every available {@link FeatBitCustomizer} last.
	 * <p>
	 * Options the consumer did not configure are left untouched so the FeatBit SDK's own
	 * defaults keep applying.
	 * @param customizers the ordered provider of FeatBitCustomizer beans
	 * @param properties the FeatBitProperties holding the provider settings
	 * @return the configured FBConfig instance
	 */
	@Bean
	@ConditionalOnMissingBean({ FBConfig.class, FeatureProvider.class })
	public FBConfig featbitConfig(ObjectProvider<FeatBitCustomizer> customizers, FeatBitProperties properties) {
		FBConfig.Builder builder = new FBConfig.Builder();

		if (properties.isOffline()) {
			builder.offline(true);
		}

		if (properties.isDisableEvents()) {
			builder.disableEvents(true);
		}

		if (properties.getStreamingUrl() != null) {
			builder.streamingURL(properties.getStreamingUrl());
		}

		if (properties.getEventUrl() != null) {
			builder.eventURL(properties.getEventUrl());
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return builder.build();
	}

	/**
	 * Creates the {@link FeatureProvider} that resolves flags against the FeatBit
	 * environment selected by the configured app token.
	 * @param featbitConfig the FBConfig the provider is built from
	 * @param properties the FeatBitProperties holding the app token and endpoints
	 * @return the FeatBit feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider featbitProvider(FBConfig featbitConfig, FeatBitProperties properties) {
		validateCredentials(properties);
		return new FBProvider(properties.getToken(), featbitConfig);
	}

	private void validateCredentials(FeatBitProperties properties) {
		if (!StringUtils.hasText(properties.getToken()) || !StringUtils.hasText(properties.getStreamingUrl())
				|| !StringUtils.hasText(properties.getEventUrl())) {
			throw new IllegalStateException("FeatBit requires '" + FeatBitProperties.FEATBIT_PREFIX + ".token', '"
					+ FeatBitProperties.FEATBIT_PREFIX + ".streaming-url' and '" + FeatBitProperties.FEATBIT_PREFIX
					+ ".event-url' when '" + FeatBitProperties.FEATBIT_PREFIX + ".enabled=true'");
		}
	}

}
