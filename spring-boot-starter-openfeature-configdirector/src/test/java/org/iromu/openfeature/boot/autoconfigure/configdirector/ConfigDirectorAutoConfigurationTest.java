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

package org.iromu.openfeature.boot.autoconfigure.configdirector;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.configdirector.ClientOptions;
import com.configdirector.openfeature.ConfigDirectorProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.configdirector.ConfigDirectorCustomizer;
import org.iromu.openfeature.boot.configdirector.ConfigDirectorProperties;
import org.iromu.openfeature.boot.configdirector.ConfigDirectorProviderOptions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.configdirector.ConfigDirectorProperties.CONFIGDIRECTOR_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link ConfigDirectorAutoConfiguration}.
 * <p>
 * The suite needs no ConfigDirector credentials: the provider constructor is lazy, so the
 * assembly tests construct the real provider with a fake API key and nothing is fetched.
 * The one test that registers the provider with the OpenFeature API (which starts the
 * background eventsource on initialization) points the endpoint at a refused localhost
 * port, so initialization fails fast without reaching the network, and shuts the provider
 * down so the eventsource threads terminate. The disabled, back-off and missing-key tests
 * never register the provider at all.
 *
 * @author Ivan Rodriguez
 */
class ConfigDirectorAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, ConfigDirectorAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed but never registered with
	// the
	// OpenFeature API, so these tests assert pure wiring and configuration assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ConfigDirectorAutoConfiguration.class));

	static final String[] requiredProperties = new String[] { CONFIGDIRECTOR_PREFIX + ".api-key=fake-api-key" };

	@Test
	void shouldSupplyDefaultBeans() {
		// Refused localhost endpoint: initialization fails fast locally, without reaching
		// the
		// ConfigDirector service.
		this.contextRunner
			.withPropertyValues(add(requiredProperties, CONFIGDIRECTOR_PREFIX + ".url=http://127.0.0.1:1"))
			.run((context) -> {
				assertThat(context).hasSingleBean(ConfigDirectorProviderOptions.class)
					.hasSingleBean(FeatureProvider.class)
					.hasBean("configDirectorProvider")
					.hasSingleBean(Client.class)
					.hasBean("client");
				FeatureProvider provider = context.getBean(FeatureProvider.class);
				assertThat(provider).isInstanceOf(ConfigDirectorProvider.class);
				// Stop the background eventsource so its threads terminate.
				provider.shutdown();
			});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, CONFIGDIRECTOR_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("configDirectorProvider")
				.doesNotHaveBean(ConfigDirectorProviderOptions.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("configDirectorProvider")
				.doesNotHaveBean(ConfigDirectorProviderOptions.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailWhenApiKeyMissing() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
				.hasMessageContaining(CONFIGDIRECTOR_PREFIX + ".api-key");
		});
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AtomicReference<ClientOptions> captured = new AtomicReference<>();

		// The customizer runs against the very ClientOptions the provider is built with,
		// so a customizer that never ran (or ran against a different instance) leaves the
		// capture null.
		this.optionsRunner.withPropertyValues(requiredProperties)
			.withBean(ConfigDirectorCustomizer.class,
					() -> (ConfigDirectorCustomizer) (options) -> captured.set(options))
			.run((context) -> {
				assertThat(captured.get()).isNotNull();
				assertThat(context.getBean(ConfigDirectorProviderOptions.class).getApiKey()).isEqualTo("fake-api-key");
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(ConfigDirectorProvider.class);
			});
	}

	@Test
	void shouldBindPropertiesToBuiltConfiguration() {
		this.optionsRunner
			.withPropertyValues(new String[] { CONFIGDIRECTOR_PREFIX + ".api-key=fake-api-key",
					CONFIGDIRECTOR_PREFIX + ".url=http://127.0.0.1:1" })
			.run((context) -> {
				assertThat(context.getBean(ConfigDirectorProperties.class).isEnabled()).isTrue();
				ConfigDirectorProviderOptions options = context.getBean(ConfigDirectorProviderOptions.class);
				assertThat(options.getApiKey()).isEqualTo("fake-api-key");
				assertThat(options.getUrl()).isEqualTo("http://127.0.0.1:1");
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(ConfigDirectorProvider.class);
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
