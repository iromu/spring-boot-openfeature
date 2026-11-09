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

package org.iromu.openfeature.boot.autoconfigure.featbit;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import co.featbit.openfeature.FBProvider;
import co.featbit.server.FBConfig;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.featbit.FeatBitCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.featbit.FeatBitProperties.FEATBIT_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link FeatBitAutoConfiguration}.
 * <p>
 * The suite never reaches FeatBit: the provider constructor is eager (it always builds an
 * {@code FBClient}), so every test that constructs the provider does so in offline mode
 * (design.md findings), which wires a null data synchronizer and a null insight processor
 * — no connection is opened and nothing blocks. The disabled, back-off and missing-token
 * tests never construct the real provider at all.
 *
 * @author Ivan Rodriguez
 */
class FeatBitAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, FeatBitAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed but never registered with
	// the
	// OpenFeature API, so these tests assert pure wiring and configuration assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(FeatBitAutoConfiguration.class));

	static final String[] requiredProperties = new String[] { FEATBIT_PREFIX + ".token=fake-token-abc123",
			FEATBIT_PREFIX + ".streaming-url=ws://fake-stream", FEATBIT_PREFIX + ".event-url=http://fake-event",
			FEATBIT_PREFIX + ".offline=true", FEATBIT_PREFIX + ".disable-events=true" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties).run((context) -> {
			assertThat(context).hasSingleBean(FBConfig.class)
				.hasSingleBean(FeatureProvider.class)
				.hasBean("featbitProvider")
				.hasSingleBean(Client.class)
				.hasBean("client");
			assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(FBProvider.class);
		});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, FEATBIT_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("featbitProvider")
				.doesNotHaveBean(FBConfig.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("featbitProvider")
				.doesNotHaveBean(FBConfig.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailWhenTokenMissing() {
		this.contextRunner
			.withPropertyValues(new String[] { FEATBIT_PREFIX + ".streaming-url=ws://fake-stream",
					FEATBIT_PREFIX + ".event-url=http://fake-event", FEATBIT_PREFIX + ".offline=true" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
					.hasMessageContaining(FEATBIT_PREFIX + ".token");
			});
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AtomicReference<FBConfig.Builder> captured = new AtomicReference<>();

		// disable-events is set only by the customizer, so a customizer that never ran
		// (or ran
		// against a different builder) leaves isDisableEvents() false and fails the
		// assertion.
		this.optionsRunner
			.withPropertyValues(new String[] { FEATBIT_PREFIX + ".token=fake-token-abc123",
					FEATBIT_PREFIX + ".streaming-url=ws://fake-stream", FEATBIT_PREFIX + ".event-url=http://fake-event",
					FEATBIT_PREFIX + ".offline=true" })
			.withBean(FeatBitCustomizer.class, () -> (FeatBitCustomizer) (builder) -> {
				builder.disableEvents(true);
				captured.set(builder);
			})
			.run((context) -> {
				assertThat(captured.get()).isNotNull();
				assertThat(context.getBean(FBConfig.class).isDisableEvents()).isTrue();
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(FBProvider.class);
			});
	}

	@Test
	void shouldBindOfflineProperty() {
		this.optionsRunner
			.withPropertyValues(new String[] { FEATBIT_PREFIX + ".token=fake-token-abc123",
					FEATBIT_PREFIX + ".streaming-url=ws://fake-stream", FEATBIT_PREFIX + ".event-url=http://fake-event",
					FEATBIT_PREFIX + ".offline=true" })
			.run((context) -> assertThat(context.getBean(FBConfig.class).isOffline()).isTrue());
	}

	@Configuration(proxyBeanMethods = false)
	static class UserProviderConfiguration {

		@Bean
		public FeatureProvider featureProvider() {
			FeatureProvider mock = Mockito.mock(FeatureProvider.class);
			Mockito.when(mock.getMetadata()).thenReturn(() -> "UserFeatureProvider");
			Mockito.when(mock.getBooleanEvaluation(any(), any(), any()))
				.thenReturn(dev.openfeature.sdk.ProviderEvaluation.<Boolean>builder().value(true).build());
			return mock;
		}

	}

	private static String[] add(String[] array, String element) {
		return Stream.concat(Arrays.stream(array), Stream.of(element)).toArray(String[]::new);
	}

}
