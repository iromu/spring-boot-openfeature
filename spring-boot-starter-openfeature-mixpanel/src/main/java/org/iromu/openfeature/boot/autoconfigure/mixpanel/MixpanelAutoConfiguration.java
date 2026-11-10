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

package org.iromu.openfeature.boot.autoconfigure.mixpanel;

import com.mixpanel.mixpanelapi.featureflags.config.RemoteFlagsConfig;
import com.mixpanel.openfeature.MixpanelProvider;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.mixpanel.MixpanelCustomizer;
import org.iromu.openfeature.boot.mixpanel.MixpanelProperties;

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
 * Autoconfiguration for the Mixpanel {@link MixpanelProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ MixpanelProvider.class })
@ConditionalOnProperty(prefix = MixpanelProperties.MIXPANEL_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(MixpanelProperties.class)
public class MixpanelAutoConfiguration {

	/**
	 * Creates the {@link RemoteFlagsConfig} built from the configured
	 * {@link MixpanelProperties}, applying every available {@link MixpanelCustomizer}
	 * last.
	 * <p>
	 * Options the consumer did not configure are left untouched so the Mixpanel SDK's own
	 * defaults keep applying.
	 * @param customizers the ordered provider of MixpanelCustomizer beans
	 * @param properties the MixpanelProperties holding the provider settings
	 * @return the configured RemoteFlagsConfig instance
	 */
	@Bean
	@ConditionalOnMissingBean({ RemoteFlagsConfig.class, FeatureProvider.class })
	public RemoteFlagsConfig mixpanelFlagsConfig(ObjectProvider<MixpanelCustomizer> customizers,
			MixpanelProperties properties) {
		RemoteFlagsConfig.Builder builder = RemoteFlagsConfig.builder();

		if (properties.getProjectToken() != null) {
			builder.projectToken(properties.getProjectToken());
		}

		if (StringUtils.hasText(properties.getApiHost())) {
			builder.apiHost(properties.getApiHost());
		}

		if (properties.getRequestTimeoutSeconds() != null) {
			builder.requestTimeoutSeconds(properties.getRequestTimeoutSeconds());
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return builder.build();
	}

	/**
	 * Creates the {@link FeatureProvider} that resolves flags against the Mixpanel
	 * project selected by the configured project token.
	 * @param mixpanelFlagsConfig the RemoteFlagsConfig the provider is built from
	 * @param properties the MixpanelProperties holding the project token and endpoints
	 * @return the Mixpanel feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider mixpanelProvider(RemoteFlagsConfig mixpanelFlagsConfig, MixpanelProperties properties) {
		validateCredentials(properties);
		return new MixpanelProvider(properties.getProjectToken(), mixpanelFlagsConfig);
	}

	private void validateCredentials(MixpanelProperties properties) {
		if (!StringUtils.hasText(properties.getProjectToken())) {
			throw new IllegalStateException("Mixpanel requires '" + MixpanelProperties.MIXPANEL_PREFIX
					+ ".project-token' when '" + MixpanelProperties.MIXPANEL_PREFIX + ".enabled=true'");
		}
	}

}
