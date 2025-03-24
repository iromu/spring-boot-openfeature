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

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.exceptions.GeneralError;
import io.getunleash.FakeUnleash;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FakeUnleashProvider}.
 *
 * @author Ivan Rodriguez
 */
class FakeUnleashProviderTest {

	private FakeUnleashProvider fakeUnleashProvider;

	private EvaluationContext evaluationContext;

	@BeforeEach
	void setUp() {
		evaluationContext = mock(EvaluationContext.class);
		fakeUnleashProvider = new FakeUnleashProvider(null);
	}

	// Test 1: Initialization should succeed when called for the first time
	@Test
	void testInitialize_Success() throws Exception {
		fakeUnleashProvider.initialize(evaluationContext);

		assertNotNull(fakeUnleashProvider.getUnleash(), "Unleash should be initialized");
		Assertions.assertTrue(fakeUnleashProvider.getUnleash() instanceof FakeUnleash,
				"Unleash should be an instance of FakeUnleash");
	}

	// Test 2: Initialization should throw GeneralError when called after initialization
	@Test
	void testInitialize_AlreadyInitialized() throws Exception {
		// First initialization call
		fakeUnleashProvider.initialize(evaluationContext);

		// Attempt to initialize again and assert exception is thrown
		GeneralError exception = Assertions.assertThrows(GeneralError.class, () -> {
			fakeUnleashProvider.initialize(evaluationContext);
		});

		assertEquals("already initialized", exception.getMessage(),
				"The exception message should be 'already initialized'");
	}

	// Test 3: Toggle feature that is initially disabled, it should be enabled
	@Test
	void testToggle_EnableFeature() throws Exception {
		fakeUnleashProvider.initialize(evaluationContext);

		String feature = "my-feature";
		fakeUnleashProvider.toggle(feature);

		assertTrue(fakeUnleashProvider.getUnleash().isEnabled(feature), "Feature should be enabled");
	}

	// Test 4: Toggle feature that is initially enabled, it should be disabled
	@Test
	void testToggle_DisableFeature() throws Exception {
		fakeUnleashProvider.initialize(evaluationContext);

		String feature = "my-feature";

		// First, ensure feature is enabled
		fakeUnleashProvider.toggle(feature); // This should enable the feature
		assertTrue(fakeUnleashProvider.getUnleash().isEnabled(feature), "Feature should be enabled");

		// Now, toggle it again to disable
		fakeUnleashProvider.toggle(feature);

		assertFalse(fakeUnleashProvider.getUnleash().isEnabled(feature), "Feature should be disabled");
	}

	// Test 5: Verify toggling behavior with another feature
	@Test
	void testToggle_WithDifferentFeature() throws Exception {
		fakeUnleashProvider.initialize(evaluationContext);

		String feature1 = "feature1";
		String feature2 = "feature2";

		// Toggle both features
		fakeUnleashProvider.toggle(feature1);
		fakeUnleashProvider.toggle(feature2);

		// Verify both features' states
		assertTrue(fakeUnleashProvider.getUnleash().isEnabled(feature1), "Feature1 should be enabled");
		assertTrue(fakeUnleashProvider.getUnleash().isEnabled(feature2), "Feature2 should be enabled");

		// Toggle again to disable both
		fakeUnleashProvider.toggle(feature1);
		fakeUnleashProvider.toggle(feature2);

		// Verify both features are disabled
		assertFalse(fakeUnleashProvider.getUnleash().isEnabled(feature1), "Feature1 should be disabled");
		assertFalse(fakeUnleashProvider.getUnleash().isEnabled(feature2), "Feature2 should be disabled");
	}

	// Test 6: Verify that enabling a feature works multiple times (idempotency)
	@Test
	void testToggle_EnablingMultipleTimes() throws Exception {
		fakeUnleashProvider.initialize(evaluationContext);

		String feature = "my-feature";

		// Initially, feature is disabled, toggle it to enable
		fakeUnleashProvider.toggle(feature);
		assertTrue(fakeUnleashProvider.getUnleash().isEnabled(feature), "Feature should be enabled");

		// Toggle it again, ensuring it remains enabled (idempotent behavior)
		fakeUnleashProvider.toggle(feature);
		assertFalse(fakeUnleashProvider.getUnleash().isEnabled(feature), "Feature should be disabled");
	}

	// Test 7: Verify that disabling a feature works multiple times (idempotency)
	@Test
	void testToggle_DisablingMultipleTimes() throws Exception {
		fakeUnleashProvider.initialize(evaluationContext);

		String feature = "my-feature2";

		// Initially, feature is disabled
		fakeUnleashProvider.toggle(feature); // Enable it
		assertTrue(fakeUnleashProvider.getUnleash().isEnabled(feature), "Feature should be enabled");

		// Now, toggle to disable it
		fakeUnleashProvider.toggle(feature);
		assertFalse(fakeUnleashProvider.getUnleash().isEnabled(feature), "Feature should be disabled");

		// Toggle again to ensure it's still disabled (idempotent behavior)
		fakeUnleashProvider.toggle(feature);
		assertTrue(fakeUnleashProvider.getUnleash().isEnabled(feature), "Feature should be enabled");
	}

}
