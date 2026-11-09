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

package org.iromu.openfeature.boot.autoconfigure.datadog;

import java.time.Duration;

import datadog.trace.api.openfeature.Provider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.ProviderEvaluation;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.datadog.DatadogCustomizer;
import org.iromu.openfeature.boot.datadog.DatadogProviderConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.datadog.DatadogProperties.DATADOG_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link DatadogAutoConfiguration}. Every test runs on a host with no Datadog
 * credentials and never triggers a network call to Datadog.
 *
 * @author Ivan Rodriguez
 */
@SuppressWarnings({ "NullableProblems", "DataFlowIssue" })
class DatadogAutoConfigurationTest {

	private static final String[] REQUIRED_PROPERTIES = { DATADOG_PREFIX + ".api-key=test-api-key",
			DATADOG_PREFIX + ".app-key=test-app-key", DATADOG_PREFIX + ".site=datadoghq.com" };

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, DatadogAutoConfiguration.class));

	private final ApplicationContextRunner configRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DatadogAutoConfiguration.class));

	@AfterAll
	static void clearTracerSettings() {
		clearIfEquals("DD_API_KEY", "test-api-key");
		clearIfEquals("DD_SITE", "datadoghq.com");
		clearIfEquals("DD_ENV", "test-env");
	}

	@Test
	void shouldSupplyDefaultBeans() {
		// The Datadog provider can only initialize with the Datadog java agent loaded,
		// so the OpenFeatureAPI singleton is stubbed to keep this test credential-free.
		try (MockedStatic<OpenFeatureAPI> api = Mockito.mockStatic(OpenFeatureAPI.class)) {
			OpenFeatureAPI openFeatureAPI = Mockito.mock(OpenFeatureAPI.class);
			api.when(OpenFeatureAPI::getInstance).thenReturn(openFeatureAPI);
			Mockito.when(openFeatureAPI.getClient()).thenReturn(Mockito.mock(Client.class));

			this.contextRunner.withPropertyValues(REQUIRED_PROPERTIES).run((context) -> {
				assertThat(context).hasSingleBean(DatadogProviderConfig.class)
					.hasBean("datadogProviderConfig")
					.hasSingleBean(FeatureProvider.class)
					.hasBean("datadogProvider")
					.hasSingleBean(Client.class)
					.hasBean("client");
				assertThat(context.getBean("datadogProvider", FeatureProvider.class)).isInstanceOf(Provider.class);
			});
		}
	}

	@Test
	void shouldCarryBoundCredentials() {
		this.configRunner
			.withPropertyValues(REQUIRED_PROPERTIES[0], REQUIRED_PROPERTIES[1], REQUIRED_PROPERTIES[2],
					DATADOG_PREFIX + ".environment=test-env", DATADOG_PREFIX + ".init-timeout=15s")
			.run((context) -> {
				DatadogProviderConfig config = context.getBean(DatadogProviderConfig.class);
				assertThat(config.getApiKey()).isEqualTo("test-api-key");
				assertThat(config.getAppKey()).isEqualTo("test-app-key");
				assertThat(config.getSite()).isEqualTo("datadoghq.com");
				assertThat(config.getEnvironment()).isEqualTo("test-env");
				assertThat(config.getInitTimeout()).isEqualTo(Duration.ofSeconds(15));
				assertThat(config.isEvaluationTelemetryOptOut()).isFalse();
			});
	}

	@Test
	void shouldConfigureWithCustomizer() {
		this.configRunner.withUserConfiguration(CustomizerConfiguration.class)
			.withPropertyValues(REQUIRED_PROPERTIES)
			.run((context) -> {
				DatadogProviderConfig config = context.getBean(DatadogProviderConfig.class);
				assertThat(config.getSite()).isEqualTo("datadoghq.eu");
				assertThat(config.isEvaluationTelemetryOptOut()).isTrue();
			});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(DATADOG_PREFIX + ".enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("datadogProvider"));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(REQUIRED_PROPERTIES)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("datadogProvider")
				.hasSingleBean(Client.class));
	}

	@Test
	void shouldFailFastWhenCredentialsMissing() {
		this.configRunner.run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
				.hasRootCauseMessage(
						"Datadog feature flags require 'spring.openfeature.datadog.api-key', 'app-key' and 'site' "
								+ "when 'spring.openfeature.datadog.enabled=true'");
		});
	}

	@Test
	void shouldFailFastWhenSiteMissing() {
		this.configRunner.withPropertyValues(REQUIRED_PROPERTIES[0], REQUIRED_PROPERTIES[1]).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class);
		});
	}

	@Test
	void shouldNotOverrideExistingTracerSettings() {
		System.setProperty("DD_API_KEY", "preset");
		try {
			this.configRunner.withPropertyValues(REQUIRED_PROPERTIES)
				.run((context) -> assertThat(System.getProperty("DD_API_KEY")).isEqualTo("preset"));
		}
		finally {
			System.clearProperty("DD_API_KEY");
		}
	}

	private static void clearIfEquals(String name, String expected) {
		if (expected.equals(System.getProperty(name))) {
			System.clearProperty(name);
		}
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
		public DatadogCustomizer datadogCustomizer() {
			return (config) -> {
				config.setSite("datadoghq.eu");
				config.setEvaluationTelemetryOptOut(true);
			};
		}

	}

}
