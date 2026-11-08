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

package org.iromu.openfeature.boot.autoconfigure.launchdarkly;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.launchdarkly.openfeature.serverprovider.Provider;
import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.integrations.FileData;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.ProviderEvaluation;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.launchdarkly.LaunchDarklyCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.launchdarkly.LaunchDarklyProperties.LAUNCHDARKLY_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link LaunchDarklyAutoConfiguration}.
 * <p>
 * The suite never reaches LaunchDarkly: the happy path points the client at a local flag
 * file through a {@link LaunchDarklyCustomizer} (file data source + diagnostic opt-out +
 * no event delivery, design.md findings), and the offline-mode tests use the SDK's
 * offline client, which opens no connection at all. The user-provider back-off keeps the
 * provider unconstructed.
 *
 * @author Ivan Rodriguez
 */
class LaunchDarklyAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, LaunchDarklyAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed but never registered, so
	// these tests assert pure wiring and configuration assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LaunchDarklyAutoConfiguration.class));

	static final String[] requiredProperties = new String[] {
			LAUNCHDARKLY_PREFIX + ".service-key=sk-fake-project-1-abc123456" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(FileDataSourceConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(LDConfig.class)
					.hasSingleBean(FeatureProvider.class)
					.hasBean("launchDarklyProvider")
					.hasSingleBean(Client.class)
					.hasBean("client");
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(Provider.class);
				// LaunchDarkly requires a targeting key in the evaluation context.
				Client client = context.getBean(Client.class);
				client.setEvaluationContext(new ImmutableContext("test-user"));
				assertThat(client.getBooleanValue("random-boolean-flag", false)).isTrue();
			});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, LAUNCHDARKLY_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("launchDarklyProvider")
				.doesNotHaveBean(LDConfig.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("launchDarklyProvider")
				.doesNotHaveBean(LDConfig.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailWhenServiceKeyMissing() {
		this.contextRunner.withUserConfiguration(FileDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
				.hasMessageContaining(LAUNCHDARKLY_PREFIX + ".service-key");
		});
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AtomicReference<LDConfig.Builder> captured = new AtomicReference<>();

		// offline is set only by the customizer, so a customizer that never ran (or ran
		// against a different builder) leaves the client online and fails the assertion.
		this.optionsRunner.withPropertyValues(requiredProperties)
			.withBean(LaunchDarklyCustomizer.class, () -> (LaunchDarklyCustomizer) (builder) -> {
				builder.offline(true);
				captured.set(builder);
			})
			.run((context) -> {
				assertThat(captured.get()).isNotNull();
				Provider provider = (Provider) context.getBean(FeatureProvider.class);
				assertThat(provider.getLdClient().isOffline()).isTrue();
			});
	}

	@Test
	void shouldBindOfflineProperty() {
		this.optionsRunner.withPropertyValues(add(requiredProperties, LAUNCHDARKLY_PREFIX + ".offline=true"))
			.run((context) -> {
				Provider provider = (Provider) context.getBean(FeatureProvider.class);
				assertThat(provider.getLdClient().isOffline()).isTrue();
			});

		this.optionsRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(FileDataSourceConfiguration.class)
			.run((context) -> {
				Provider provider = (Provider) context.getBean(FeatureProvider.class);
				assertThat(provider.getLdClient().isOffline()).isFalse();
			});
	}

	// Loads flags from a local file and disables event delivery so the LDClient never
	// opens a connection to LaunchDarkly; the fake service key is only format-checked
	// by the SDK.
	@Configuration(proxyBeanMethods = false)
	static class FileDataSourceConfiguration {

		@Bean
		public LaunchDarklyCustomizer fileDataSourceCustomizer() {
			return (builder) -> builder.dataSource(FileData.dataSource().classpathResources("launchdarkly/flags.json"))
				.diagnosticOptOut(true)
				.events(Components.noEvents());
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

	private static String[] add(String[] array, String element) {
		return Stream.concat(Arrays.stream(array), Stream.of(element)).toArray(String[]::new);
	}

}
