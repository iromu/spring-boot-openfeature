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

package org.iromu.openfeature.boot.autoconfigure.superposition;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.ProviderState;
import io.juspay.superposition.openfeature.SuperpositionOpenFeatureProvider;
import io.juspay.superposition.openfeature.SuperpositionProviderOptions;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.superposition.SuperpositionCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.superposition.SuperpositionProperties.SUPERPOSITION_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link SuperpositionAutoConfiguration}.
 * <p>
 * The suite never reaches Superposition: the provider constructor is lazy (the
 * configuration refresh jobs only start in {@code initialize}), so every test that
 * constructs the provider does so with fake credentials and no connection is opened. The
 * disabled, back-off and missing-credential tests never construct the real provider at
 * all.
 *
 * @author Ivan Rodriguez
 */
class SuperpositionAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, SuperpositionAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed (no network I/O) but never
	// registered with the OpenFeature API, so these tests assert pure configuration
	// assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SuperpositionAutoConfiguration.class));

	static final String[] requiredProperties = new String[] { SUPERPOSITION_PREFIX + ".api-key=fake-api-key",
			SUPERPOSITION_PREFIX + ".org-id=fake-org-id", SUPERPOSITION_PREFIX + ".workspace-id=fake-workspace-id" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties).run((context) -> {
			assertThat(context).hasSingleBean(SuperpositionProviderOptions.class)
				.hasSingleBean(FeatureProvider.class)
				.hasBean("superpositionProvider")
				.hasSingleBean(Client.class)
				.hasBean("client");
			assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(SuperpositionOpenFeatureProvider.class);
		});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, SUPERPOSITION_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("superpositionProvider")
				.doesNotHaveBean(SuperpositionProviderOptions.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("superpositionProvider")
				.doesNotHaveBean(SuperpositionProviderOptions.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailWhenApiKeyMissing() {
		this.contextRunner
			.withPropertyValues(new String[] { SUPERPOSITION_PREFIX + ".org-id=fake-org-id",
					SUPERPOSITION_PREFIX + ".workspace-id=fake-workspace-id" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasMessageContaining(SUPERPOSITION_PREFIX + ".api-key");
			});
	}

	@Test
	void shouldFailWhenOrgIdMissing() {
		this.contextRunner
			.withPropertyValues(new String[] { SUPERPOSITION_PREFIX + ".api-key=fake-api-key",
					SUPERPOSITION_PREFIX + ".workspace-id=fake-workspace-id" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasMessageContaining(SUPERPOSITION_PREFIX + ".org-id");
			});
	}

	@Test
	void shouldFailWhenWorkspaceIdMissing() {
		this.contextRunner
			.withPropertyValues(new String[] { SUPERPOSITION_PREFIX + ".api-key=fake-api-key",
					SUPERPOSITION_PREFIX + ".org-id=fake-org-id" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasMessageContaining(SUPERPOSITION_PREFIX + ".workspace-id");
			});
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AtomicReference<SuperpositionProviderOptions.SuperpositionProviderOptionsBuilder> captured = new AtomicReference<>();

		// endpoint is set only by the customizer, so a customizer that never ran (or ran
		// against a different builder) leaves the endpoint at the default and fails the
		// assertion.
		this.optionsRunner.withPropertyValues(requiredProperties)
			.withBean(SuperpositionCustomizer.class, () -> (SuperpositionCustomizer) (builder) -> {
				builder.endpoint("custom.example.org");
				captured.set(builder);
			})
			.run((context) -> {
				assertThat(captured.get()).isNotNull();
				assertThat(context.getBean(SuperpositionProviderOptions.class).getEndpoint())
					.isEqualTo("custom.example.org");
			});
	}

	@Test
	void shouldBindOptionalProperties() {
		this.optionsRunner.withPropertyValues(new String[] { SUPERPOSITION_PREFIX + ".api-key=fake-api-key",
				SUPERPOSITION_PREFIX + ".org-id=fake-org-id", SUPERPOSITION_PREFIX + ".workspace-id=fake-workspace-id",
				SUPERPOSITION_PREFIX + ".endpoint=https://superposition.example.org",
				SUPERPOSITION_PREFIX + ".refresh-timeout-millis=1500",
				SUPERPOSITION_PREFIX + ".refresh-interval-millis=750" })
			.run((context) -> {
				SuperpositionProviderOptions options = context.getBean(SuperpositionProviderOptions.class);
				assertThat(options.getEndpoint()).isEqualTo("https://superposition.example.org");
				assertThat(options.getRefreshStrategy().getTimeout()).isEqualTo(1500);
				assertThat(context.getBean(org.iromu.openfeature.boot.superposition.SuperpositionProperties.class)
					.getRefreshIntervalMillis()).isEqualTo(750);
			});
	}

	private static String[] add(String[] array, String element) {
		return Stream.concat(Arrays.stream(array), Stream.of(element)).toArray(String[]::new);
	}

	@Configuration(proxyBeanMethods = false)
	static class UserProviderConfiguration {

		@Bean
		public FeatureProvider featureProvider() {
			FeatureProvider mock = Mockito.mock(FeatureProvider.class);
			Mockito.when(mock.getMetadata()).thenReturn(() -> "UserFeatureProvider");
			Mockito.when(mock.getState()).thenReturn(ProviderState.READY);
			Mockito.when(mock.getBooleanEvaluation(any(), any(), any()))
				.thenReturn(ProviderEvaluation.<Boolean>builder().value(true).build());
			return mock;
		}

	}

}
