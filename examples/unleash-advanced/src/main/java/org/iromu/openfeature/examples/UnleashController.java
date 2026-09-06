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

package org.iromu.openfeature.examples;

import java.util.Map;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.Value;
import org.iromu.openfeature.boot.aop.ToggleOnFlag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample Controller.
 *
 * @author Ivan Rodriguez
 */
@RestController
@RequestMapping("/feature")
public class UnleashController {

	private final Client client;

	private final MockedUnleashApiServer mockedUnleashApiServer;

	public UnleashController(Client client, MockedUnleashApiServer mockedUnleashApiServer) {
		this.client = client;
		this.mockedUnleashApiServer = mockedUnleashApiServer;

	}

	@GetMapping("toggle")
	public Boolean toggle(@RequestParam final String name) {
		this.mockedUnleashApiServer.toggle(name);

		return this.client.getBooleanValue(name, false);
	}

	@GetMapping("{name}")
	public Boolean feature(@PathVariable("name") final String name) {
		return this.client.getBooleanValue(name, false);
	}

	@GetMapping("user/{id}")
	public Boolean featureOnUserId(@PathVariable("id") final String id) {
		return this.client.getBooleanValue("users-flag", false, new ImmutableContext(Map.of("userId", new Value(id))));
	}

	@GetMapping("annotated/user/{id}")
	@ToggleOnFlag(key = "users-flag", attributes = "{'userId': #id}", orElse = "featureOnUserIdDisabled")
	public String featureOnUserIdAnnotated(@PathVariable("id") final String id) {
		return "User allowed";
	}

	public String featureOnUserIdDisabled(final String id) {
		return "uset not allowed";
	}

	@GetMapping("greet/{name}")
	@ToggleOnFlag(key = "random-boolean-flag", orElse = "greetWhenDisabled")
	public String greet(@PathVariable("name") final String name) {
		this.mockedUnleashApiServer.toggle("random-boolean-flag");
		return "Hello " + name;
	}

	public String greetWhenDisabled(final String name) {
		this.mockedUnleashApiServer.toggle("random-boolean-flag");
		return "HELLO " + name;
	}

}
