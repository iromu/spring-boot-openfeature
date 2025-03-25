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

package org.iromu.openfeature.boot.unleash;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.openfeature.contrib.providers.unleash.UnleashProvider;
import dev.openfeature.contrib.providers.unleash.UnleashProviderConfig;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.exceptions.GeneralError;
import io.getunleash.FakeUnleash;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Provider implementation for FakeUnleash.
 *
 * @author Ivan Rodriguez
 */
@Slf4j
public class FakeUnleashProvider extends UnleashProvider {

	private final AtomicBoolean isInitialized = new AtomicBoolean(false);

	@Getter
	private FakeUnleash unleash;

	/**
	 * Constructor.
	 * @param unleashProviderConfig the UnleashProviderConfig
	 */
	public FakeUnleashProvider(UnleashProviderConfig unleashProviderConfig) {
		super(null);
	}

	/**
	 * Initialize the provider.
	 * @param evaluationContext evaluation context
	 * @throws Exception on error
	 */
	@Override
	public void initialize(EvaluationContext evaluationContext) throws Exception {
		boolean initialized = this.isInitialized.getAndSet(true);
		if (initialized) {
			throw new GeneralError("already initialized");
		}

		this.unleash = new FakeUnleash();
		setUnleash(this.unleash);

		// Unleash is per definition ready after it is initialized.
		log.info("finished initializing fake provider");
	}

	public void toggle(String feature) {

		if (this.unleash.isEnabled(feature)) {
			this.unleash.disable(feature);
		}
		else {
			this.unleash.enable(feature);
		}
		log.debug("{} updated to {}", feature, this.unleash.isEnabled(feature));
	}

}
