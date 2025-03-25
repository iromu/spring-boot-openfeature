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

package org.iromu.openfeature.boot.gofeatureflag;

import lombok.Data;
import org.iromu.openfeature.boot.autoconfigure.gofeatureflag.GoFeatureFlagAutoConfiguration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for {@link GoFeatureFlagAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = GoFeatureFlagProperties.GOFEATUREFLAG_PREFIX)
@Data
public class GoFeatureFlagProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String GOFEATUREFLAG_PREFIX = "spring.openfeature.gofeatureflag";

	private boolean enabled = true;

	/**
	 * (mandatory) endpoint contains the DNS of your GO Feature Flag relay proxy. example:
	 * https://mydomain.com/gofeatureflagproxy/
	 */
	private String endpoint;

	/**
	 * (optional) If the relay proxy is configured to authenticate the requests, you
	 * should provide an API Key to the provider. Please ask the administrator of the
	 * relay proxy to provide an API Key. (This feature is available only if you are using
	 * GO Feature Flag relay proxy v1.7.0 or above) Default: null
	 */
	private String apiKey;

}
