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

package org.iromu.openfeature.boot.autoconfigure.split;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.ProviderEvaluation;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactoryBuilder;
import io.split.openfeature.SplitProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.split.SplitCustomizer;
import org.iromu.openfeature.boot.split.SplitProviderOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.iromu.openfeature.boot.split.SplitProperties.SPLIT_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Tests for {@link SplitAutoConfiguration}.
 * <p>
 * The Split provider constructor opens a streaming connection and blocks until the client
 * is ready, so the back-off/disabled/option-assembly tests keep the real provider
 * unconstructed. The default-bean test instead exercises the genuine happy path through
 * the Split SDK's localhost (file-based) mode, which needs no Split credentials and makes
 * no network calls (design.md D3).
 *
 * @author Ivan Rodriguez
 */
class SplitAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, SplitAutoConfiguration.class));

	static String[] requiredProperties = new String[] { SPLIT_PREFIX + ".authorization-key=test-key" };

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(SPLIT_PREFIX + ".enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("splitProvider")
				.doesNotHaveBean(SplitProviderOptions.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("splitProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldSupplyDefaultBeansFromLocalhostSplitFile(@TempDir Path tempDir) throws Exception {
		Path splitFile = tempDir.resolve("splits.yaml");
		Files.writeString(splitFile, "- random-boolean-flag:\n    treatment: \"on\"\n    keys:\n      - user-1\n");

		this.contextRunner.withPropertyValues(SPLIT_PREFIX + ".authorization-key=localhost")
			.withBean(SplitCustomizer.class,
					() -> (SplitCustomizer) (builder) -> builder.splitFile(splitFile.toString())
						.setBlockUntilReadyTimeout(10000)
						.disableIPAddress())
			.run((context) -> {
				assertThat(context).hasSingleBean(SplitProviderOptions.class)
					.hasSingleBean(FeatureProvider.class)
					.hasBean("splitProvider")
					.hasSingleBean(Client.class)
					.hasBean("client");
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(SplitProvider.class);

				ImmutableContext evaluationContext = new ImmutableContext("user-1");
				assertThat(
						context.getBean(Client.class).getBooleanValue("random-boolean-flag", false, evaluationContext))
					.isTrue();
			});
	}

	@Test
	void shouldAssembleDefaultOptionsWithAuthorizationKey() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> {
				SplitProviderOptions options = context.getBean(SplitProviderOptions.class);
				assertThat(options.getAuthorizationKey()).isEqualTo("test-key");
				assertThat(options.getClientConfig()).isNotNull();
			});
	}

	@Test
	void shouldBindDomainFromProperties() {
		this.contextRunner.withPropertyValues(add(requiredProperties, SPLIT_PREFIX + ".domain=custom.domain.com"))
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> {
				SplitClientConfig config = context.getBean(SplitProviderOptions.class).getClientConfig();
				assertThat(config.endpoint()).isEqualTo("custom.domain.com");
				assertThat(config.eventsEndpoint()).isEqualTo("custom.domain.com");
			});
	}

	@Test
	void shouldApplyCustomizerToAssembledConfiguration() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class, CustomizerConfiguration.class)
			.run((context) -> assertThat(
					context.getBean(SplitProviderOptions.class).getClientConfig().blockUntilReady())
				.isEqualTo(4321));
	}

	@Test
	void shouldFailFastWhenAuthorizationKeyIsMissing() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
				.hasMessageContaining(SPLIT_PREFIX + ".authorization-key");
		});
	}

	@Test
	void shouldFailFastWhenAuthorizationKeyIsBlank() {
		assertThatThrownBy(() -> new SplitAutoConfiguration()
			.splitProvider(new SplitProviderOptions("   ", SplitClientConfig.builder().build())))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(SPLIT_PREFIX + ".authorization-key");
	}

	@Test
	void shouldWrapSplitFactoryCreationFailures() {
		try (MockedStatic<SplitFactoryBuilder> splitFactoryBuilder = Mockito.mockStatic(SplitFactoryBuilder.class)) {
			splitFactoryBuilder.when(() -> SplitFactoryBuilder.build(anyString(), any(SplitClientConfig.class)))
				.thenThrow(new IOException("factory creation failed"));

			assertThatThrownBy(() -> new SplitAutoConfiguration()
				.splitProvider(new SplitProviderOptions("test-key", SplitClientConfig.builder().build())))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Unable to create the Split factory")
				.hasRootCauseInstanceOf(IOException.class);
		}
	}

	private String[] add(String[] array, String element) {
		return Stream.concat(Arrays.stream(array), Stream.of(element)).toArray(String[]::new);
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

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		public SplitCustomizer splitCustomizer() {
			return (builder) -> builder.setBlockUntilReadyTimeout(4321);
		}

	}

}
