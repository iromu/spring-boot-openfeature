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

package org.iromu.openfeature.boot.autoconfigure.multiprovider;

import dev.openfeature.contrib.providers.multiprovider.FirstMatchStrategy;
import dev.openfeature.contrib.providers.multiprovider.MultiProvider;
import dev.openfeature.contrib.providers.multiprovider.Strategy;
import dev.openfeature.sdk.Client;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.autoconfigure.envvar.EnvVarAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultiProviderAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class MultiProviderAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, EnvVarAutoConfiguration.class,
				MultiProviderAutoConfiguration.class));

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(Strategy.class)
			.hasBean("firstMatchStrategy")
			.hasSingleBean(MultiProvider.class)
			.hasBean("multiProvider")
			.hasSingleBean(Client.class)
			.hasBean("multiClient")
			.doesNotHaveBean("client"));
	}

	@Test
	void shouldBackOffWhenUserSuppliesMultiProvider() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MultiProviderAutoConfiguration.class))
			.withUserConfiguration(UserMultiProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(MultiProvider.class)
				.doesNotHaveBean("multiProvider")
				.hasBean("multiClient"));
	}

	@Test
	void shouldBackOffWhenUserSuppliesStrategy() {
		this.contextRunner.withUserConfiguration(UserStrategyConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(Strategy.class)
				.doesNotHaveBean("firstMatchStrategy")
				.hasBean("multiProvider"));
	}

	@Configuration(proxyBeanMethods = false)
	static class UserMultiProviderConfiguration {

		@Bean
		public MultiProvider customMultiProvider() {
			// A Mockito mock skips the constructor, leaving EventProvider's final
			// attachment
			// field null, so the SDK NPEs when it attaches the provider to the event bus.
			return new MultiProvider(List.of(), new FirstMatchStrategy());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserStrategyConfiguration {

		@Bean
		public Strategy customStrategy() {
			return Mockito.mock(Strategy.class);
		}

	}

}
