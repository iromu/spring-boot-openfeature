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

package org.iromu.openfeature.examples;

import dev.openfeature.sdk.Client;
import org.iromu.openfeature.boot.aop.ToggleOnFlag;
import org.iromu.openfeature.boot.unleash.FakeUnleashProvider;

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

	private final FakeUnleashProvider fakeUnleashProvider;

	public UnleashController(Client client, FakeUnleashProvider fakeUnleashProvider) {
		this.client = client;
		this.fakeUnleashProvider = fakeUnleashProvider;

		// Init features
		this.fakeUnleashProvider.getUnleash().enable("random-boolean-flag");

	}

	@GetMapping("toggle")
	public Boolean toggle(@RequestParam final String name) {
		this.fakeUnleashProvider.toggle(name);

		return this.client.getBooleanValue(name, false);
	}

	@GetMapping("{name}")
	public Boolean feature(@PathVariable("name") final String name) {
		return this.client.getBooleanValue(name, false);
	}

	@GetMapping("greet/{name}")
	@ToggleOnFlag(key = "random-boolean-flag", orElse = "greetWhenDisabled")
	public String greet(@PathVariable("name") final String name) {
		this.fakeUnleashProvider.toggle("random-boolean-flag");
		return "Hello " + name;
	}

	public String greetWhenDisabled(final String name) {
		this.fakeUnleashProvider.toggle("random-boolean-flag");
		return "HELLO " + name;
	}

}
