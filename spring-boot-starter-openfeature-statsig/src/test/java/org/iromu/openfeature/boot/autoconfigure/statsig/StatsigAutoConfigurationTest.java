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

package org.iromu.openfeature.boot.autoconfigure.statsig;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.statsig.StatsigCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.statsig.StatsigProperties.STATSIG_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link StatsigAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class StatsigAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, StatsigAutoConfiguration.class));

	static String[] requiredProperties = new String[] { STATSIG_PREFIX + ".sdkKey=test",
			STATSIG_PREFIX + ".localMode=true" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.hasBean("statsigProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(STATSIG_PREFIX + ".enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("statsigProvider"));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("statsigProvider")
				.hasSingleBean(Client.class));
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AppliedCustomizer.RESET.run();
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(CustomizerConfiguration.class)
			.run((context) -> assertThat(AppliedCustomizer.APPLIED.get()).isTrue());
	}

	static class AppliedCustomizer {

		static final AtomicBoolean APPLIED = new AtomicBoolean(false);

		static final Runnable RESET = () -> APPLIED.set(false);

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
		// StatsigProvider (which runs postInit) is never constructed.
		@Bean
		public FeatureProvider featureProvider() {
			return userProvider();
		}

		@Bean
		public StatsigCustomizer statsigRecordingCustomizer() {
			return builder -> AppliedCustomizer.APPLIED.set(true);
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
