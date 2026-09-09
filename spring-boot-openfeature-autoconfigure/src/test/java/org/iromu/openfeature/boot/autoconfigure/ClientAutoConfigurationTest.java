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

package org.iromu.openfeature.boot.autoconfigure;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link ClientAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class ClientAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class));

	@Test
	void shouldSupplyClientWhenProviderPresent() {
		this.contextRunner.withUserConfiguration(ProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldNotSupplyClientWhenNoProvider() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(Client.class));
	}

	@Test
	void shouldBackOffWhenMultiProviderPresent() {
		this.contextRunner.withUserConfiguration(MultiProviderConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(Client.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class ProviderConfiguration {

		@Bean
		public FeatureProvider featureProvider() {
			FeatureProvider mock = Mockito.mock(FeatureProvider.class);
			Mockito.when(mock.getMetadata()).thenReturn(() -> "MockedFeatureProvider");
			Mockito.when(mock.getBooleanEvaluation(any(), any(), any()))
				.thenReturn(ProviderEvaluation.<Boolean>builder().value(true).build());
			return mock;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultiProviderConfiguration {

		@Bean
		public FeatureProvider multiProvider() {
			FeatureProvider mock = Mockito.mock(FeatureProvider.class);
			Mockito.when(mock.getMetadata()).thenReturn(() -> "MockedMultiProvider");
			Mockito.when(mock.getBooleanEvaluation(any(), any(), any()))
				.thenReturn(ProviderEvaluation.<Boolean>builder().value(true).build());
			return mock;
		}

	}

}
