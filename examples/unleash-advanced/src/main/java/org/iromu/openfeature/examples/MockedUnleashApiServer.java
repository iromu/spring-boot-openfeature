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

package org.iromu.openfeature.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.iromu.openfeature.boot.unleash.UnleashBackFile;
import org.iromu.openfeature.boot.unleash.UnleashProperties;

import org.springframework.stereotype.Component;

/**
 * Simulation for Unleash server.
 *
 * @author Ivan Rodriguez
 */
@Component
@Slf4j
public class MockedUnleashApiServer {

	UnleashBackFile unleashBackFile;

	@SneakyThrows
	public MockedUnleashApiServer(MockWebServer mockWebServer, UnleashProperties properties,
			ObjectMapper objectMapper) {
		this.unleashBackFile = objectMapper.readValue(properties.getBackupFile().getInputStream(),
				UnleashBackFile.class);

		mockWebServer.setDispatcher(new Dispatcher() {
			@SneakyThrows
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				String path = request.getPath();
				log.info("Requested {}", path);
				if ("/api/client/metrics".equals(path)) {
					return new MockResponse().setResponseCode(200);
				}
				else if ("/api/client/register".equals(path)) {
					return new MockResponse().setResponseCode(200);
				}
				else if ("/api/client/features".equals(path)) {
					return new MockResponse().setBody(objectMapper.writeValueAsString(unleashBackFile))
						.addHeader("Content-Type", "application/json")
						.setResponseCode(200);
				}
				else {
					return new MockResponse().setResponseCode(404);
				}
			}
		});

		log.info("MockedUnleashApiServer init.");
	}

	public void toggle(String key) {
		this.unleashBackFile.getFeatures()
			.stream()
			.filter((feature) -> key.equals(feature.getName()))
			.forEach((feature) -> feature.setEnabled(!feature.isEnabled()));
	}

}
