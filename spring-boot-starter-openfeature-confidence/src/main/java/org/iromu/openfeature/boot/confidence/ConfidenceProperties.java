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

package org.iromu.openfeature.boot.confidence;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the Confidence feature-flag provider. The API token authenticates
 * against the Confidence service; any other
 * {@link com.spotify.confidence.Confidence.Builder} option is delegated to
 * {@link ConfidenceCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = ConfidenceProperties.CONFIDENCE_PREFIX)
@Data
public class ConfidenceProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String CONFIDENCE_PREFIX = "spring.openfeature.confidence";

	private boolean enabled = true;

	/**
	 * Confidence API token (client secret) used to authenticate against the Confidence
	 * service. Required when the starter is enabled.
	 */
	private String apiToken;

	/**
	 * Optional Confidence flag-resolver endpoint override. When unset the provider's
	 * default endpoint is used.
	 */
	private String host;

	/**
	 * Optional Confidence flag-resolver endpoint port. Defaults to 443 when a custom
	 * {@link #host} is configured.
	 */
	private Integer port;

}
