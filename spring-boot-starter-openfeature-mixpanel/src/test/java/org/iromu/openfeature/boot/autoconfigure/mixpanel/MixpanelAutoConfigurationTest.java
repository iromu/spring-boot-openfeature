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

package org.iromu.openfeature.boot.autoconfigure.mixpanel;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.mixpanel.mixpanelapi.featureflags.config.RemoteFlagsConfig;
import com.mixpanel.openfeature.MixpanelProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.mixpanel.MixpanelCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.mixpanel.MixpanelProperties.MIXPANEL_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link MixpanelAutoConfiguration}.
 * <p>
 * The suite never reaches Mixpanel: the remote-evaluation provider constructor is lazy
 * (no network I/O until the first flag evaluation — design.md findings), so every test
 * that constructs the provider does so with a fake token and no connection is opened. The
 * disabled, back-off and missing-token tests never construct the real provider at all.
 *
 * @author Ivan Rodriguez
 */
class MixpanelAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, MixpanelAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed but never registered with
	// the
	// OpenFeature API, so these tests assert pure wiring and configuration assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MixpanelAutoConfiguration.class));

	static final String[] requiredProperties = new String[] { MIXPANEL_PREFIX + ".project-token=fake-token-abc123",
			MIXPANEL_PREFIX + ".api-host=fake.api.mixpanel.com", MIXPANEL_PREFIX + ".request-timeout-seconds=5" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties).run((context) -> {
			assertThat(context).hasSingleBean(RemoteFlagsConfig.class)
				.hasSingleBean(FeatureProvider.class)
				.hasBean("mixpanelProvider")
				.hasSingleBean(Client.class)
				.hasBean("client");
			assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(MixpanelProvider.class);
		});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, MIXPANEL_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("mixpanelProvider")
				.doesNotHaveBean(RemoteFlagsConfig.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("mixpanelProvider")
				.doesNotHaveBean(RemoteFlagsConfig.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailWhenTokenMissing() {
		this.contextRunner
			.withPropertyValues(new String[] { MIXPANEL_PREFIX + ".api-host=fake.api.mixpanel.com",
					MIXPANEL_PREFIX + ".request-timeout-seconds=5" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
					.hasMessageContaining(MIXPANEL_PREFIX + ".project-token");
			});
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AtomicReference<RemoteFlagsConfig.Builder> captured = new AtomicReference<>();

		// request-timeout-seconds is set only by the customizer, so a customizer that
		// never
		// ran
		// (or ran against a different builder) leaves the SDK default of 10 in place and
		// fails
		// the assertion.
		this.optionsRunner.withPropertyValues(new String[] { MIXPANEL_PREFIX + ".project-token=fake-token-abc123" })
			.withBean(MixpanelCustomizer.class, () -> (MixpanelCustomizer) (builder) -> {
				builder.requestTimeoutSeconds(42);
				captured.set(builder);
			})
			.run((context) -> {
				assertThat(captured.get()).isNotNull();
				assertThat(context.getBean(RemoteFlagsConfig.class).getRequestTimeoutSeconds()).isEqualTo(42);
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(MixpanelProvider.class);
			});
	}

	@Test
	void shouldBindPropertiesToBuiltConfiguration() {
		this.optionsRunner.withPropertyValues(requiredProperties).run((context) -> {
			RemoteFlagsConfig config = context.getBean(RemoteFlagsConfig.class);
			assertThat(config.getProjectToken()).isEqualTo("fake-token-abc123");
			assertThat(config.getApiHost()).isEqualTo("fake.api.mixpanel.com");
			assertThat(config.getRequestTimeoutSeconds()).isEqualTo(5);
		});
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
