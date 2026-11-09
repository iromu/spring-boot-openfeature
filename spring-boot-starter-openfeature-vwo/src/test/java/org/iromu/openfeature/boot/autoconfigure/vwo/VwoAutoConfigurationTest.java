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

package org.iromu.openfeature.boot.autoconfigure.vwo;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.vwo.VWO;
import com.vwo.VWOProvider;
import com.vwo.models.user.VWOInitOptions;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.ProviderState;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.vwo.VwoCustomizer;
import org.iromu.openfeature.boot.vwo.VwoProviderOptions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.vwo.VwoProperties.VWO_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link VwoAutoConfiguration}.
 * <p>
 * The suite never reaches VWO: the provider constructor calls the static {@code VWO.init}
 * factory, which initializes the VWO client against the VWO service (design.md D3), so
 * every test that constructs the real provider does so inside a Mockito static mock of
 * {@code VWO}. The disabled, back-off and missing-credential tests never construct the
 * real provider at all.
 *
 * @author Ivan Rodriguez
 */
class VwoAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, VwoAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed (with VWO.init mocked) but
	// never registered with the OpenFeature API, so these tests assert pure configuration
	// assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(VwoAutoConfiguration.class));

	static final String[] requiredProperties = new String[] { VWO_PREFIX + ".account-id=12345",
			VWO_PREFIX + ".api-key=fake-api-key" };

	@Test
	void shouldSupplyDefaultBeans() {
		try (MockedStatic<VWO> ignored = mockVwoInit()) {
			this.contextRunner.withPropertyValues(requiredProperties).run((context) -> {
				assertThat(context).hasSingleBean(VwoProviderOptions.class)
					.hasSingleBean(FeatureProvider.class)
					.hasBean("vwoProvider")
					.hasSingleBean(Client.class)
					.hasBean("client");
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(VWOProvider.class);
			});
		}
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, VWO_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("vwoProvider")
				.doesNotHaveBean(VwoProviderOptions.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("vwoProvider")
				.doesNotHaveBean(VwoProviderOptions.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailWhenAccountIdMissing() {
		this.contextRunner.withPropertyValues(VWO_PREFIX + ".api-key=fake-api-key").run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasMessageContaining(VWO_PREFIX + ".account-id");
		});
	}

	@Test
	void shouldFailWhenApiKeyMissing() {
		this.contextRunner.withPropertyValues(VWO_PREFIX + ".account-id=12345").run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasMessageContaining(VWO_PREFIX + ".api-key");
		});
	}

	@Test
	void shouldFailWhenAccountIdNotNumeric() {
		this.contextRunner
			.withPropertyValues(
					new String[] { VWO_PREFIX + ".account-id=not-a-number", VWO_PREFIX + ".api-key=fake-api-key" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasMessageContaining(VWO_PREFIX + ".account-id");
			});
	}

	@Test
	void shouldBindRequiredPropertiesIntoOptions() {
		try (MockedStatic<VWO> ignored = mockVwoInit()) {
			this.optionsRunner.withPropertyValues(requiredProperties).run((context) -> {
				VwoProviderOptions options = context.getBean(VwoProviderOptions.class);
				assertThat(options.getOptions()).containsEntry("accountId", "12345")
					.containsEntry("sdkKey", "fake-api-key");
			});
		}
	}

	@Test
	void shouldApplyCustomizerToAssembledOptions() {
		AtomicReference<Object> capturedExtra = new AtomicReference<>();

		// The customizer adds a key that is set only by the customizer, so a customizer
		// that
		// never ran (or ran against a different map) leaves the key absent and fails the
		// assertion.
		try (MockedStatic<VWO> ignored = mockVwoInit()) {
			this.optionsRunner.withPropertyValues(requiredProperties)
				.withBean(VwoCustomizer.class, () -> (VwoCustomizer) (options) -> {
					options.put("custom-option", "custom-value");
					capturedExtra.set(options.get("custom-option"));
				})
				.run((context) -> {
					assertThat(capturedExtra.get()).isEqualTo("custom-value");
					assertThat(context.getBean(VwoProviderOptions.class).getOptions()).containsEntry("custom-option",
							"custom-value");
				});
		}
	}

	private static MockedStatic<VWO> mockVwoInit() {
		MockedStatic<VWO> mocked = Mockito.mockStatic(VWO.class);
		mocked.when(() -> VWO.init(any(VWOInitOptions.class))).thenReturn(Mockito.mock(VWO.class));
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
