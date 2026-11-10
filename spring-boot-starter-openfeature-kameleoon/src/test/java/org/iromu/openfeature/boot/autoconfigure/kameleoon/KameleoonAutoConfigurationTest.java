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

package org.iromu.openfeature.boot.autoconfigure.kameleoon;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.kameleoon.KameleoonClientConfig;
import com.kameleoon.openfeature.KameleoonProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.ProviderState;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.kameleoon.KameleoonCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.kameleoon.KameleoonProperties.KAMELEOON_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link KameleoonAutoConfiguration}.
 * <p>
 * The suite never reaches Kameleoon: the provider constructor starts a background
 * configuration refresh against the Kameleoon service (design.md findings), so every test
 * that constructs the provider does so through a Mockito construction mock. The disabled,
 * back-off and missing-credential tests never construct the real provider at all.
 *
 * @author Ivan Rodriguez
 */
class KameleoonAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, KameleoonAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed (mocked) but never
	// registered
	// with the OpenFeature API, so these tests assert pure configuration assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(KameleoonAutoConfiguration.class));

	static final String[] requiredProperties = new String[] { KAMELEOON_PREFIX + ".site-code=fake-site-code",
			KAMELEOON_PREFIX + ".client-id=fake-client-id", KAMELEOON_PREFIX + ".client-secret=fake-client-secret" };

	@Test
	void shouldSupplyDefaultBeans() {
		try (MockedConstruction<KameleoonProvider> ignored = Mockito.mockConstruction(KameleoonProvider.class,
				(mock, context) -> {
					Mockito.when(mock.getMetadata()).thenReturn(() -> "Kameleoon");
					Mockito.when(mock.getState()).thenReturn(ProviderState.READY);
				})) {
			this.contextRunner.withPropertyValues(requiredProperties).run((context) -> {
				assertThat(context).hasSingleBean(KameleoonClientConfig.class)
					.hasSingleBean(FeatureProvider.class)
					.hasBean("kameleoonProvider")
					.hasSingleBean(Client.class)
					.hasBean("client");
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(KameleoonProvider.class);
			});
		}
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, KAMELEOON_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("kameleoonProvider")
				.doesNotHaveBean(KameleoonClientConfig.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("kameleoonProvider")
				.doesNotHaveBean(KameleoonClientConfig.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailWhenSiteCodeMissing() {
		this.contextRunner
			.withPropertyValues(new String[] { KAMELEOON_PREFIX + ".client-id=fake-client-id",
					KAMELEOON_PREFIX + ".client-secret=fake-client-secret" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasMessageContaining(KAMELEOON_PREFIX + ".site-code");
			});
	}

	@Test
	void shouldFailWhenClientIdMissing() {
		this.contextRunner
			.withPropertyValues(new String[] { KAMELEOON_PREFIX + ".site-code=fake-site-code",
					KAMELEOON_PREFIX + ".client-secret=fake-client-secret" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasMessageContaining(KAMELEOON_PREFIX + ".client-id");
			});
	}

	@Test
	void shouldFailWhenClientSecretMissing() {
		this.contextRunner
			.withPropertyValues(new String[] { KAMELEOON_PREFIX + ".site-code=fake-site-code",
					KAMELEOON_PREFIX + ".client-id=fake-client-id" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasMessageContaining(KAMELEOON_PREFIX + ".client-secret");
			});
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AtomicReference<KameleoonClientConfig.Builder> captured = new AtomicReference<>();

		// environment is set only by the customizer, so a customizer that never ran (or
		// ran
		// against a different builder) leaves environment() null and fails the assertion.
		try (MockedConstruction<KameleoonProvider> ignored = Mockito.mockConstruction(KameleoonProvider.class)) {
			this.optionsRunner.withPropertyValues(requiredProperties)
				.withBean(KameleoonCustomizer.class, () -> (KameleoonCustomizer) (builder) -> {
					builder.environment("custom-environment");
					captured.set(builder);
				})
				.run((context) -> {
					assertThat(captured.get()).isNotNull();
					assertThat(context.getBean(KameleoonClientConfig.class).environment())
						.isEqualTo("custom-environment");
				});
		}
	}

	@Test
	void shouldBindOptionalProperties() {
		try (MockedConstruction<KameleoonProvider> ignored = Mockito.mockConstruction(KameleoonProvider.class)) {
			this.optionsRunner
				.withPropertyValues(new String[] { KAMELEOON_PREFIX + ".site-code=fake-site-code",
						KAMELEOON_PREFIX + ".client-id=fake-client-id",
						KAMELEOON_PREFIX + ".client-secret=fake-client-secret", KAMELEOON_PREFIX + ".environment=prod",
						KAMELEOON_PREFIX + ".top-level-domain=kameleoon.com", KAMELEOON_PREFIX + ".refresh-interval=3",
						KAMELEOON_PREFIX + ".session-duration=4", KAMELEOON_PREFIX + ".default-timeout=5000",
						KAMELEOON_PREFIX + ".tracking-interval=1000" })
				.run((context) -> {
					KameleoonClientConfig config = context.getBean(KameleoonClientConfig.class);
					assertThat(config.environment()).isEqualTo("prod");
					assertThat(config.topLevelDomain()).isEqualTo("kameleoon.com");
					assertThat(config.refreshIntervalMillis()).isEqualTo(3 * 60 * 1000);
					assertThat(config.sessionDurationMillis()).isEqualTo(4 * 60 * 1000);
					assertThat(config.defaultTimeoutMillis()).isEqualTo(5000);
					assertThat(config.trackingIntervalMillis()).isEqualTo(1000);
				});
		}
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
