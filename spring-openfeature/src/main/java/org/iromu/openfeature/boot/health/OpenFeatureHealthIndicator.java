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

package org.iromu.openfeature.boot.health;

import dev.openfeature.sdk.NoOpProvider;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.ProviderState;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for OpenFeature integration in Spring Boot.
 * <p>
 * This class implements {@link HealthIndicator} to check the health of the OpenFeature
 * provider and expose it via the Actuator health endpoint.
 * </p>
 *
 * @author Ivan Rodriguez
 */
@Component
@ConditionalOnBean(OpenFeatureAPI.class)
@RequiredArgsConstructor
public class OpenFeatureHealthIndicator implements HealthIndicator {

	private static final String PROVIDER = "provider";

	private final OpenFeatureAPI openFeatureAPI;

	/**
	 * Performs a health check on OpenFeature.
	 * @return {@link Health#up()} if OpenFeature is configured correctly and responding,
	 * otherwise {@link Health#down()} with error details.
	 */
	@Override
	public Health health() {
		try {
			var provider = this.openFeatureAPI.getProvider();

			if (provider instanceof NoOpProvider) {
				return Health.down().withDetail("error", "No active OpenFeature provider").build();
			}

			ProviderState providerState = this.openFeatureAPI.getClient().getProviderState();
			Health health = Health.down().withDetail("Unexpected value", providerState).build();

			final String providerName = provider.getClass().getSimpleName();
			switch (providerState) {
				case READY -> health = Health.up().withDetail(PROVIDER, providerName).build();
				case NOT_READY, STALE -> health = Health.outOfService().withDetail(PROVIDER, providerName).build();
				case ERROR, FATAL -> health = Health.down().withDetail(PROVIDER, providerName).build();
			}

			return health;

		}
		catch (Exception ex) {
			return Health.down().withDetail("error", ex.getMessage()).build();
		}
	}

}
