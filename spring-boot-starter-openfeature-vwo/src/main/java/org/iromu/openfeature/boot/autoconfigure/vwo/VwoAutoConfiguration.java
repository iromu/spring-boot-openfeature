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

package org.iromu.openfeature.boot.autoconfigure.vwo;

import java.util.HashMap;
import java.util.Map;

import com.vwo.VWOProvider;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.vwo.VwoCustomizer;
import org.iromu.openfeature.boot.vwo.VwoProperties;
import org.iromu.openfeature.boot.vwo.VwoProviderOptions;

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
 * Autoconfiguration for the VWO {@link VWOProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ VWOProvider.class })
@ConditionalOnProperty(prefix = VwoProperties.VWO_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(VwoProperties.class)
public class VwoAutoConfiguration {

	/**
	 * Creates the {@link VwoProviderOptions} built from the configured
	 * {@link VwoProperties}, applying every available {@link VwoCustomizer} last.
	 * <p>
	 * Options the consumer did not configure are left untouched so the VWO SDK's own
	 * defaults keep applying.
	 * @param customizers the ordered provider of VwoCustomizer beans
	 * @param properties the VwoProperties holding the connection settings
	 * @return the configured VwoProviderOptions instance
	 */
	@Bean
	@ConditionalOnMissingBean({ VwoProviderOptions.class, FeatureProvider.class })
	public VwoProviderOptions vwoProviderOptions(ObjectProvider<VwoCustomizer> customizers, VwoProperties properties) {
		Map<String, Object> options = new HashMap<>();

		if (properties.getAccountId() != null) {
			options.put("accountId", properties.getAccountId());
		}

		if (properties.getApiKey() != null) {
			options.put("sdkKey", properties.getApiKey());
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(options));

		return new VwoProviderOptions(options);
	}

	/**
	 * Creates the {@link FeatureProvider} that resolves flags against the VWO account
	 * selected by the configured credentials. Fails fast when the starter is enabled but
	 * the account ID or the API key is missing or malformed, because the provider would
	 * swallow the error and end up with an uninitialized client.
	 * @param vwoProviderOptions the VwoProviderOptions the provider is built from
	 * @param properties the VwoProperties holding the credentials
	 * @return the VWO feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider vwoProvider(VwoProviderOptions vwoProviderOptions, VwoProperties properties) {
		if (!StringUtils.hasText(properties.getAccountId())) {
			throw new IllegalStateException("VWO requires '" + VwoProperties.VWO_PREFIX + ".account-id' when '"
					+ VwoProperties.VWO_PREFIX + ".enabled=true'");
		}
		try {
			Integer.parseInt(properties.getAccountId());
		}
		catch (NumberFormatException ex) {
			throw new IllegalStateException("'" + VwoProperties.VWO_PREFIX
					+ ".account-id' must be a numeric VWO account ID, got '" + properties.getAccountId() + "'", ex);
		}
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new IllegalStateException("VWO requires '" + VwoProperties.VWO_PREFIX + ".api-key' when '"
					+ VwoProperties.VWO_PREFIX + ".enabled=true'");
		}
		return new VWOProvider(vwoProviderOptions.getOptions());
	}

}
