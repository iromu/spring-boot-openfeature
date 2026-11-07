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

package org.iromu.openfeature.boot.autoconfigure.optimizely;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import dev.openfeature.contrib.providers.optimizely.OptimizelyProviderConfig;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.ProviderEvaluation;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.optimizely.OptimizelyCustomizer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.optimizely.OptimizelyProperties.OPTIMIZELY_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link OptimizelyAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class OptimizelyAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, OptimizelyAutoConfiguration.class));

	static String datafile;

	static String[] requiredProperties;

	@BeforeAll
	static void loadTestDatafile() throws IOException {
		datafile = Files.readString(Path.of("src/test/resources/optimizely-datafile.json"), StandardCharsets.UTF_8)
			.trim();
		requiredProperties = new String[] { OPTIMIZELY_PREFIX + ".datafile=" + datafile };
	}

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.hasBean("optimizelyProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldEvaluateFlagFromDatafile() {
		this.contextRunner.withPropertyValues(requiredProperties).run((context) -> {
			Client client = context.getBean(Client.class);
			MutableContext evaluationContext = new MutableContext();
			evaluationContext.setTargetingKey("user-1");
			// The datafile is the only config source; a valid datafile makes the
			// 100%-traffic rollout resolve test_feature to true, so a null/invalid
			// datafile would fall back to the default (false) and fail this assertion.
			assertThat(client.getBooleanValue("test_feature", false, evaluationContext)).isTrue();
		});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(OPTIMIZELY_PREFIX + ".enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("optimizelyProvider"));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("optimizelyProvider")
				.hasSingleBean(Client.class));
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AppliedCustomizer.RESET.run();
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(CustomizerConfiguration.class)
			.run((context) -> assertThat(AppliedCustomizer.APPLIED.get()).isTrue());
	}

	@Test
	void shouldBindDatafileProperty() {
		DatafileCapturingCustomizer.RESET.run();
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(DatafileCapturingConfiguration.class)
			.run((context) -> assertThat(DatafileCapturingCustomizer.CAPTURED.get()).isEqualTo(datafile));
	}

	static class AppliedCustomizer {

		static final AtomicBoolean APPLIED = new AtomicBoolean(false);

		static final Runnable RESET = () -> APPLIED.set(false);

	}

	static class DatafileCapturingCustomizer {

		static final AtomicReference<String> CAPTURED = new AtomicReference<>();

		static final Runnable RESET = () -> CAPTURED.set(null);

	}

	@Configuration(proxyBeanMethods = false)
	static class UserProviderConfiguration {

		@Bean
		public FeatureProvider featureProvider() {
			return userProvider();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		// The user-supplied provider makes the auto-config provider back off, so the real
		// OptimizelyProvider (which starts vendor machinery on initialize) is never
		// constructed.
		@Bean
		public FeatureProvider featureProvider() {
			return userProvider();
		}

		@Bean
		public OptimizelyCustomizer optimizelyRecordingCustomizer() {
			return builder -> AppliedCustomizer.APPLIED.set(true);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DatafileCapturingConfiguration {

		// The user-supplied provider makes the auto-config provider back off, so the real
		// OptimizelyProvider is never constructed.
		@Bean
		public FeatureProvider featureProvider() {
			return userProvider();
		}

		@Bean
		public OptimizelyCustomizer optimizelyDatafileCapturingCustomizer() {
			return builder -> {
				OptimizelyProviderConfig config = builder.build();
				DatafileCapturingCustomizer.CAPTURED.set(config.getDatafile());
			};
		}

	}

	private static FeatureProvider userProvider() {
		FeatureProvider mock = Mockito.mock(FeatureProvider.class);
		Mockito.when(mock.getMetadata()).thenReturn(() -> "UserFeatureProvider");
		Mockito.when(mock.getBooleanEvaluation(any(), any(), any()))
			.thenReturn(ProviderEvaluation.<Boolean>builder().value(true).build());
		return mock;
	}

}
