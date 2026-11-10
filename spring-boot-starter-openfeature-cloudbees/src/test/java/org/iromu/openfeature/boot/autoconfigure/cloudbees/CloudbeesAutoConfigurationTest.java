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

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.cloudbees.openfeature.provider.CloudbeesProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.ProviderState;
import io.rollout.rox.server.Rox;
import io.rollout.rox.server.RoxOptions;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.cloudbees.CloudbeesCustomizer;
import org.iromu.openfeature.boot.cloudbees.CloudbeesProviderOptions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.cloudbees.CloudbeesProperties.CLOUDBEES_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Tests for {@link CloudbeesAutoConfiguration}.
 * <p>
 * The suite never reaches CloudBees: the provider constructor calls the static
 * {@code Rox.setup} factory, which opens a connection to the CloudBees service (design.md
 * D3), so every test that constructs the real provider does so inside a Mockito static
 * mock of {@code Rox}. The disabled, back-off and missing-credential tests never
 * construct the real provider at all.
 *
 * @author Ivan Rodriguez
 */
class CloudbeesAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, CloudbeesAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed (with Rox.setup mocked)
	// but never registered with the OpenFeature API, so these tests assert pure
	// configuration assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CloudbeesAutoConfiguration.class));

	static final String[] requiredProperties = new String[] { CLOUDBEES_PREFIX + ".api-key=fake-app-key" };

	@Test
	void shouldSupplyDefaultBeans() {
		try (MockedStatic<Rox> ignored = mockRoxSetup()) {
			this.contextRunner.withPropertyValues(requiredProperties).run((context) -> {
				assertThat(context).hasSingleBean(CloudbeesProviderOptions.class)
					.hasSingleBean(FeatureProvider.class)
					.hasBean("cloudbeesProvider")
					.hasSingleBean(Client.class)
					.hasBean("client");
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(CloudbeesProvider.class);
			});
		}
	}

	@Test
	void shouldSupplyDefaultBeansWithEndpoint() {
		try (MockedStatic<Rox> ignored = mockRoxSetup()) {
			this.contextRunner
				.withPropertyValues(add(requiredProperties, CLOUDBEES_PREFIX + ".endpoint=https://rox.example.com/rox"))
				.run((context) -> {
					assertThat(context).hasSingleBean(FeatureProvider.class);
					assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(CloudbeesProvider.class);
				});
		}
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, CLOUDBEES_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("cloudbeesProvider")
				.doesNotHaveBean(CloudbeesProviderOptions.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("cloudbeesProvider")
				.doesNotHaveBean(CloudbeesProviderOptions.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailWhenApiKeyMissing() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasMessageContaining(CLOUDBEES_PREFIX + ".api-key");
		});
	}

	@Test
	void shouldFailWhenEndpointMalformed() {
		this.contextRunner.withPropertyValues(
				new String[] { CLOUDBEES_PREFIX + ".api-key=fake-app-key", CLOUDBEES_PREFIX + ".endpoint=not-a-url" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasMessageContaining(CLOUDBEES_PREFIX + ".endpoint");
			});
	}

	@Test
	void shouldPassConfiguredApiKeyToProvider() {
		AtomicReference<String> capturedAppKey = new AtomicReference<>();

		// The static mock captures the app key the provider constructor hands to the
		// vendor SDK, so this test proves the configured property reaches the provider
		// without any CloudBees credentials.
		MockedStatic<Rox> mocked = Mockito.mockStatic(Rox.class);
		mocked.when(() -> Rox.setup(anyString(), any(RoxOptions.class))).thenAnswer((invocation) -> {
			capturedAppKey.set(invocation.getArgument(0));
			return CompletableFuture.completedFuture(null);
		});

		try (mocked) {
			this.optionsRunner.withPropertyValues(requiredProperties)
				.run((context) -> assertThat(capturedAppKey.get()).isEqualTo("fake-app-key"));
		}
	}

	@Test
	void shouldApplyCustomizerToAssembledOptions() {
		AtomicReference<RoxOptions.Builder> capturedBuilder = new AtomicReference<>();

		// The customizer sets the fetch interval, so a customizer that never ran
		// (or ran against a different builder) leaves the default in place and fails
		// the assertion.
		try (MockedStatic<Rox> ignored = mockRoxSetup()) {
			this.optionsRunner.withPropertyValues(requiredProperties)
				.withBean(CloudbeesCustomizer.class, () -> (CloudbeesCustomizer) (builder) -> {
					builder.withFetchIntervalInSeconds(99L);
					capturedBuilder.set(builder);
				})
				.run((context) -> {
					assertThat(capturedBuilder.get()).isNotNull();
					assertThat(context.getBean(CloudbeesProviderOptions.class).getOptions().getFetchIntervalInSeconds())
						.isEqualTo(99L);
				});
		}
	}

	private static MockedStatic<Rox> mockRoxSetup() {
		MockedStatic<Rox> mocked = Mockito.mockStatic(Rox.class);
		mocked.when(() -> Rox.setup(anyString(), any(RoxOptions.class)))
			.thenReturn(CompletableFuture.completedFuture(null));
		return mocked;
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
