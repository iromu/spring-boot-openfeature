/*
 * Copyright 2025-2025 the original author or authors.
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

package org.iromu.openfeature.boot.autoconfigure.unleash;

import dev.openfeature.contrib.providers.unleash.UnleashProviderConfig;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import io.getunleash.UnleashContext;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.unleash.UnleashCustomizer;
import org.iromu.openfeature.boot.unleash.UnleashProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.unleash.UnleashProperties.UNLEASH_PREFIX;

/**
 * Tests for {@link UnleashAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
@SuppressWarnings({ "NullableProblems", "DataFlowIssue" })
class UnleashAutoConfigurationTest {

	private static MockWebServer mockWebServer;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, UnleashAutoConfiguration.class));
	static String[] requiredProperties;

	@BeforeAll
	public static void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		requiredProperties = new String[] { UNLEASH_PREFIX + ".appName=Foo",
				UNLEASH_PREFIX + ".unleashAPI=" + mockWebServer.url("/api") };
	}

	@AfterAll
	public static void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	void shouldSupplyDefaultBeans() {
		mockWebServer.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				String path = request.getPath();
				if ("/api/client/register".equals(path)) {
					return new MockResponse().setResponseCode(200);
				}
				else if ("/api/client/features".equals(path)) {
					return new MockResponse().setBody("{\"version\":1,\"features\":[]}")
						.addHeader("Content-Type", "application/json")
						.setResponseCode(200);
				}
				else {
					return new MockResponse().setResponseCode(404);
				}
			}
		});

		this.contextRunner.withPropertyValues(requiredProperties).run((context) -> {
			assertThat(context).hasSingleBean(UnleashProviderConfig.class)
				.hasBean("unleashProviderConfig")
				.hasSingleBean(FeatureProvider.class)
				.hasBean("unleashProvider")
				.hasSingleBean(Client.class)
				.hasBean("client");

			Set<String> paths = Set.of(mockWebServer.takeRequest().getPath(), mockWebServer.takeRequest().getPath());
			assertThat(paths).containsExactlyInAnyOrder("/api/client/features", "/api/client/register");
		});
	}

	@Test
	void shouldConfigureUnleashWithCustomizer() {
		this.contextRunner.withUserConfiguration(CustomizerConfiguration.class)
			.withPropertyValues(requiredProperties)
			.run((context) -> {
				UnleashProviderConfig unleashProviderConfig = context.getBean(UnleashProviderConfig.class);
				UnleashConfig unleashConfig = unleashProviderConfig.getUnleashConfigBuilder().build();

				assertThat(unleashConfig).extracting("fallbackStrategy")
					.hasFieldOrPropertyWithValue("name", "a_fallback_for Foo");
			});
	}

	@Test
	void shouldConfigureUnleashWithToken() {
		this.contextRunner.withPropertyValues(add(requiredProperties, UNLEASH_PREFIX + ".unleashToken=token"))
			.run((context) -> {
				UnleashProviderConfig unleashProviderConfig = context.getBean(UnleashProviderConfig.class);
				UnleashConfig unleashConfig = unleashProviderConfig.getUnleashConfigBuilder().build();

				assertThat(unleashConfig).extracting("customHttpHeaders")
					.hasFieldOrPropertyWithValue("Authorization", "token");
			});
	}

	private String[] add(String[] array, String element) {
		return Stream.concat(Arrays.stream(array), Stream.of(element)).toArray(String[]::new);
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		public UnleashCustomizer unleashFallbackCustomizer(
				@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") UnleashProperties properties) {
			return builder -> builder.fallbackStrategy(new Strategy() {

				@Override
				public String getName() {
					return "a_fallback_for " + properties.getAppName();
				}

				@Override
				public boolean isEnabled(Map<String, String> parameters, UnleashContext context) {
					return true;
				}

			});
		}

	}

}
