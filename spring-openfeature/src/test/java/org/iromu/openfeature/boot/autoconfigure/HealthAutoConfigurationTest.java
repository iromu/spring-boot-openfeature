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

import dev.openfeature.sdk.OpenFeatureAPI;
import org.iromu.openfeature.boot.health.OpenFeatureHealthIndicator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class HealthAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HealthAutoConfiguration.class));

	@Test
	void shouldImportHealthIndicatorWhenApiPresent() {
		this.contextRunner.withUserConfiguration(ApiConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(OpenFeatureHealthIndicator.class));
	}

	@Test
	void shouldNotImportHealthIndicatorWhenApiAbsent() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(OpenFeatureHealthIndicator.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class ApiConfiguration {

		@Bean
		public OpenFeatureAPI openFeatureAPI() {
			return Mockito.mock(OpenFeatureAPI.class);
		}

	}

}
