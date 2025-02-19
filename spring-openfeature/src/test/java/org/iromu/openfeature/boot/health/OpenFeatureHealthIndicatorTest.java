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

import dev.openfeature.sdk.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Validates the functionality and behavior of the {@link OpenFeatureHealthIndicator}.
 *
 * @author Ivan Rodriguez
 */
@ExtendWith(MockitoExtension.class)
class OpenFeatureHealthIndicatorTest {

	@Mock
	private OpenFeatureAPI openFeatureAPI;

	@Mock
	private FeatureProvider featureProvider;

	@Mock
	private Client client;

	@InjectMocks
	private OpenFeatureHealthIndicator healthIndicator;

	@Test
	void health_ShouldReturnDown_WhenProviderIsNoOpProvider() {
		when(openFeatureAPI.getProvider()).thenReturn(new NoOpProvider());
		Health health = healthIndicator.health();
		assertEquals(Health.down().build().getStatus(), health.getStatus());
		assertTrue(health.getDetails().containsKey("error"));
	}

	@Test
	void health_ShouldReturnUp_WhenProviderIsReady() {
		when(openFeatureAPI.getProvider()).thenReturn(featureProvider);
		when(openFeatureAPI.getClient()).thenReturn(client);
		when(client.getProviderState()).thenReturn(ProviderState.READY);
		Health health = healthIndicator.health();
		assertEquals(Health.up().build().getStatus(), health.getStatus());
	}

	@Test
	void health_ShouldReturnOutOfService_WhenProviderIsNotReadyOrStale() {
		when(openFeatureAPI.getProvider()).thenReturn(featureProvider);
		when(openFeatureAPI.getClient()).thenReturn(client);
		when(client.getProviderState()).thenReturn(ProviderState.NOT_READY);
		Health health = healthIndicator.health();
		assertEquals(Health.outOfService().build().getStatus(), health.getStatus());
	}

	@Test
	void health_ShouldReturnDown_WhenProviderHasErrorOrFatal() {
		when(openFeatureAPI.getProvider()).thenReturn(featureProvider);
		when(openFeatureAPI.getClient()).thenReturn(client);
		when(client.getProviderState()).thenReturn(ProviderState.ERROR);
		Health health = healthIndicator.health();
		assertEquals(Health.down().build().getStatus(), health.getStatus());
	}

	@Test
	void health_ShouldReturnDown_OnException() {
		when(openFeatureAPI.getProvider()).thenThrow(new RuntimeException("Test exception"));
		Health health = healthIndicator.health();
		assertEquals(Health.down().build().getStatus(), health.getStatus());
		assertTrue(health.getDetails().containsKey("error"));
	}

}
