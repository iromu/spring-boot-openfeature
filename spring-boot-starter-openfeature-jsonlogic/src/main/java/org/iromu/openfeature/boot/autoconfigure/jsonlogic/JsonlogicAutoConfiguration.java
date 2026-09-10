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

package org.iromu.openfeature.boot.autoconfigure.jsonlogic;

import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.Nullable;

import dev.openfeature.contrib.providers.jsonlogic.FileBasedFetcher;
import dev.openfeature.contrib.providers.jsonlogic.JsonlogicProvider;
import dev.openfeature.contrib.providers.jsonlogic.RuleFetcher;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FeatureProvider;
import io.github.jamsesso.jsonlogic.JsonLogic;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.jsonlogic.JsonlogicProperties;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for {@link JsonlogicProvider}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore(value = { ClientAutoConfiguration.class },
		name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")
@ConditionalOnClass({ JsonlogicProvider.class })
@ConditionalOnProperty(prefix = JsonlogicProperties.JSONLOGIC_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(JsonlogicProperties.class)
@Slf4j
public class JsonlogicAutoConfiguration {

	/**
	 * Creates the {@link JsonLogic} evaluator used to evaluate JSON Logic rules.
	 * @return the JsonLogic evaluator instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public JsonLogic jsonLogic() {
		return new JsonLogic();
	}

	/**
	 * Creates a {@link FileBasedFetcher} that loads rules from the configured
	 * {@code jsonlogic.filename} location.
	 * @param properties the JsonlogicProperties holding the rules file location
	 * @return the file based RuleFetcher instance
	 */
	@SneakyThrows
	@Bean
	@ConditionalOnProperty(prefix = JsonlogicProperties.JSONLOGIC_PREFIX, name = "filename")
	@ConditionalOnMissingBean
	public RuleFetcher fileBasedFetcher(JsonlogicProperties properties) {
		if (log.isTraceEnabled()) {
			log.trace("jsonlogic.filename={}",
					String.join("\n", Files.readAllLines(Paths.get(properties.getFilename().getURI()))));
		}
		return new FileBasedFetcher(properties.getFilename().getURI());
	}

	/**
	 * Creates a no-operation {@link RuleFetcher} fallback used when no
	 * {@code jsonlogic.filename} is configured.
	 * @return the no-op RuleFetcher instance
	 */
	@Bean
	@ConditionalOnProperty(prefix = JsonlogicProperties.JSONLOGIC_PREFIX, name = "filename", matchIfMissing = true)
	@ConditionalOnMissingBean
	public RuleFetcher noopFetcher() {
		return new RuleFetcher() {
			@Override
			public void initialize(EvaluationContext evaluationContext) {

			}

			@Nullable
			@Override
			public String getRuleForKey(String s) {
				return null;
			}
		};
	}

	/**
	 * Creates the {@link JsonlogicProvider} feature provider backed by the
	 * {@link JsonLogic} evaluator and a {@link RuleFetcher}.
	 * @param logic the JsonLogic evaluator used to evaluate rules
	 * @param fetcher the RuleFetcher used to look up rules by key
	 * @return the JsonlogicProvider feature provider instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public FeatureProvider jsonlogicProvider(JsonLogic logic, RuleFetcher fetcher) {
		return new JsonlogicProvider(logic, fetcher);
	}

}
