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

package org.iromu.openfeature.boot.autoconfigure.kameleoon;

import com.kameleoon.KameleoonClientConfig;
import com.kameleoon.KameleoonException;
import com.kameleoon.openfeature.KameleoonProvider;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.kameleoon.KameleoonCustomizer;
import org.iromu.openfeature.boot.kameleoon.KameleoonProperties;

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
 * Autoconfiguration for the Kameleoon {@link KameleoonProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ KameleoonProvider.class })
@ConditionalOnProperty(prefix = KameleoonProperties.KAMELEOON_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(KameleoonProperties.class)
public class KameleoonAutoConfiguration {

	/**
	 * Creates the {@link KameleoonClientConfig} built from the configured
	 * {@link KameleoonProperties}, applying every available {@link KameleoonCustomizer}
	 * last.
	 * <p>
	 * Options the consumer did not configure are left untouched so the Kameleoon client's
	 * own defaults keep applying. The Kameleoon client requires a client ID and a client
	 * secret, so a missing one fails fast with a message naming the missing properties.
	 * @param customizers the ordered provider of KameleoonCustomizer beans
	 * @param properties the KameleoonProperties holding the provider settings
	 * @return the configured KameleoonClientConfig instance
	 */
	@Bean
	@ConditionalOnMissingBean({ KameleoonClientConfig.class, FeatureProvider.class })
	public KameleoonClientConfig kameleoonClientConfig(ObjectProvider<KameleoonCustomizer> customizers,
			KameleoonProperties properties) {
		KameleoonClientConfig.Builder builder = new KameleoonClientConfig.Builder();

		if (properties.getEnvironment() != null) {
			builder.environment(properties.getEnvironment());
		}

		if (properties.getTopLevelDomain() != null) {
			builder.topLevelDomain(properties.getTopLevelDomain());
		}

		if (properties.getClientId() != null) {
			builder.clientId(properties.getClientId());
		}

		if (properties.getClientSecret() != null) {
			builder.clientSecret(properties.getClientSecret());
		}

		if (properties.getRefreshInterval() != null) {
			builder.refreshInterval(properties.getRefreshInterval());
		}

		if (properties.getSessionDuration() != null) {
			builder.sessionDuration(properties.getSessionDuration());
		}

		if (properties.getDefaultTimeout() != null) {
			builder.defaultTimeout(properties.getDefaultTimeout());
		}

		if (properties.getTrackingInterval() != null) {
			builder.trackingInterval(properties.getTrackingInterval());
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		try {
			return builder.build();
		}
		catch (KameleoonException.ConfigCredentialsInvalid ex) {
			throw new IllegalStateException("Kameleoon requires '" + KameleoonProperties.KAMELEOON_PREFIX
					+ ".client-id' and '" + KameleoonProperties.KAMELEOON_PREFIX + ".client-secret' when '"
					+ KameleoonProperties.KAMELEOON_PREFIX + ".enabled=true'", ex);
		}
	}

	/**
	 * Creates the {@link FeatureProvider} that resolves flags against the Kameleoon site
	 * selected by the configured site code. Fails fast when the starter is enabled but no
	 * site code is configured.
	 * @param kameleoonClientConfig the KameleoonClientConfig the provider is built from
	 * @param properties the KameleoonProperties holding the site code
	 * @return the Kameleoon feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider kameleoonProvider(KameleoonClientConfig kameleoonClientConfig,
			KameleoonProperties properties) {
		if (!StringUtils.hasText(properties.getSiteCode())) {
			throw new IllegalStateException("Kameleoon requires '" + KameleoonProperties.KAMELEOON_PREFIX
					+ ".site-code' when '" + KameleoonProperties.KAMELEOON_PREFIX + ".enabled=true'");
		}
		return new KameleoonProvider(properties.getSiteCode(), kameleoonClientConfig);
	}

}
