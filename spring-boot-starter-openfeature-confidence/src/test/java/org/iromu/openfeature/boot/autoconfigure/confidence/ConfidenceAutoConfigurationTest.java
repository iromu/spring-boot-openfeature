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

package org.iromu.openfeature.boot.autoconfigure.confidence;

import java.util.concurrent.atomic.AtomicReference;

import com.spotify.confidence.Confidence;
import com.spotify.confidence.ConfidenceFeatureProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.confidence.ConfidenceCustomizer;
import org.iromu.openfeature.boot.confidence.ConfidenceProviderOptions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.iromu.openfeature.boot.confidence.ConfidenceProperties.CONFIDENCE_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Tests for {@link ConfidenceAutoConfiguration}. Every test runs on a host with no
 * Confidence credentials and never triggers a network call to Confidence: the provider
 * and the underlying gRPC channels are lazy (no connection until the first RPC), and the
 * Confidence provider does not override {@code initialize()}, so the default-bean test
 * constructs the genuine provider without any initialization traffic.
 *
 * @author Ivan Rodriguez
 */
class ConfidenceAutoConfigurationTest {

	private static final String API_TOKEN_PROPERTY = CONFIDENCE_PREFIX + ".api-token=test-api-token";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, ConfidenceAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed but never registered, so
	// these tests assert pure wiring and configuration assembly.
	private final ApplicationContextRunner configRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ConfidenceAutoConfiguration.class));

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(API_TOKEN_PROPERTY).run((context) -> {
			assertThat(context).hasSingleBean(ConfidenceProviderOptions.class)
				.hasBean("confidenceProviderOptions")
				.hasSingleBean(FeatureProvider.class)
				.hasBean("confidenceProvider")
				.hasSingleBean(Client.class)
				.hasBean("client");
			assertThat(context.getBean("confidenceProvider", FeatureProvider.class))
				.isInstanceOf(ConfidenceFeatureProvider.class);
		});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(CONFIDENCE_PREFIX + ".enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("confidenceProvider")
				.doesNotHaveBean(ConfidenceProviderOptions.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(API_TOKEN_PROPERTY)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("confidenceProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldBindHostAndPortFromProperties() {
		try (MockedStatic<Confidence> confidence = Mockito.mockStatic(Confidence.class)) {
			Confidence.Builder builder = Mockito.mock(Confidence.Builder.class);
			confidence.when(() -> Confidence.builder(anyString())).thenReturn(builder);

			this.configRunner
				.withPropertyValues(API_TOKEN_PROPERTY, CONFIDENCE_PREFIX + ".host=resolver.example.com",
						CONFIDENCE_PREFIX + ".port=1234")
				.run((context) -> {
					ConfidenceProviderOptions options = context.getBean(ConfidenceProviderOptions.class);
					assertThat(options.getApiToken()).isEqualTo("test-api-token");
					assertThat(options.getBuilder()).isSameAs(builder);
					Mockito.verify(builder).flagResolverManagedChannel("resolver.example.com", 1234);
				});
		}
	}

	@Test
	void shouldUseDefaultPortWhenOnlyHostIsSet() {
		try (MockedStatic<Confidence> confidence = Mockito.mockStatic(Confidence.class)) {
			Confidence.Builder builder = Mockito.mock(Confidence.Builder.class);
			confidence.when(() -> Confidence.builder(anyString())).thenReturn(builder);

			this.configRunner.withPropertyValues(API_TOKEN_PROPERTY, CONFIDENCE_PREFIX + ".host=resolver.example.com")
				.run((context) -> {
					ConfidenceProviderOptions options = context.getBean(ConfidenceProviderOptions.class);
					assertThat(options.getBuilder()).isSameAs(builder);
					Mockito.verify(builder).flagResolverManagedChannel("resolver.example.com", 443);
				});
		}
	}

	@Test
	void shouldKeepDefaultEndpointWhenHostIsNotSet() {
		try (MockedStatic<Confidence> confidence = Mockito.mockStatic(Confidence.class)) {
			Confidence.Builder builder = Mockito.mock(Confidence.Builder.class);
			confidence.when(() -> Confidence.builder(anyString())).thenReturn(builder);

			this.configRunner.withPropertyValues(API_TOKEN_PROPERTY).run((context) -> {
				ConfidenceProviderOptions options = context.getBean(ConfidenceProviderOptions.class);
				assertThat(options.getApiToken()).isEqualTo("test-api-token");
				assertThat(options.getBuilder()).isSameAs(builder);
				Mockito.verify(builder, Mockito.never()).flagResolverManagedChannel(anyString(), anyInt());
			});
		}
	}

	@Test
	void shouldApplyCustomizerToAssembledConfiguration() {
		AtomicReference<Confidence.Builder> customized = new AtomicReference<>();

		this.configRunner.withPropertyValues(API_TOKEN_PROPERTY)
			.withBean(ConfidenceCustomizer.class, () -> (ConfidenceCustomizer) customized::set)
			.run((context) -> {
				ConfidenceProviderOptions options = context.getBean(ConfidenceProviderOptions.class);
				assertThat(options.getBuilder()).isNotNull();
				assertThat(customized.get()).isSameAs(options.getBuilder());
			});
	}

	@Test
	void shouldFailFastWhenApiTokenIsMissing() {
		this.configRunner.run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
				.hasRootCauseMessage("Confidence feature flags require '" + CONFIDENCE_PREFIX + ".api-token' when '"
						+ CONFIDENCE_PREFIX + ".enabled=true'");
		});
	}

	@Test
	void shouldFailFastWhenApiTokenIsBlank() {
		assertThatThrownBy(() -> new ConfidenceAutoConfiguration()
			.confidenceProvider(new ConfidenceProviderOptions("   ", Mockito.mock(Confidence.Builder.class))))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(CONFIDENCE_PREFIX + ".api-token");
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

}
