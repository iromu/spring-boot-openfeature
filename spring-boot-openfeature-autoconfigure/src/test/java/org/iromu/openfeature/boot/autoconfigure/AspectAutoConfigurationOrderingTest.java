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

package org.iromu.openfeature.boot.autoconfigure;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import org.iromu.openfeature.boot.aop.ToggleOnFlagAspect;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Regression test pinning the auto-configuration ordering between
 * {@link AspectAutoConfiguration} and {@link ClientAutoConfiguration}.
 *
 * <p>
 * {@link ToggleOnFlagAspect} is {@code @ConditionalOnBean(Client.class)}, so the
 * {@code Client} bean definition must be registered before the aspect's condition is
 * evaluated. If {@link AspectAutoConfiguration} loses its
 * {@code @AutoConfigureAfter(ClientAutoConfiguration)}, the alphabetical sort evaluates
 * it first, the aspect's condition is checked before the {@code Client} exists, and the
 * aspect is silently never registered.
 *
 * @author Ivan Rodriguez
 */
class AspectAutoConfigurationOrderingTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, AspectAutoConfiguration.class));

	@Test
	void shouldRegisterAspectWhenClientIsAutoConfigured() {
		this.contextRunner.withUserConfiguration(ProviderConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Client.class);
			assertThat(context).hasSingleBean(ToggleOnFlagAspect.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class ProviderConfiguration {

		@Bean
		public FeatureProvider featureProvider() {
			FeatureProvider mock = Mockito.mock(FeatureProvider.class);
			Mockito.when(mock.getMetadata()).thenReturn(() -> "MockedFeatureProvider");
			Mockito.when(mock.getBooleanEvaluation(any(), any(), any()))
				.thenReturn(ProviderEvaluation.<Boolean>builder().value(true).build());
			return mock;
		}

	}

}
