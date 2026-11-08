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

package org.iromu.openfeature.boot.autoconfigure.launchdarkly;

import com.launchdarkly.openfeature.serverprovider.Provider;
import com.launchdarkly.sdk.server.LDConfig;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.launchdarkly.LaunchDarklyCustomizer;
import org.iromu.openfeature.boot.launchdarkly.LaunchDarklyProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for the LaunchDarkly {@link Provider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ Provider.class })
@ConditionalOnProperty(prefix = LaunchDarklyProperties.LAUNCHDARKLY_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(LaunchDarklyProperties.class)
public class LaunchDarklyAutoConfiguration {

	/**
	 * Creates the {@link LDConfig} built from the configured
	 * {@link LaunchDarklyProperties}, applying every available
	 * {@link LaunchDarklyCustomizer} last.
	 * <p>
	 * Options the consumer did not configure are left untouched so the LaunchDarkly SDK's
	 * own defaults keep applying.
	 * @param customizers the ordered provider of LaunchDarklyCustomizer beans
	 * @param launchDarklyProperties the LaunchDarklyProperties holding the provider
	 * settings
	 * @return the configured LDConfig instance
	 */
	@Bean
	@ConditionalOnMissingBean({ LDConfig.class, FeatureProvider.class })
	public LDConfig launchDarklyConfig(ObjectProvider<LaunchDarklyCustomizer> customizers,
			LaunchDarklyProperties launchDarklyProperties) {
		LDConfig.Builder builder = new LDConfig.Builder();

		if (launchDarklyProperties.isOffline()) {
			builder.offline(true);
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return builder.build();
	}

	/**
	 * Creates the {@link FeatureProvider} that resolves flags against the LaunchDarkly
	 * environment selected by the configured service key.
	 * @param launchDarklyConfig the LDConfig the provider is built from
	 * @param launchDarklyProperties the LaunchDarklyProperties holding the service key
	 * @return the LaunchDarkly feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider launchDarklyProvider(LDConfig launchDarklyConfig,
			LaunchDarklyProperties launchDarklyProperties) {
		String serviceKey = launchDarklyProperties.getServiceKey();
		if (serviceKey == null || serviceKey.isBlank()) {
			throw new IllegalStateException("LaunchDarkly service key is not set. Configure "
					+ LaunchDarklyProperties.LAUNCHDARKLY_PREFIX + ".service-key or disable the starter with "
					+ LaunchDarklyProperties.LAUNCHDARKLY_PREFIX + ".enabled=false");
		}
		return new Provider(serviceKey, launchDarklyConfig);
	}

}
