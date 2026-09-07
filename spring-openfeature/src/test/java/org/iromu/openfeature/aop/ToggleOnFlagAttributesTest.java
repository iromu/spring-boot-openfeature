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
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class ToggleOnFlagAttributesTest {

	@Mock
	private Client client;

	@InjectMocks
	private ToggleOnFlagAspect toggleOnFlagAspect; // Inject the aspect with the mocked

	// flags

	private ToggleOnFlagAttributesTest.MyService createProxy(ToggleOnFlagAttributesTest.MyService target) {
		// Create a proxy for the service with the aspect applied
		AspectJProxyFactory factory = new AspectJProxyFactory(target);
		factory.addAspect(toggleOnFlagAspect);
		return factory.getProxy();
	}

	public static class MyService {

		@ToggleOnFlag(key = "users-flag", attributes = "{'userId': #arg0, 'role': 'admin'}",
				orElse = "featureOnUserIdDisabled")
		public String performTask(final String id) {
			return "Primary Task Completed " + id;
		}

		public String featureOnUserIdDisabled(final String id) {
			return "Fallback Task Completed " + id;
		}

	}

	@Test
	public void testPrimaryMethodInvoked() {
		// Mock the feature flag to return true for the key "enablePrimaryTask"
		Mockito.when(client.getBooleanValue(eq("users-flag"), eq(false), any(EvaluationContext.class)))
			.thenReturn(true);

		// Create a proxy of MyService with the aspect applied
		ToggleOnFlagAttributesTest.MyService proxy = createProxy(new ToggleOnFlagAttributesTest.MyService());

		// Call the method and verify the primary method is invoked
		String result = proxy.performTask("111");
		Assertions.assertEquals("Primary Task Completed 111", result);
	}

	@Test
	public void testFallbackMethodInvoked() {
		// Mock the feature flag to return false for the key "enablePrimaryTask"
		Mockito.when(client.getBooleanValue(eq("users-flag"), eq(false), any(EvaluationContext.class)))
			.thenReturn(false);

		// Create a proxy of MyService with the aspect applied
		ToggleOnFlagAttributesTest.MyService proxy = createProxy(new ToggleOnFlagAttributesTest.MyService());

		// Call the method and verify the fallback method is invoked
		String result = proxy.performTask("999");
		Assertions.assertEquals("Fallback Task Completed 999", result);
	}

}
