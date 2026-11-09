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

package org.iromu.openfeature.boot.featureflip;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the Featureflip feature-flag provider. The target environment is
 * encoded in the per-environment SDK key itself; data-source tuning (streaming, polling,
 * timeouts) is delegated to {@link FeatureflipCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = FeatureflipProperties.FEATUREFLIP_PREFIX)
@Data
public class FeatureflipProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String FEATUREFLIP_PREFIX = "spring.openfeature.featureflip";

	/**
	 * Enable or disable the Featureflip auto-configuration.
	 */
	private boolean enabled = true;

	/**
	 * Featureflip SDK key for the target environment. Required when the starter is
	 * enabled.
	 */
	private String apiKey;

	/**
	 * Optional Featureflip endpoint override (self-hosted deployments). When unset the
	 * provider's default endpoint is used.
	 */
	private String baseUrl;

}
