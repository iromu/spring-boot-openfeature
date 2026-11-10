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

package org.iromu.openfeature.boot.configdirector;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the ConfigDirector OpenFeature provider. ConfigDirector is a
 * remote configuration and feature-flag service: the starter needs the server SDK key,
 * and optionally the endpoint the provider connects to. Every other option is delegated
 * to {@link ConfigDirectorCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = ConfigDirectorProperties.CONFIGDIRECTOR_PREFIX)
@Data
public class ConfigDirectorProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String CONFIGDIRECTOR_PREFIX = "spring.openfeature.configdirector";

	private boolean enabled = true;

	/**
	 * Server SDK key for the ConfigDirector environment. Required when the starter is
	 * enabled.
	 */
	private String apiKey;

	/**
	 * Endpoint the provider connects to. Optional — the ConfigDirector default endpoint
	 * applies.
	 */
	private String url;

}
