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

package org.iromu.openfeature.boot.autoconfigure.flagd;

import dev.openfeature.contrib.providers.flagd.FlagdOptions;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.flagd.FlagdCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.flagd.FlagdProperties.FLAGD_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link FlagdAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class FlagdAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, FlagdAutoConfiguration.class));

	static String[] requiredProperties = new String[] { FLAGD_PREFIX + ".resolverType=file",
			FLAGD_PREFIX + ".offlineFlagSourcePath=flags/testing-flags.json", FLAGD_PREFIX + ".deadline=1000" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.hasBean("flagdProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(FLAGD_PREFIX + ".enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("flagdProvider"));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("flagdProvider")
				.hasSingleBean(Client.class));
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(CustomizerConfiguration.class)
			.run((context) -> assertThat(context.getBean(FlagdOptions.class)).extracting("deadline").isEqualTo(4242));
	}

	@Configuration(proxyBeanMethods = false)
	static class UserProviderConfiguration {

		@Bean
		public FeatureProvider featureProvider() {
			FeatureProvider mock = Mockito.mock(FeatureProvider.class);
			Mockito.when(mock.getMetadata()).thenReturn(() -> "UserFeatureProvider");
			Mockito.when(mock.getBooleanEvaluation(any(), any(), any()))
				.thenReturn(ProviderEvaluation.<Boolean>builder().value(true).build());
			return mock;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		public FlagdCustomizer flagdDeadlineCustomizer() {
			return builder -> builder.deadline(4242);
		}

	}

}
