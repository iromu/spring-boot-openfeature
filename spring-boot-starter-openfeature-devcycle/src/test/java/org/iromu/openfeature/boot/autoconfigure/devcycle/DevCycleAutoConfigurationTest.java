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

package org.iromu.openfeature.boot.autoconfigure.devcycle;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.devcycle.sdk.server.cloud.model.DevCycleCloudOptions;
import com.devcycle.sdk.server.openfeature.DevCycleProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.ProviderEvaluation;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.devcycle.DevCycleCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.devcycle.DevCycleProperties.DEVCYCLE_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link DevCycleAutoConfiguration}.
 * <p>
 * The suite never reaches DevCycle: the happy path redirects the client to a local
 * {@link MockWebServer} through the base URL override (design.md findings — the cloud
 * client opens no connection at construction time, and there is no background event
 * delivery to switch off), and the user-provider back-off keeps the provider
 * unconstructed.
 *
 * @author Ivan Rodriguez
 */
class DevCycleAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, DevCycleAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed but never registered, so
	// these tests assert pure wiring and configuration assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DevCycleAutoConfiguration.class));

	static final String[] requiredProperties = new String[] { DEVCYCLE_PREFIX + ".server-side-key=dvc_server_fake" };

	@Test
	void shouldSupplyDefaultBeans() throws Exception {
		try (MockWebServer server = new MockWebServer()) {
			server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
				.setBody("{\"key\":\"random-boolean-flag\",\"value\":true,\"type\":\"Boolean\",\"defaultValue\":false,"
						+ "\"isDefaulted\":false}"));

			this.contextRunner
				.withPropertyValues(add(requiredProperties, DEVCYCLE_PREFIX + ".base-url-override=" + server.url("/")))
				.run((context) -> {
					assertThat(context).hasSingleBean(DevCycleCloudOptions.class)
						.hasSingleBean(FeatureProvider.class)
						.hasBean("devCycleProvider")
						.hasSingleBean(Client.class)
						.hasBean("client");
					assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(DevCycleProvider.class);
					Client client = context.getBean(Client.class);
					client.setEvaluationContext(new ImmutableContext("test-user"));
					assertThat(client.getBooleanValue("random-boolean-flag", false)).isTrue();
				});
		}
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, DEVCYCLE_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("devCycleProvider")
				.doesNotHaveBean(DevCycleCloudOptions.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("devCycleProvider")
				.doesNotHaveBean(DevCycleCloudOptions.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldFailWhenServerSideKeyMissing() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
				.hasMessageContaining(DEVCYCLE_PREFIX + ".server-side-key");
		});
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AtomicReference<DevCycleCloudOptions.DevCycleCloudOptionsBuilder> captured = new AtomicReference<>();

		// enableEdgeDB is set only by the customizer, so a customizer that never ran (or
		// ran against a different builder) leaves the SDK default and fails the
		// assertion.
		this.optionsRunner.withPropertyValues(requiredProperties)
			.withBean(DevCycleCustomizer.class, () -> (DevCycleCustomizer) (builder) -> {
				builder.enableEdgeDB(true);
				captured.set(builder);
			})
			.run((context) -> {
				assertThat(captured.get()).isNotNull();
				assertThat(context.getBean(DevCycleCloudOptions.class).getEnableEdgeDB()).isTrue();
			});
	}

	@Test
	void shouldBindBaseUrlOverrideProperty() {
		this.optionsRunner
			.withPropertyValues(add(requiredProperties, DEVCYCLE_PREFIX + ".base-url-override=https://example.org/api"))
			.run((context) -> assertThat(context.getBean(DevCycleCloudOptions.class).getBaseURLOverride())
				.isEqualTo("https://example.org/api"));
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
