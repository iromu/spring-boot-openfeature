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

package org.iromu.openfeature.boot.devcycle;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the DevCycle OpenFeature provider. The cloud client connects to
 * the DevCycle bucketing API; any other client option is delegated to
 * {@link DevCycleCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = DevCycleProperties.DEVCYCLE_PREFIX)
@Data
public class DevCycleProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String DEVCYCLE_PREFIX = "spring.openfeature.devcycle";

	private boolean enabled = true;

	/**
	 * DevCycle server-side SDK key for the target project. Must start with
	 * {@code dvc_server} (or {@code server}). Required when the starter is enabled.
	 */
	private String serverSideKey;

	/**
	 * Base URL override for the DevCycle bucketing API (defaults to the production API).
	 */
	private String baseUrlOverride;

}
