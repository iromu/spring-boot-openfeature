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

package org.iromu.openfeature.boot.launchdarkly;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the LaunchDarkly OpenFeature provider. The environment is encoded
 * in the per-environment service key itself; data-source selection (streaming, polling,
 * file) is delegated to {@link LaunchDarklyCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = LaunchDarklyProperties.LAUNCHDARKLY_PREFIX)
@Data
public class LaunchDarklyProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String LAUNCHDARKLY_PREFIX = "spring.openfeature.launchdarkly";

	private boolean enabled = true;

	/**
	 * LaunchDarkly server-side SDK key for the target environment. Required when the
	 * starter is enabled.
	 */
	private String serviceKey;

	/**
	 * Run the LaunchDarkly client in offline mode: no connection to LaunchDarkly is
	 * attempted. Useful for local development and credential-free test hosts.
	 */
	private boolean offline = false;

}
