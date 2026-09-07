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

package org.iromu.openfeature.boot.autoconfigure.flipt;

import dev.openfeature.contrib.providers.flipt.FliptProviderConfig;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.ProviderEvaluation;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.flipt.FliptCustomizer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.flipt.FliptProperties.FLIPT_PREFIX;
import static org.mockito.ArgumentMatchers.any;

@SuppressWarnings("NullableProblems")
class FliptAutoConfigurationTest {

	public static final String TARGETING_KEY = "targeting_key";

	public static final String BOOLEAN_PAYLOAD = """
			{
			  "enabled": true,
			  "reason": "UNKNOWN_EVALUATION_REASON",
			  "requestDurationMillis": 1,
			  "requestId": "string",
			  "timestamp": "2022-12-03T10:15:30+01:00"
			}
			""";

	private static MockWebServer mockWebServer;

	private static String[] requiredProperties;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, FliptAutoConfiguration.class));

	@BeforeAll
	public static void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		requiredProperties = new String[] { FLIPT_PREFIX + ".baseURL=" + mockWebServer.url("") };
	}

	@AfterAll
	public static void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(FliptProviderConfig.class)
			.hasBean("fliptProviderConfig")
			.hasSingleBean(FeatureProvider.class)
			.hasBean("fliptProvider")
			.hasSingleBean(Client.class)
			.hasBean("client"));
	}

	@Test
	void shouldEvalBoolean() {
		mockWebServer.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				String path = request.getPath();
				if ("/evaluate/v1/boolean".equals(path)) {
					return new MockResponse().setBody(BOOLEAN_PAYLOAD)
						.addHeader("Content-Type", "application/json")
						.setResponseCode(200);
				}
				else {
					return new MockResponse().setResponseCode(404);
				}
			}
		});
		this.contextRunner.withPropertyValues(requiredProperties).run((context) -> {
			assertThat(context).hasSingleBean(FliptProviderConfig.class)
				.hasBean("fliptProviderConfig")
				.hasSingleBean(FeatureProvider.class)
				.hasBean("fliptProvider")
				.hasSingleBean(Client.class)
				.hasBean("client");

			MutableContext evaluationContext = new MutableContext();
			evaluationContext.setTargetingKey(TARGETING_KEY);

			assertThat(context.getBean(Client.class).getBooleanValue("example", false, evaluationContext)).isTrue();

		});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(FLIPT_PREFIX + ".enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("fliptProvider"));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("fliptProvider")
				.hasSingleBean(Client.class));
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		this.contextRunner.withUserConfiguration(CustomizerConfiguration.class)
			.run((context) -> assertThat(context.getBean(FliptProviderConfig.class)).extracting("namespace")
				.isEqualTo("custom-namespace"));
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
		// FliptProvider is never constructed.
		@Bean
		public FeatureProvider featureProvider() {
			return userProvider();
		}

		@Bean
		public FliptCustomizer fliptNamespaceCustomizer() {
			return builder -> builder.namespace("custom-namespace");
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
