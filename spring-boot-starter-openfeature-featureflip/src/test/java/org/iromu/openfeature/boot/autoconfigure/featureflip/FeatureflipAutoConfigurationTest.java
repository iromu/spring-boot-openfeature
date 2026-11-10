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

package org.iromu.openfeature.boot.autoconfigure.featureflip;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.ProviderEvaluation;
import io.featureflip.openfeature.FeatureflipProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.featureflip.FeatureflipCustomizer;
import org.iromu.openfeature.boot.featureflip.FeatureflipProviderConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.featureflip.FeatureflipProperties.FEATUREFLIP_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link FeatureflipAutoConfiguration}. Every test runs on a host with no
 * Featureflip credentials and never triggers a network call to Featureflip: the provider
 * constructor is lazy (it only stores the SDK key), so the default-bean test stubs the
 * {@link OpenFeatureAPI} singleton to keep the provider from being initialized, and the
 * disabled/back-off tests never construct the provider at all.
 *
 * @author Ivan Rodriguez
 */
@SuppressWarnings({ "NullableProblems", "DataFlowIssue" })
class FeatureflipAutoConfigurationTest {

	private static final String API_KEY_PROPERTY = FEATUREFLIP_PREFIX + ".api-key=test-api-key";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, FeatureflipAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed but never registered, so
	// these tests assert pure wiring and configuration assembly.
	private final ApplicationContextRunner configRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(FeatureflipAutoConfiguration.class));

	@Test
	void shouldSupplyDefaultBeans() {
		// The OpenFeatureAPI singleton is stubbed so setProviderAndWait is a no-op and
		// the
		// lazily-constructed FeatureflipProvider is never initialized (no network call).
		try (MockedStatic<OpenFeatureAPI> api = Mockito.mockStatic(OpenFeatureAPI.class)) {
			OpenFeatureAPI openFeatureAPI = Mockito.mock(OpenFeatureAPI.class);
			api.when(OpenFeatureAPI::getInstance).thenReturn(openFeatureAPI);
			Mockito.when(openFeatureAPI.getClient()).thenReturn(Mockito.mock(Client.class));

			this.contextRunner.withPropertyValues(API_KEY_PROPERTY).run((context) -> {
				assertThat(context).hasSingleBean(FeatureflipProviderConfig.class)
					.hasBean("featureflipProviderConfig")
					.hasSingleBean(FeatureProvider.class)
					.hasBean("featureflipProvider")
					.hasSingleBean(Client.class)
					.hasBean("client");
				assertThat(context.getBean("featureflipProvider", FeatureProvider.class))
					.isInstanceOf(FeatureflipProvider.class);
			});
		}
	}

	@Test
	void shouldCarryBoundCredentials() {
		this.configRunner.withPropertyValues(API_KEY_PROPERTY, FEATUREFLIP_PREFIX + ".base-url=https://ff.example.com")
			.run((context) -> {
				FeatureflipProviderConfig config = context.getBean(FeatureflipProviderConfig.class);
				assertThat(config.getApiKey()).isEqualTo("test-api-key");
				assertThat(config.getBaseUrl()).isEqualTo("https://ff.example.com");
			});
	}

	@Test
	void shouldConfigureWithCustomizer() {
		this.configRunner.withUserConfiguration(CustomizerConfiguration.class)
			.withPropertyValues(API_KEY_PROPERTY)
			.run((context) -> {
				FeatureflipProviderConfig config = context.getBean(FeatureflipProviderConfig.class);
				assertThat(config.getBaseUrl()).isEqualTo("https://custom.example.com");
			});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(FEATUREFLIP_PREFIX + ".enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("featureflipProvider")
				.doesNotHaveBean(FeatureflipProviderConfig.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(API_KEY_PROPERTY)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("featureflipProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailFastWhenApiKeyMissing() {
		this.configRunner.run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
				.hasRootCauseMessage("Featureflip feature flags require '" + FEATUREFLIP_PREFIX + ".api-key' when '"
						+ FEATUREFLIP_PREFIX + ".enabled=true'");
		});
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
		public FeatureflipCustomizer featureflipCustomizer() {
			return (config) -> config.setBaseUrl("https://custom.example.com");
		}

	}

}
