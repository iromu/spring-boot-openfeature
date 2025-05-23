/*
 * Copyright 2025-2025 the original author or authors.
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

package org.iromu.openfeature.boot.autoconfigure.envvar;

import dev.openfeature.contrib.providers.envvar.EnvVarProvider;
import dev.openfeature.contrib.providers.envvar.EnvironmentGateway;
import dev.openfeature.contrib.providers.envvar.EnvironmentKeyTransformer;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.envvar.EnvVarProperties;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for {@link EnvVarProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ EnvVarProvider.class })
@ConditionalOnProperty(prefix = EnvVarProperties.ENVVAR_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(EnvVarProperties.class)
public class EnvVarAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public EnvironmentGateway environmentGateway() {
		return System::getenv;
	}

	@Bean
	@ConditionalOnMissingBean
	public EnvironmentKeyTransformer environmentKeyTransformer() {
		return EnvironmentKeyTransformer.doNothing();
	}

	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider envVarProvider(EnvironmentGateway environmentGateway,
			EnvironmentKeyTransformer environmentKeyTransformer) {
		return new EnvVarProvider(environmentGateway, environmentKeyTransformer);
	}

}
