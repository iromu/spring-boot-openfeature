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

package org.iromu.openfeature.examples;

import dev.openfeature.contrib.providers.unleash.UnleashProvider;
import dev.openfeature.contrib.providers.unleash.UnleashProviderConfig;
import dev.openfeature.sdk.FeatureProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Sample Configuration.
 *
 * @author Ivan Rodriguez
 */
@Configuration
@Slf4j
public class UnleashConfiguration {

	@SneakyThrows
	@Bean
	MockWebServer unleashApi() {
		MockWebServer mockWebServer = new MockWebServer();
		mockWebServer.start(54242);
		return mockWebServer;
	}

	@Bean
	@DependsOn("mockedUnleashApiServer")
	public FeatureProvider unleashProvider(UnleashProviderConfig unleashProviderConfig) {
		log.info("UnleashProvider init.");
		return new UnleashProvider(unleashProviderConfig);
	}

}
