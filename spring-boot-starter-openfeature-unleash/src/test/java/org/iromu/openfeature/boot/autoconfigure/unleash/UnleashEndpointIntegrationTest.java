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

package org.iromu.openfeature.boot.autoconfigure.unleash;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.unleash.UnleashCustomizer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.unleash.UnleashProperties.UNLEASH_PREFIX;

/**
 * Drives the auto-configured Unleash {@link FeatureProvider} against a real Unleash
 * server, authoring flags through the server's admin API and reading them back through
 * the client SDK.
 *
 * This is the endpoint re-verification the {@code upgrade-dependencies-2026-09} gate asks
 * for: the {@link UnleashAutoConfigurationTest} family stubs the HTTP layer with a mock
 * web server, so nothing in this module otherwise proves that the client actually talks
 * to a live server. A flag that was created on the server can only evaluate {@code true}
 * here if the client fetched it, because every evaluation in this test falls back to
 * {@code false}.
 *
 * Bring the endpoint up first (see
 * {@code src/test/resources/unleash/docker-compose.integration.yml}):
 *
 * <pre>{@code
 *   docker compose -f src/test/resources/unleash/docker-compose.integration.yml up -d
 *   JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -pl spring-boot-starter-openfeature-unleash test \
 *       -Dtest=UnleashEndpointIntegrationTest
 * }</pre>
 *
 * The test self-skips with a loud message when no endpoint answers, so a contributor
 * without Docker still gets a green build; it is not {@code @Disabled} and carries no
 * "local only" marker.
 *
 * @author Ivan Rodriguez
 */
@SuppressWarnings({ "NullableProblems", "DataFlowIssue" })
class UnleashEndpointIntegrationTest {

	private static final String BASE_URL = System.getProperty("unleash.it.url", "http://localhost:4243");

	private static final String ADMIN_TOKEN = System.getProperty("unleash.it.adminToken",
			"*:*.integration-admin-token");

	private static final String CLIENT_TOKEN = System.getProperty("unleash.it.token",
			"default:development.unleash-insecure-api-token");

	private static final String PROJECT = "default";

	/**
	 * The environment the seeded backend token is scoped to; flags must be enabled here.
	 */
	private static final String ENVIRONMENT = System.getProperty("unleash.it.environment", "development");

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	private static final String RUN_ID = Long.toHexString(System.nanoTime());

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, UnleashAutoConfiguration.class));

	@Test
	void shouldEvaluateFlagsAuthoredOnARealServer() throws Exception {
		Assumptions.assumeTrue(isEndpointUp(), "No Unleash endpoint at " + BASE_URL + " - start it with "
				+ "src/test/resources/unleash/docker-compose.integration.yml, otherwise this gate is not exercised");

		String enabledFlag = "qwen.it." + RUN_ID + ".enabled";
		String disabledFlag = "qwen.it." + RUN_ID + ".disabled";
		createFeature(enabledFlag, true);
		createFeature(disabledFlag, false);

		this.contextRunner.withUserConfiguration(BlockingFetchConfiguration.class)
			.withPropertyValues(UNLEASH_PREFIX + ".appName=" + RUN_ID,
					UNLEASH_PREFIX + ".unleashAPI=" + BASE_URL + "/api",
					UNLEASH_PREFIX + ".unleashToken=" + CLIENT_TOKEN)
			.run((context) -> {
				assertThat(context).hasSingleBean(FeatureProvider.class).hasBean("unleashProvider");
				Client client = context.getBean(Client.class);

				// A client that never fetched the server-authored flag still fails this
				// assertion, because every evaluation falls back to false.
				boolean seen = false;
				for (int attempt = 0; attempt < 40 && !seen; attempt++) {
					seen = Boolean.TRUE.equals(client.getBooleanValue(enabledFlag, false));
					if (!seen) {
						Thread.sleep(250);
					}
				}
				assertThat(seen)
					.withFailMessage("The real server was told " + enabledFlag
							+ " is enabled but client 12.3.0 never fetched it")
					.isTrue();
				assertThat(Boolean.TRUE.equals(client.getBooleanValue(disabledFlag, false))).isFalse();
				assertThat(Boolean.TRUE.equals(client.getBooleanValue("qwen.it." + RUN_ID + ".absent", false)))
					.isFalse();
			});
	}

	private boolean isEndpointUp() {
		HttpRequest request = HttpRequest.newBuilder(java.net.URI.create(BASE_URL + "/health"))
			.timeout(Duration.ofSeconds(5))
			.GET()
			.build();
		try {
			return HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Creates the flag and then sets its per-environment state explicitly. The create
	 * endpoint ignores the {@code environments} block it is handed, so a flag is only
	 * live once the {@code /on} endpoint has been called for the environment the client
	 * token belongs to; without that the server keeps reporting {@code enabled=false} and
	 * every evaluation falls back, which reads as a client failure but is not one.
	 */
	private void createFeature(String name, boolean enabled) throws Exception {
		String body = "{\"name\":\"" + name + "\",\"description\":\"qwen gate\",\"type\":\"release\",\"enabled\":false,"
				+ "\"project\":\"" + PROJECT + "\"}";
		admin("/api/admin/projects/" + PROJECT + "/features", body, 201,
				"Could not author " + name + " on the real server");
		admin("/api/admin/projects/" + PROJECT + "/features/" + name + "/environments/" + ENVIRONMENT
				+ (enabled ? "/on" : "/off"), "{}", 200, "Could not set " + name + " enabled=" + enabled);
	}

	private void admin(String path, String body, int expected, String what) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(java.net.URI.create(BASE_URL + path))
			.timeout(Duration.ofSeconds(10))
			.header("Content-Type", "application/json")
			.header("Authorization", ADMIN_TOKEN)
			.POST(BodyPublishers.ofString(body))
			.build();
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).withFailMessage(what + ": " + response.statusCode() + " " + response.body())
			.isEqualTo(expected);
	}

	/**
	 * Makes the first fetch blocking so the assertions below see server state rather than
	 * an empty repository. {@link UnleashProperties} declares
	 * {@code synchronousFetchOnInitialisation} but {@link UnleashAutoConfiguration} never
	 * forwards it to the client builder, so the client would otherwise only start its 10s
	 * poller and the context would close before the first tick.
	 */
	@Configuration(proxyBeanMethods = false)
	static class BlockingFetchConfiguration {

		@Bean
		UnleashCustomizer blockingFetchCustomizer() {
			return builder -> builder.synchronousFetchOnInitialisation(true).fetchTogglesInterval(1);
		}

	}

}
