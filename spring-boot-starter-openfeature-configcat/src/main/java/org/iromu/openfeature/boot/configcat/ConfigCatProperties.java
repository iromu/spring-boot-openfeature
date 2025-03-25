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

package org.iromu.openfeature.boot.configcat;

import dev.openfeature.contrib.providers.configcat.ConfigCatProviderConfig;
import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Spring properties for {@link ConfigCatProviderConfig}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = ConfigCatProperties.CONFIGCAT_PREFIX)
@Data
public class ConfigCatProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String CONFIGCAT_PREFIX = "spring.openfeature.config-cat";

	private boolean enabled = true;

	private String sdkKey;

	private Resource filename;

}
