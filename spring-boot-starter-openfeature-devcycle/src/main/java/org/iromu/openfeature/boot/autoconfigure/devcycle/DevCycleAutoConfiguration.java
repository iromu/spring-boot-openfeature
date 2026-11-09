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

package org.iromu.openfeature.boot.autoconfigure.devcycle;

import com.devcycle.sdk.server.cloud.api.DevCycleCloudClient;
import com.devcycle.sdk.server.cloud.model.DevCycleCloudOptions;
import com.devcycle.sdk.server.openfeature.DevCycleProvider;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.devcycle.DevCycleCustomizer;
import org.iromu.openfeature.boot.devcycle.DevCycleProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for the DevCycle {@link DevCycleProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ DevCycleProvider.class })
@ConditionalOnProperty(prefix = DevCycleProperties.DEVCYCLE_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(DevCycleProperties.class)
public class DevCycleAutoConfiguration {

	/**
	 * Creates the {@link DevCycleCloudOptions} built from the configured
	 * {@link DevCycleProperties}, applying every available {@link DevCycleCustomizer}
	 * last.
	 * <p>
	 * Options the consumer did not configure are left untouched so the DevCycle SDK's own
	 * defaults keep applying.
	 * @param customizers the ordered provider of DevCycleCustomizer beans
	 * @param devCycleProperties the DevCycleProperties holding the provider settings
	 * @return the configured DevCycleCloudOptions instance
	 */
	@Bean
	@ConditionalOnMissingBean({ DevCycleCloudOptions.class, FeatureProvider.class })
	public DevCycleCloudOptions devCycleOptions(ObjectProvider<DevCycleCustomizer> customizers,
			DevCycleProperties devCycleProperties) {
		DevCycleCloudOptions.DevCycleCloudOptionsBuilder builder = DevCycleCloudOptions.builder();

		if (devCycleProperties.getBaseUrlOverride() != null && !devCycleProperties.getBaseUrlOverride().isBlank()) {
			builder.baseURLOverride(devCycleProperties.getBaseUrlOverride());
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return builder.build();
	}

	/**
	 * Creates the {@link FeatureProvider} that resolves flags against the DevCycle
	 * project selected by the configured server-side key.
	 * @param devCycleOptions the DevCycleCloudOptions the client is built from
	 * @param devCycleProperties the DevCycleProperties holding the server-side key
	 * @return the DevCycle feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider devCycleProvider(DevCycleCloudOptions devCycleOptions,
			DevCycleProperties devCycleProperties) {
		String serverSideKey = devCycleProperties.getServerSideKey();
		if (serverSideKey == null || serverSideKey.isBlank()) {
			throw new IllegalStateException("DevCycle server-side key is not set. Configure "
					+ DevCycleProperties.DEVCYCLE_PREFIX + ".server-side-key or disable the starter with "
					+ DevCycleProperties.DEVCYCLE_PREFIX + ".enabled=false");
		}
		return new DevCycleProvider(new DevCycleCloudClient(serverSideKey, devCycleOptions));
	}

}
