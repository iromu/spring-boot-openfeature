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

package org.iromu.openfeature.boot.autoconfigure.ageris;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.github.lichtsinn.ageris.core.Ageris;
import io.github.lichtsinn.ageris.openfeature.AgerisProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.ageris.AgerisCustomizer;
import org.iromu.openfeature.boot.ageris.AgerisProviderOptions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.ageris.AgerisProperties.AGERIS_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link AgerisAutoConfiguration}.
 * <p>
 * The suite needs no Ageris configuration anywhere but a local temp directory: Ageris is
 * file-based and the provider constructor is lazy, so the assembly tests construct the
 * real provider with fake paths and nothing is read. The one test that registers the
 * provider with the OpenFeature API (which starts the configuration loader on
 * initialization) points it at real temp files, asserts a flag lookup against them, and
 * shuts the provider down so the loader's non-daemon executor threads terminate. The
 * disabled, back-off and missing-property tests never construct the real provider at all.
 *
 * @author Ivan Rodriguez
 */
class AgerisAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, AgerisAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed but never registered with
	// the
	// OpenFeature API, so these tests assert pure wiring and configuration assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AgerisAutoConfiguration.class));

	static final String[] requiredProperties = new String[] { AGERIS_PREFIX + ".application=fake-app",
			AGERIS_PREFIX + ".path=/tmp/fake-ageris-config", AGERIS_PREFIX + ".feature-flag-files=featureflags.json" };

	@Test
	void shouldSupplyDefaultBeans() throws Exception {
		// Real configuration files in a temp directory: provider initialization loads
		// them,
		// which proves the assembled provider answers lookups end to end. The fetcher
		// reads from <path>/<application>/<file>.
		Path configDir = Files.createTempDirectory("ageris-test");
		Path appDir = Files.createDirectories(configDir.resolve("fake-app"));
		Files.writeString(appDir.resolve("featureflags.json"),
				"{ \"feature-flags\": { \"welcome-flag\": { \"value\": true } } }");
		Files.writeString(appDir.resolve("configs.json"),
				"{ \"configs\": { \"greeting\": { \"value\": \"hello\" } } }");

		this.contextRunner
			.withPropertyValues(new String[] { AGERIS_PREFIX + ".application=fake-app",
					AGERIS_PREFIX + ".path=" + configDir, AGERIS_PREFIX + ".feature-flag-files=featureflags.json",
					AGERIS_PREFIX + ".json-config-files=configs.json" })
			.run((context) -> {
				assertThat(context).hasSingleBean(AgerisProviderOptions.class)
					.hasSingleBean(FeatureProvider.class)
					.hasBean("agerisProvider")
					.hasSingleBean(Client.class)
					.hasBean("client");
				FeatureProvider provider = context.getBean(FeatureProvider.class);
				assertThat(provider).isInstanceOf(AgerisProvider.class);
				assertThat(context.getBean(Client.class).getBooleanValue("welcome-flag", false)).isTrue();
				// Stop the configuration loader so its non-daemon executor threads
				// terminate.
				provider.shutdown();
			});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, AGERIS_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("agerisProvider")
				.doesNotHaveBean(AgerisProviderOptions.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("agerisProvider")
				.doesNotHaveBean(AgerisProviderOptions.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailWhenApplicationMissing() {
		this.contextRunner
			.withPropertyValues(new String[] { AGERIS_PREFIX + ".path=/tmp/fake-ageris-config",
					AGERIS_PREFIX + ".feature-flag-files=featureflags.json" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
					.hasMessageContaining(AGERIS_PREFIX + ".application");
			});
	}

	@Test
	void shouldFailWhenPathMissing() {
		this.contextRunner
			.withPropertyValues(new String[] { AGERIS_PREFIX + ".application=fake-app",
					AGERIS_PREFIX + ".feature-flag-files=featureflags.json" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
					.hasMessageContaining(AGERIS_PREFIX + ".path");
			});
	}

	@Test
	void shouldFailWhenFeatureFlagFilesMissing() {
		this.contextRunner
			.withPropertyValues(new String[] { AGERIS_PREFIX + ".application=fake-app",
					AGERIS_PREFIX + ".path=/tmp/fake-ageris-config" })
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
					.hasMessageContaining(AGERIS_PREFIX + ".feature-flag-files");
			});
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AtomicReference<Ageris> captured = new AtomicReference<>();

		this.optionsRunner.withPropertyValues(requiredProperties)
			.withBean(AgerisCustomizer.class, () -> (AgerisCustomizer) (ageris) -> captured.set(ageris))
			.run((context) -> {
				assertThat(captured.get()).isNotNull();
				assertThat(context.getBean(AgerisProviderOptions.class).getFeatureFlagManager()).isNotNull();
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(AgerisProvider.class);
			});
	}

	@Test
	void shouldBindPropertiesToBuiltConfiguration() {
		this.optionsRunner
			.withPropertyValues(new String[] { AGERIS_PREFIX + ".application=fake-app",
					AGERIS_PREFIX + ".path=/tmp/fake-ageris-config",
					AGERIS_PREFIX + ".feature-flag-files=featureflags.json",
					AGERIS_PREFIX + ".json-config-files=configs.json", AGERIS_PREFIX + ".targeting-key-type=customer",
					AGERIS_PREFIX + ".refresh-interval=5s", AGERIS_PREFIX + ".first-load-timeout=7s" })
			.run((context) -> {
				AgerisProviderOptions options = context.getBean(AgerisProviderOptions.class);
				assertThat(options.getFeatureFlagManager()).isNotNull();
				assertThat(options.getJsonConfigManager()).isNotNull();
				assertThat(options.getConfigurationLoader()).isNotNull();
				assertThat(options.getTargetingKeyType()).isEqualTo("customer");
				assertThat(options.getFirstLoadTimeout()).isEqualTo(Duration.ofSeconds(7));
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(AgerisProvider.class);
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
