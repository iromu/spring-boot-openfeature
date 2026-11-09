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

package org.iromu.openfeature.boot.autoconfigure.octopusdeploy;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.octopus.openfeature.provider.OctopusFeatureConfiguration;
import com.octopus.openfeature.provider.OctopusFeatureProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.octopusdeploy.OctopusDeployCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.octopusdeploy.OctopusDeployProperties.OCTOPUSDEPLOY_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link OctopusDeployAutoConfiguration}.
 * <p>
 * The suite never reaches Octopus Deploy: the provider constructor is lazy (no network
 * calls), and the tests only assert wiring, configuration assembly, and back-off
 * behavior.
 *
 * @author Ivan Rodriguez
 */
class OctopusDeployAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, OctopusDeployAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed but never registered with
	// the OpenFeature API, so these tests assert pure wiring and configuration assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OctopusDeployAutoConfiguration.class));

	static final String[] requiredProperties = new String[] {
			OCTOPUSDEPLOY_PREFIX + ".client-identifier=fake-api-key-abc123",
			OCTOPUSDEPLOY_PREFIX + ".product-name=my-app" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties).run((context) -> {
			assertThat(context).hasSingleBean(OctopusFeatureConfiguration.class)
				.hasSingleBean(FeatureProvider.class)
				.hasBean("octopusDeployProvider")
				.hasSingleBean(Client.class)
				.hasBean("client");
			assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(OctopusFeatureProvider.class);
		});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, OCTOPUSDEPLOY_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("octopusDeployProvider")
				.doesNotHaveBean(OctopusFeatureConfiguration.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("octopusDeployProvider")
				.doesNotHaveBean(OctopusFeatureConfiguration.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailWhenClientIdentifierMissing() {
		this.contextRunner.withPropertyValues(new String[] { OCTOPUSDEPLOY_PREFIX + ".product-name=my-app" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
					.hasMessageContaining(OCTOPUSDEPLOY_PREFIX + ".client-identifier");
			});
	}

	@Test
	void shouldFailWhenProductNameMissing() {
		this.contextRunner.withPropertyValues(new String[] { OCTOPUSDEPLOY_PREFIX + ".client-identifier=fake-key" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
					.hasMessageContaining(OCTOPUSDEPLOY_PREFIX + ".product-name");
			});
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AtomicReference<OctopusFeatureConfiguration> captured = new AtomicReference<>();

		this.optionsRunner.withPropertyValues(requiredProperties)
			.withBean(OctopusDeployCustomizer.class, () -> (OctopusDeployCustomizer) (config) -> {
				config.setCacheDuration(java.time.Duration.ofSeconds(30));
				captured.set(config);
			})
			.run((context) -> {
				assertThat(captured.get()).isNotNull();
				assertThat(context.getBean(OctopusFeatureConfiguration.class).getCacheDuration())
					.isEqualTo(java.time.Duration.ofSeconds(30));
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(OctopusFeatureProvider.class);
			});
	}

	@Test
	void shouldBindProductVersionProperty() {
		this.optionsRunner.withPropertyValues(requiredProperties)
			.withPropertyValues(OCTOPUSDEPLOY_PREFIX + ".product-version=1.2.3")
			.run((context) -> {
				OctopusFeatureConfiguration config = context.getBean(OctopusFeatureConfiguration.class);
				assertThat(config.getProductMetadata().getName()).isEqualTo("my-app");
				assertThat(config.getProductMetadata().getVersion()).contains("1.2.3");
			});
	}

	@Test
	void shouldBindCacheDurationProperty() {
		this.optionsRunner.withPropertyValues(requiredProperties)
			.withPropertyValues(OCTOPUSDEPLOY_PREFIX + ".cache-duration=5m")
			.run((context) -> {
				OctopusFeatureConfiguration config = context.getBean(OctopusFeatureConfiguration.class);
				assertThat(config.getCacheDuration()).isEqualTo(java.time.Duration.ofMinutes(5));
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
