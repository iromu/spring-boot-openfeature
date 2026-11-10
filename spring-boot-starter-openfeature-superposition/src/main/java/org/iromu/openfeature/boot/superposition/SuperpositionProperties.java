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

package org.iromu.openfeature.boot.superposition;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the Superposition OpenFeature provider. The API token, the
 * organization ID and the workspace ID are required; the remaining
 * {@link io.juspay.superposition.openfeature.SuperpositionProviderOptions} options are
 * optional and any other option is delegated to {@link SuperpositionCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = SuperpositionProperties.SUPERPOSITION_PREFIX)
@Data
public class SuperpositionProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String SUPERPOSITION_PREFIX = "spring.openfeature.superposition";

	/**
	 * Default Superposition API endpoint.
	 */
	public static final String DEFAULT_ENDPOINT = "https://api.superposition.dev";

	/**
	 * Default configuration refresh timeout in milliseconds.
	 */
	public static final int DEFAULT_REFRESH_TIMEOUT_MILLIS = 10000;

	/**
	 * Default configuration refresh interval in milliseconds.
	 */
	public static final int DEFAULT_REFRESH_INTERVAL_MILLIS = 5000;

	private boolean enabled = true;

	/**
	 * Superposition API token for server-to-server access. Required when the starter is
	 * enabled.
	 */
	private String apiKey;

	/**
	 * Superposition organization ID. Required when the starter is enabled.
	 */
	private String orgId;

	/**
	 * Superposition workspace ID. Required when the starter is enabled.
	 */
	private String workspaceId;

	/**
	 * Superposition API endpoint. Optional; defaults to the public Superposition
	 * endpoint.
	 */
	private String endpoint = DEFAULT_ENDPOINT;

	/**
	 * Configuration refresh timeout in milliseconds. Optional; the default applies when
	 * unset.
	 */
	private Integer refreshTimeoutMillis = DEFAULT_REFRESH_TIMEOUT_MILLIS;

	/**
	 * Configuration refresh interval in milliseconds. Optional; the default applies when
	 * unset.
	 */
	private Integer refreshIntervalMillis = DEFAULT_REFRESH_INTERVAL_MILLIS;

}
