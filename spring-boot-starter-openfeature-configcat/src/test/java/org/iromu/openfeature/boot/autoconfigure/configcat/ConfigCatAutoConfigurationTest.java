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

package org.iromu.openfeature.boot.autoconfigure.configcat;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.openfeature.contrib.providers.configcat.ConfigCatProviderConfig;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.configcat.ConfigCatCustomizer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.configcat.ConfigCatProperties.CONFIGCAT_PREFIX;
import static org.mockito.ArgumentMatchers.any;

class ConfigCatAutoConfigurationTest {

	public static final String[] REQUIRED = { CONFIGCAT_PREFIX + ".sdk-key=dummy",
			CONFIGCAT_PREFIX + ".filename=configcat/features.json" };

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, ConfigCatAutoConfiguration.class));

	@Test
	void shouldSupplyDefaultBeansWithOptionalClasspathFile() {
		this.contextRunner
			.withPropertyValues(CONFIGCAT_PREFIX + ".sdk-key=dummy",
					CONFIGCAT_PREFIX + ".filename=classpath:/configcat/features.json")
			.run((context) -> assertThat(context).hasSingleBean(ConfigCatProviderConfig.class)
				.hasBean("configCatProviderConfig")
				.hasSingleBean(FeatureProvider.class)
				.hasBean("configCatProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldUseFileRules() {
		this.contextRunner.withPropertyValues(REQUIRED)
			.run((context) -> assertThat(context.getBean(Client.class).getBooleanValue("enabledFeature", false))
				.isTrue());
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(CONFIGCAT_PREFIX + ".enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("configCatProvider"));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(CONFIGCAT_PREFIX + ".sdk-key=dummy")
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("configCatProvider")
				.hasSingleBean(Client.class));
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AppliedCustomizer.RESET.run();
		this.contextRunner.withPropertyValues(CONFIGCAT_PREFIX + ".sdk-key=dummy")
			.withUserConfiguration(CustomizerConfiguration.class)
			.run((context) -> {
				ConfigCatProviderConfig config = context.getBean(ConfigCatProviderConfig.class);
				// The customizer is wired into the config's deferred options consumer;
				// applying it
				// to the options must invoke the registered customizer.
				config.getOptions().accept(null);
				assertThat(AppliedCustomizer.APPLIED.get()).isTrue();
			});
	}

	static class AppliedCustomizer {

		static final AtomicBoolean APPLIED = new AtomicBoolean(false);

		static final Runnable RESET = () -> APPLIED.set(false);

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

		// The user-supplied provider makes the auto-config provider back off, so the real
		// ConfigCatProvider (which validates the SDK key) is never constructed.
		@Bean
		public FeatureProvider featureProvider() {
			FeatureProvider mock = Mockito.mock(FeatureProvider.class);
			Mockito.when(mock.getMetadata()).thenReturn(() -> "UserFeatureProvider");
			Mockito.when(mock.getBooleanEvaluation(any(), any(), any()))
				.thenReturn(ProviderEvaluation.<Boolean>builder().value(true).build());
			return mock;
		}

		@Bean
		public ConfigCatCustomizer configCatRecordingCustomizer() {
			return options -> AppliedCustomizer.APPLIED.set(true);
		}

	}

}
