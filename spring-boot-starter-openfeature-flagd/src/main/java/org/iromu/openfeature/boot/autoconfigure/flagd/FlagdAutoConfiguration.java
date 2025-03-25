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

package org.iromu.openfeature.boot.autoconfigure.flagd;

import java.io.IOException;

import dev.openfeature.contrib.providers.flagd.FlagdOptions;
import dev.openfeature.contrib.providers.flagd.FlagdProvider;
import dev.openfeature.sdk.FeatureProvider;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.flagd.FlagdCustomizer;
import org.iromu.openfeature.boot.flagd.FlagdProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for {@link FlagdProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ FlagdProvider.class })
@ConditionalOnProperty(prefix = FlagdProperties.FLAGD_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(FlagdProperties.class)
@Slf4j
public class FlagdAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public FlagdOptions flagdOptions(ObjectProvider<FlagdCustomizer> customizers, FlagdProperties flagdProperties) {
		FlagdOptions.FlagdOptionsBuilder builder = FlagdOptions.builder()
			.resolverType(flagdProperties.getResolverType())
			.deadline(flagdProperties.getDeadline());

		if (flagdProperties.getOfflineFlagSourcePath() != null) {
			try {
				builder.offlineFlagSourcePath(flagdProperties.getOfflineFlagSourcePath().getFile().getAbsolutePath());
			}
			catch (IOException ex) {
				log.error(ex.getMessage());
			}
		}

		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider flagdProvider(FlagdOptions flagdOptions) {
		return new FlagdProvider(flagdOptions);
	}

}
