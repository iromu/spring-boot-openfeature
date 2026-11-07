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
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.optimizely.ab.config.HttpProjectConfigManager;
import com.optimizely.ab.config.ProjectConfigManager;
import com.sun.net.httpserver.HttpServer;
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

	// No ClientAutoConfiguration, so the provider is never initialized
	// (OptimizelyProvider
	// only starts its config-manager polling in initialize()); these tests assert pure
	// config assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OptimizelyAutoConfiguration.class));

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

	@Test
	void shouldBuildHttpConfigManagerFromSdkKeyProperties() throws IOException {
		SdkKeyCapturingCustomizer.RESET.run();

		// The vendor's HttpProjectConfigManager.Builder.build() starts its poller and
		// blocks
		// in getConfig() until the first fetch lands, so a fake off-host URL would hang
		// the
		// test for the full blocking timeout. Serve the real datafile from a local server
		// so
		// the first fetch succeeds immediately.
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/", (exchange) -> {
			byte[] body = datafile.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		int port = server.getAddress().getPort();
		String configUrl = "http://localhost:" + port + "/api/v4/optimizely/datafile.json";
		try {
			this.optionsRunner
				.withPropertyValues(OPTIMIZELY_PREFIX + ".sdk-key=test-sdk-key",
						OPTIMIZELY_PREFIX + ".datafile-auth-token=auth-token",
						OPTIMIZELY_PREFIX + ".config-url=" + configUrl, OPTIMIZELY_PREFIX + ".poll-interval=30")
				.withUserConfiguration(SdkKeyCapturingConfiguration.class)
				.run((context) -> {
					OptimizelyProviderConfig config = SdkKeyCapturingCustomizer.CAPTURED.get();
					assertThat(config).isNotNull();
					ProjectConfigManager manager = config.getProjectConfigManager();
					try {
						assertThat(manager).isInstanceOf(HttpProjectConfigManager.class);
						assertThat(((HttpProjectConfigManager) manager).getUri()).isEqualTo(URI.create(configUrl));
						assertThat(manager.getSDKKey()).isEqualTo("test-sdk-key");
					}
					finally {
						// Stop the poller the manager started so the test leaves no live
						// thread.
						((AutoCloseable) manager).close();
					}
				});
		}
		finally {
			server.stop(0);
		}
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

	static class SdkKeyCapturingCustomizer {

		static final AtomicReference<OptimizelyProviderConfig> CAPTURED = new AtomicReference<>();

		static final Runnable RESET = () -> CAPTURED.set(null);

	}

	@Configuration(proxyBeanMethods = false)
	static class SdkKeyCapturingConfiguration {

		// The user-supplied provider makes the auto-config provider back off, so the real
		// OptimizelyProvider (which would start the HTTP config manager polling) is never
		// constructed.
		@Bean
		public FeatureProvider featureProvider() {
			return userProvider();
		}

		@Bean
		public OptimizelyCustomizer optimizelySdkKeyCapturingCustomizer() {
			return builder -> SdkKeyCapturingCustomizer.CAPTURED.set(builder.build());
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
