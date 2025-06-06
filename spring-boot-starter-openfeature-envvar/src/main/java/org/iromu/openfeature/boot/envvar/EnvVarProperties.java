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

package org.iromu.openfeature.boot.envvar;

import lombok.Data;
import org.iromu.openfeature.boot.autoconfigure.envvar.EnvVarAutoConfiguration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for {@link EnvVarAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = EnvVarProperties.ENVVAR_PREFIX)
@Data
public class EnvVarProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String ENVVAR_PREFIX = "spring.openfeature.env-var";

	private boolean enabled = true;

}
