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

package org.iromu.openfeature.boot.autoconfigure.cloudbees;

import java.net.MalformedURLException;
import java.net.URL;

import com.cloudbees.openfeature.provider.CloudbeesProvider;
import dev.openfeature.sdk.FeatureProvider;
import io.rollout.rox.server.RoxOptions;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.cloudbees.CloudbeesCustomizer;
import org.iromu.openfeature.boot.cloudbees.CloudbeesProperties;
import org.iromu.openfeature.boot.cloudbees.CloudbeesProviderOptions;

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
 * Autoconfiguration for the CloudBees {@link CloudbeesProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ CloudbeesProvider.class })
@ConditionalOnProperty(prefix = CloudbeesProperties.CLOUDBEES_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(CloudbeesProperties.class)
public class CloudbeesAutoConfiguration {

	/**
	 * Creates the {@link CloudbeesProviderOptions} built from the configured
	 * {@link CloudbeesProperties}, applying every available {@link CloudbeesCustomizer}
	 * last.
	 * <p>
	 * Options the consumer did not configure are left untouched so the CloudBees SDK's
	 * own defaults keep applying.
	 * @param customizers the ordered provider of CloudbeesCustomizer beans
	 * @param properties the CloudbeesProperties holding the connection settings
	 * @return the configured CloudbeesProviderOptions instance
	 */
	@Bean
	@ConditionalOnMissingBean({ CloudbeesProviderOptions.class, FeatureProvider.class })
	public CloudbeesProviderOptions cloudbeesProviderOptions(ObjectProvider<CloudbeesCustomizer> customizers,
			CloudbeesProperties properties) {
		RoxOptions.Builder builder = new RoxOptions.Builder();

		if (StringUtils.hasText(properties.getEndpoint())) {
			builder.withRoxyURL(toUrl(properties.getEndpoint()));
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return new CloudbeesProviderOptions(builder.build());
	}

	/**
	 * Creates the {@link FeatureProvider} that resolves flags against the CloudBees
	 * environment selected by the configured credentials. Fails fast when the starter is
	 * enabled but the API key is missing, because the provider would otherwise NPE inside
	 * the vendor SDK on a null app key.
	 * @param cloudbeesProviderOptions the CloudbeesProviderOptions the provider is built
	 * from
	 * @param properties the CloudbeesProperties holding the credentials
	 * @return the CloudBees feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider cloudbeesProvider(CloudbeesProviderOptions cloudbeesProviderOptions,
			CloudbeesProperties properties) {
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new IllegalStateException("CloudBees requires '" + CloudbeesProperties.CLOUDBEES_PREFIX
					+ ".api-key' when '" + CloudbeesProperties.CLOUDBEES_PREFIX + ".enabled=true'");
		}
		return new CloudbeesProvider(properties.getApiKey(), cloudbeesProviderOptions.getOptions());
	}

	private static URL toUrl(String endpoint) {
		try {
			return new URL(endpoint);
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException("'" + CloudbeesProperties.CLOUDBEES_PREFIX
					+ ".endpoint' must be a valid URL, got '" + endpoint + "'", ex);
		}
	}

}
