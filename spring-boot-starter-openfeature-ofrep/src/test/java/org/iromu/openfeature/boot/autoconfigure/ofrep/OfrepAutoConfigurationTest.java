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

package org.iromu.openfeature.boot.autoconfigure.ofrep;

import java.time.Duration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.openfeature.contrib.providers.ofrep.OfrepProviderOptions;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.ofrep.OfrepCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.ofrep.OfrepProperties.OFREP_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link OfrepAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class OfrepAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, OfrepAutoConfiguration.class));

	static String[] requiredProperties = new String[] { OFREP_PREFIX + ".base-url=http://localhost:8080",
			OFREP_PREFIX + ".connect-timeout=500ms", OFREP_PREFIX + ".request-timeout=500ms" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.hasBean("ofrepProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(OFREP_PREFIX + ".enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("ofrepProvider"));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("ofrepProvider")
				.hasSingleBean(Client.class));
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(CustomizerConfiguration.class)
			.run((context) -> assertThat(context.getBean(OfrepProviderOptions.class)).extracting("baseUrl")
				.isEqualTo("http://localhost:9999"));
	}

	@Test
	void shouldBindEndpointProperties() {
		BoundOptions.RESET.run();
		this.contextRunner
			.withPropertyValues(OFREP_PREFIX + ".base-url=http://localhost:8080",
					OFREP_PREFIX + ".connect-timeout=123ms", OFREP_PREFIX + ".request-timeout=456ms",
					OFREP_PREFIX + ".headers.Authorization=Bearer token")
			.withUserConfiguration(CapturingCustomizerConfiguration.class)
			.run((context) -> {
				assertThat(BoundOptions.baseUrls).isEqualTo("http://localhost:8080");
				assertThat(BoundOptions.connectTimeout).isEqualTo(Duration.ofMillis(123));
				assertThat(BoundOptions.requestTimeout).isEqualTo(Duration.ofMillis(456));
				assertThat(BoundOptions.headers).containsEntry("Authorization", ImmutableList.of("Bearer token"));
			});
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

		@Bean
		public OfrepCustomizer ofrepBaseUrlCustomizer() {
			return builder -> builder.baseUrl("http://localhost:9999");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CapturingCustomizerConfiguration {

		// The customizer runs after property binding, so a snapshot of the builder
		// captures
		// the values the auto-configuration set from the bound properties.
		@Bean
		public OfrepCustomizer ofrepCapturingCustomizer() {
			return builder -> {
				OfrepProviderOptions snapshot = builder.build();
				BoundOptions.baseUrls = snapshot.getBaseUrl();
				BoundOptions.connectTimeout = snapshot.getConnectTimeout();
				BoundOptions.requestTimeout = snapshot.getRequestTimeout();
				BoundOptions.headers = snapshot.getHeaders();
			};
		}

	}

	static class BoundOptions {

		static String baseUrls;

		static Duration connectTimeout;

		static Duration requestTimeout;

		static ImmutableMap<String, ImmutableList<String>> headers;

		static final Runnable RESET = () -> {
			baseUrls = null;
			connectTimeout = null;
			requestTimeout = null;
			headers = null;
		};

	}

	private static FeatureProvider userProvider() {
		FeatureProvider mock = Mockito.mock(FeatureProvider.class);
		Mockito.when(mock.getMetadata()).thenReturn(() -> "UserFeatureProvider");
		Mockito.when(mock.getBooleanEvaluation(any(), any(), any()))
			.thenReturn(ProviderEvaluation.<Boolean>builder().value(true).build());
		return mock;
	}

}
