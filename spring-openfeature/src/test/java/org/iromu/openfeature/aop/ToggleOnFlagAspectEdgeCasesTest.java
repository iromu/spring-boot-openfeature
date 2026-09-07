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

package org.iromu.openfeature.aop;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import dev.openfeature.sdk.Client;
import org.iromu.openfeature.boot.aop.ToggleOnFlag;
import org.iromu.openfeature.boot.aop.ToggleOnFlagAspect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

/**
 * Edge-case unit tests for {@link ToggleOnFlagAspect} covering the decision paths that
 * the happy-path tests do not reach: condition false without a declared fallback,
 * condition false with a declared fallback, and {@code attributes} SpEL expressions that
 * fail validation (non-map result and duplicate-key result).
 *
 * @author Ivan Rodriguez
 */
@ExtendWith(MockitoExtension.class)
class ToggleOnFlagAspectEdgeCasesTest {

	@Mock
	private Client client;

	@InjectMocks
	private ToggleOnFlagAspect toggleOnFlagAspect;

	private MyService createProxy(MyService target) {
		AspectJProxyFactory factory = new AspectJProxyFactory(target);
		factory.addAspect(toggleOnFlagAspect);
		return factory.getProxy();
	}

	@Test
	void proceedsWhenFlagFalseAndNoFallbackDeclared() {
		Mockito.when(client.getBooleanValue("edge-no-fallback", false)).thenReturn(false);

		MyService proxy = createProxy(new MyService());

		Assertions.assertEquals("Primary Task Completed", proxy.runWithoutFallback());
	}

	@Test
	void invokesFallbackWhenFlagFalseAndOrElseDeclared() {
		Mockito.when(client.getBooleanValue("edge-fallback", false)).thenReturn(false);

		MyService proxy = createProxy(new MyService());

		Assertions.assertEquals("Fallback Task Completed", proxy.runWithFallback());
	}

	@Test
	void raisesWhenAttributesSpelResolvesToNonMap() {
		MyService proxy = createProxy(new MyService());

		Throwable caught = null;
		try {
			proxy.runWithNonMapAttributes("not-a-map");
		}
		catch (Throwable throwable) {
			caught = throwable;
		}

		Assertions.assertTrue(caught != null);
		Throwable root = (caught.getCause() != null) ? caught.getCause() : caught;
		Assertions.assertTrue(root instanceof IllegalArgumentException);
		Assertions.assertTrue(root.getMessage().contains("did not resolve to a Map"));
	}

	@Test
	void raisesWhenAttributesSpelResolvesToDuplicateKeyMap() {
		MyService proxy = createProxy(new MyService());

		Throwable caught = null;
		try {
			proxy.runWithDuplicateKeyAttributes(duplicateKeyMap());
		}
		catch (Throwable throwable) {
			caught = throwable;
		}

		Assertions.assertTrue(caught != null);
		Throwable root = (caught.getCause() != null) ? caught.getCause() : caught;
		Assertions.assertTrue(root instanceof IllegalStateException);
		Assertions.assertTrue(root.getMessage().contains("Duplicate key"));
	}

	/**
	 * Builds a {@link Map} whose {@code entrySet()} yields two entries that share the
	 * same key, forcing the aspect's duplicate-key guard to fire. A conforming
	 * {@code Map} cannot hold duplicate keys, so this fixture intentionally violates the
	 * contract to exercise the defensive branch.
	 * @return a map exposing duplicate keys through its entry set
	 */
	private static Map<String, Object> duplicateKeyMap() {
		return new AbstractMap<String, Object>() {
			@Override
			public Set<Entry<String, Object>> entrySet() {
				Set<Entry<String, Object>> entries = new LinkedHashSet<>();
				entries.add(Map.entry("same-key", "first"));
				entries.add(Map.entry("same-key", "second"));
				return entries;
			}
		};
	}

	public static class MyService {

		@ToggleOnFlag(key = "edge-no-fallback")
		public String runWithoutFallback() {
			return "Primary Task Completed";
		}

		@ToggleOnFlag(key = "edge-fallback", orElse = "fallbackTask")
		public String runWithFallback() {
			return "Primary Task Completed";
		}

		public String fallbackTask() {
			return "Fallback Task Completed";
		}

		@ToggleOnFlag(key = "edge-attributes", attributes = "#arg0")
		public String runWithNonMapAttributes(String value) {
			return "Primary Task Completed";
		}

		@ToggleOnFlag(key = "edge-attributes", attributes = "#arg0")
		public String runWithDuplicateKeyAttributes(Map<String, Object> value) {
			return "Primary Task Completed";
		}

	}

}
