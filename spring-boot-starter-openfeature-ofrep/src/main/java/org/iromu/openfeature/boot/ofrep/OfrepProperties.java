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

package org.iromu.openfeature.boot.ofrep;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import lombok.Data;
import org.iromu.openfeature.boot.autoconfigure.ofrep.OfrepAutoConfiguration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for {@link OfrepAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = OfrepProperties.OFREP_PREFIX)
@Data
public class OfrepProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String OFREP_PREFIX = "spring.openfeature.ofrep";

	private boolean enabled = true;

	/**
	 * Base URL of the OFREP endpoint.
	 */
	private String baseUrl;

	/**
	 * Timeout for establishing the connection to the OFREP endpoint.
	 */
	private Duration connectTimeout;

	/**
	 * Timeout for a single evaluation request to the OFREP endpoint.
	 */
	private Duration requestTimeout;

	/**
	 * Headers sent with every OFREP request.
	 */
	private Map<String, List<String>> headers;

}
