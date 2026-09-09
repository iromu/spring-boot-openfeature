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

package org.iromu.openfeature.boot.autoconfigure.flagsmith;

import dev.openfeature.contrib.providers.flagsmith.FlagsmithProviderOptions;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.flagsmith.FlagsmithCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.flagsmith.FlagsmithProperties.FLAGSMITH_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link FlagsmithAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class FlagsmithAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, FlagsmithAutoConfiguration.class));

	static String[] requiredProperties = new String[] { FLAGSMITH_PREFIX + ".apiKey=API_KEY" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.hasBean("flagsmithProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(FLAGSMITH_PREFIX + ".enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("flagsmithProvider"));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("flagsmithProvider")
				.hasSingleBean(Client.class));
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(CustomizerConfiguration.class)
			.run((context) -> assertThat(context.getBean(FlagsmithProviderOptions.class)).extracting("baseUri")
				.isEqualTo("http://custom.flagsmith.example/api/v1/"));
	}

	@Configuration(proxyBeanMethods = false)
	static class UserProviderConfiguration {

		@Bean
		public FeatureProvider featureProvider() {
			return userProvider();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		// The user-supplied provider makes the auto-config provider back off, so the real
		// FlagsmithProvider (which initialises a client) is never constructed.
		@Bean
		public FeatureProvider featureProvider() {
			return userProvider();
		}

		@Bean
		public FlagsmithCustomizer flagsmithBaseUriCustomizer() {
			return builder -> builder.baseUri("http://custom.flagsmith.example/api/v1/");
		}

	}

	private static FeatureProvider userProvider() {
		FeatureProvider mock = Mockito.mock(FeatureProvider.class);
		Mockito.when(mock.getMetadata()).thenReturn(() -> "UserFeatureProvider");
		Mockito.when(mock.getBooleanEvaluation(any(), any(), any()))
			.thenReturn(ProviderEvaluation.<Boolean>builder().value(true).build());
		return mock;
	}

}
