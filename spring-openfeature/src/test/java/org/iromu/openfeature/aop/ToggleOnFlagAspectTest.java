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
 * Unit test for {@link ToggleOnFlagAspect} to validate the behavior of class-level and
 * method-level {@link ToggleOnFlag} annotations, specifically testing fallback methods.
 *
 * @author Ivan Rodriguez
 */
@ExtendWith(MockitoExtension.class)
class ToggleOnFlagAspectTest {

	@Mock
	private Client client;

	@InjectMocks
	private ToggleOnFlagAspect toggleOnFlagAspect; // Inject the aspect with the mocked
													// flags

	private MyService createProxy(MyService target) {
		// Create a proxy for the service with the aspect applied
		AspectJProxyFactory factory = new AspectJProxyFactory(target);
		factory.addAspect(toggleOnFlagAspect);
		return factory.getProxy();
	}

	@Test
	public void testPrimaryMethodInvoked() {
		// Mock the feature flag to return true for the key "enablePrimaryTask"
		Mockito.when(client.getBooleanValue("enablePrimaryTask", false)).thenReturn(true);

		// Create a proxy of MyService with the aspect applied
		MyService proxy = createProxy(new MyService());

		// Call the method and verify the primary method is invoked
		String result = proxy.performTask();
		Assertions.assertEquals("Primary Task Completed", result);
	}

	@Test
	public void testFallbackMethodInvoked() {
		// Mock the feature flag to return false for the key "enablePrimaryTask"
		Mockito.when(client.getBooleanValue("enablePrimaryTask", false)).thenReturn(false);

		// Create a proxy of MyService with the aspect applied
		MyService proxy = createProxy(new MyService());

		// Call the method and verify the fallback method is invoked
		String result = proxy.performTask();
		Assertions.assertEquals("Fallback Task Completed", result);
	}

	@Test
	public void testClassLevelAnnotation() {
		// Mock the feature flag to return true for the key "enablePrimaryTask"
		Mockito.when(client.getBooleanValue("enablePrimaryTask", false)).thenReturn(true);

		// Create a proxy of MyService with the aspect applied
		MyService proxy = createProxy(new MyService());

		// Call the method and verify the primary method is invoked
		String result = proxy.performTask();
		Assertions.assertEquals("Primary Task Completed", result);
	}

	@Test
	public void testMethodLevelAnnotation() {
		// Mock the feature flag to return false for the key "enableFallbackTask"
		Mockito.when(client.getBooleanValue("enableFallbackTask", false)).thenReturn(false);

		// Create a proxy of MyService with the aspect applied
		MyService proxy = createProxy(new MyService());

		// Call the method and verify the fallback method is invoked
		String result = proxy.performAnotherTask();
		Assertions.assertEquals("Alternative Task Completed", result);
	}

	@Test
	public void testClassLevelFallbackMethod() {
		// Mock the feature flag to return false for the key "enablePrimaryTask"
		Mockito.when(client.getBooleanValue("enablePrimaryTask", false)).thenReturn(false);

		// Create a proxy of MyService with the aspect applied
		MyService proxy = createProxy(new MyService());

		// Call the method and verify the fallback method is invoked
		String result = proxy.performTask();
		Assertions.assertEquals("Fallback Task Completed", result);
	}

	@Test
	public void testMethodLevelFallbackMethod() {
		// Mock the feature flag to return false for the key "enableFallbackTask"
		Mockito.when(client.getBooleanValue("enableFallbackTask", false)).thenReturn(false);

		// Create a proxy of MyService with the aspect applied
		MyService proxy = createProxy(new MyService());

		// Call the method and verify the fallback method is invoked
		String result = proxy.performAnotherTask();
		Assertions.assertEquals("Alternative Task Completed", result);
	}

	@ToggleOnFlag(key = "enablePrimaryTask", orElse = "fallbackTask")
	public static class MyService {

		public String performTask() {
			return "Primary Task Completed";
		}

		@ToggleOnFlag(key = "enableFallbackTask", orElse = "alternativeTask")
		public String performAnotherTask() {
			return "Another Primary Task Completed";
		}

		public String fallbackTask() {
			return "Fallback Task Completed";
		}

		public String alternativeTask() {
			return "Alternative Task Completed";
		}

	}

}
