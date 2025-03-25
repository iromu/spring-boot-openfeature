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

package org.iromu.openfeature.boot.autoconfigure.configcat;

import dev.openfeature.contrib.providers.configcat.ConfigCatProviderConfig;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.configcat.ConfigCatProperties.CONFIGCAT_PREFIX;

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

}
