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

package org.iromu.openfeature.boot.octopusdeploy;

import java.time.Duration;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the Octopus Deploy OpenFeature provider.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = OctopusDeployProperties.OCTOPUSDEPLOY_PREFIX)
@Data
public class OctopusDeployProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String OCTOPUSDEPLOY_PREFIX = "spring.openfeature.octopusdeploy";

	private boolean enabled = true;

	/**
	 * Octopus Deploy client identifier (API key) for the target environment. Required
	 * when the starter is enabled.
	 */
	private String clientIdentifier;

	/**
	 * Product name identifying the calling application in the Octopus Deploy
	 * X-Octopus-Client header. Required when the starter is enabled.
	 */
	private String productName;

	/**
	 * Product version identifying the calling application build. Optional.
	 */
	private String productVersion;

	/**
	 * How often the provider re-checks for flag changes. Default is 1 minute.
	 */
	private Duration cacheDuration = Duration.ofMinutes(1);

}
