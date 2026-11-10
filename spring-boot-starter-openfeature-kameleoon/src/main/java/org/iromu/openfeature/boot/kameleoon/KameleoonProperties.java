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

package org.iromu.openfeature.boot.kameleoon;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the Kameleoon OpenFeature provider. The site code and the client
 * credentials are required; the remaining {@link com.kameleoon.KameleoonClientConfig}
 * options are optional and any other option is delegated to {@link KameleoonCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = KameleoonProperties.KAMELEOON_PREFIX)
@Data
public class KameleoonProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String KAMELEOON_PREFIX = "spring.openfeature.kameleoon";

	private boolean enabled = true;

	/**
	 * Kameleoon site code for the target site. Required when the starter is enabled.
	 */
	private String siteCode;

	/**
	 * Kameleoon client ID for server-to-server access. Required when the starter is
	 * enabled.
	 */
	private String clientId;

	/**
	 * Kameleoon client secret for server-to-server access. Required when the starter is
	 * enabled.
	 */
	private String clientSecret;

	/**
	 * Kameleoon environment name (for example, a staging or production environment).
	 * Optional.
	 */
	private String environment;

	/**
	 * Top-level domain for the Kameleoon endpoints, recommended when using subdomains.
	 * Optional.
	 */
	private String topLevelDomain;

	/**
	 * Configuration refresh interval in minutes. Optional; the Kameleoon client default
	 * applies when unset.
	 */
	private Integer refreshInterval;

	/**
	 * Visitor session duration in minutes. Optional; the Kameleoon client default applies
	 * when unset.
	 */
	private Integer sessionDuration;

	/**
	 * Default HTTP timeout in milliseconds. Optional; the Kameleoon client default
	 * applies when unset.
	 */
	private Integer defaultTimeout;

	/**
	 * Conversion tracking interval in milliseconds. Optional; the Kameleoon client
	 * default applies when unset.
	 */
	private Integer trackingInterval;

}
