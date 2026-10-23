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

package org.iromu.openfeature.boot.autoconfigure.gcp;

import dev.openfeature.contrib.providers.gcp.GcpProviderOptions;
import dev.openfeature.contrib.providers.gcp.GcpSecretManagerProvider;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.ProviderEvaluation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-backend test for {@link GcpSecretManagerProvider}, mirroring upstream's own
 * integration gate.
 * <p>
 * It is <b>not</b> part of the build's contract: design.md D4 keeps the
 * provider-vs-backend conformance upstream's job, so this class exists as a
 * manually-runnable check and is expected to report itself <em>skipped</em> on any host
 * without Google credentials. Only the unit tier may never skip.
 * <p>
 * <b>Manual prerequisites</b> (set {@code GCP_PROJECT_ID} and provide Application Default
 * Credentials, e.g. via {@code gcloud auth application-default login}):
 * <ul>
 * <li>the Secret Manager secret {@code my-boolean-flag} exists in that project with an
 * active {@code latest} version whose payload is the literal {@code true};</li>
 * <li>the credential's principal holds {@code secretmanager.versions.access} on it.</li>
 * </ul>
 * <p>
 * Gating note: upstream uses {@code @DisabledIfEnvironmentVariable(matches = "")}, which
 * leaves the test enabled when the variable is <em>absent</em> rather than empty; that
 * would fail a credential-less CI run, which spec scenario 6.1 forbids. Enabling on a
 * non-blank value only skips on both absent and empty, which is the behaviour the spec
 * actually pins.
 *
 * @author Ivan Rodriguez
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "GCP_PROJECT_ID", matches = ".+")
class GcpSecretManagerProviderIntegrationTest {

	private static final String FLAG_KEY = "my-boolean-flag";

	@Test
	void shouldEvaluateSeededSecretFromSecretManager() throws Exception {
		GcpProviderOptions options = GcpProviderOptions.builder().projectId(System.getenv("GCP_PROJECT_ID")).build();

		GcpSecretManagerProvider provider = new GcpSecretManagerProvider(options);
		try {
			provider.initialize(new MutableContext());

			ProviderEvaluation<Boolean> evaluation = provider.getBooleanEvaluation(FLAG_KEY, false,
					new MutableContext());

			assertThat(evaluation.getValue()).isTrue();
		}
		finally {
			provider.shutdown();
		}
	}

}
