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

package org.iromu.openfeature.boot.autoconfigure.jsonlogic;

import dev.openfeature.contrib.providers.jsonlogic.RuleFetcher;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.Value;
import io.github.jamsesso.jsonlogic.JsonLogic;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.jsonlogic.JsonlogicProperties.JSONLOGIC_PREFIX;

/**
 * Tests for {@link JsonlogicAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class JsonlogicAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, JsonlogicAutoConfiguration.class));

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(JsonLogic.class)
				.hasBean("jsonLogic")
				.hasSingleBean(RuleFetcher.class)
				.hasBean("noopFetcher")
				.hasSingleBean(FeatureProvider.class)
				.hasBean("jsonlogicProvider")
				.hasSingleBean(Client.class)
				.hasBean("client");
			assertThat(context.getBean(RuleFetcher.class).getRuleForKey("foo")).isNull();
		});
	}

	@Test
	void shouldUseFileFetcherBeans() {
		this.contextRunner.withPropertyValues(JSONLOGIC_PREFIX + ".filename=classpath:/jsonlogic/many-types.json")
			.run((context) -> {
				assertThat(context).hasSingleBean(RuleFetcher.class).hasBean("fileBasedFetcher");

				assertThat(context.getBean(RuleFetcher.class)).extracting("rules.map")
					.asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
					.containsKey("if");
			});
	}

	@Test
	@DisabledIf("ci")
	void shouldApplyLogicBeans() {
		this.contextRunner.withPropertyValues(JSONLOGIC_PREFIX + ".filename=classpath:/jsonlogic/test-rules.json")
			.run((context) -> {
				assertThat(context).hasSingleBean(RuleFetcher.class).hasBean("fileBasedFetcher");
				RuleFetcher ruleFetcher = context.getBean(RuleFetcher.class);
				assertThat(ruleFetcher).extracting("rules.map")
					.asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
					.containsKey("should-have-dessert");

				assertThat(ruleFetcher.getRuleForKey("should-have-dessert")).isEqualTo(
						"{\"if\":[{\"or\":[{\"in\":[{\"var\":\"userId\"},[1,2,3,4]]},{\"in\":[{\"var\":\"category\"},[\"pies\",\"cakes\"]]}]},true,false]}");

				assertThat(context).hasSingleBean(Client.class).hasBean("client");
				Client client = context.getBean(Client.class);

				assertThat(client.getBooleanValue("should-have-dessert", false,
						new ImmutableContext(Collections.singletonMap("userId", new Value(2)))))
					.isTrue();

				assertThat(client.getBooleanValue("should-have-dessert", false,
						new ImmutableContext(Collections.singletonMap("userId", new Value(5)))))
					.isFalse();

			});
	}

	boolean ci() {
		String ciEnvironment = System.getenv("GITHUB_ACTION");
		return ciEnvironment != null && !ciEnvironment.isEmpty();
	}

}
