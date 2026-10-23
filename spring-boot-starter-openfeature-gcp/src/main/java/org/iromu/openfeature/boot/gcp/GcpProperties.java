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

package org.iromu.openfeature.boot.gcp;

import java.time.Duration;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Spring properties for {@link dev.openfeature.contrib.providers.gcp.GcpProviderOptions}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = GcpProperties.GCP_PREFIX)
@Data
public class GcpProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String GCP_PREFIX = "spring.openfeature.gcp";

	/**
	 * Type value selecting the Secret Manager backed variant.
	 */
	public static final String SECRET_MANAGER_TYPE = "secretmanager";

	/**
	 * Type value selecting the Parameter Manager backed variant.
	 */
	public static final String PARAMETER_MANAGER_TYPE = "parametermanager";

	private boolean enabled = true;

	private String type = SECRET_MANAGER_TYPE;

	private String projectId;

	/**
	 * Service-account JSON to build the Google credentials from. When unset the provider
	 * falls back to Application Default Credentials.
	 */
	private Resource serviceAccountJson;

	private String version;

	private String locationId;

	private Duration cacheExpiry;

	private Integer cacheMaxSize;

	private String namePrefix;

}
