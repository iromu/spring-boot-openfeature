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

package org.iromu.openfeature.boot.optimizely;

import lombok.Data;
import org.iromu.openfeature.boot.autoconfigure.optimizely.OptimizelyAutoConfiguration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for {@link OptimizelyAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = OptimizelyProperties.OPTIMIZELY_PREFIX)
@Data
public class OptimizelyProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String OPTIMIZELY_PREFIX = "spring.openfeature.optimizely";

	private boolean enabled = true;

	private String datafile;

	private String sdkKey;

	private String datafileAuthToken;

	private String configUrl;

	private Long pollInterval;

}
