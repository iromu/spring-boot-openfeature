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

package org.iromu.openfeature.boot.mixpanel;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the Mixpanel OpenFeature provider. The project is identified by
 * the project token; any other
 * {@link com.mixpanel.mixpanelapi.featureflags.config.RemoteFlagsConfig} option is
 * delegated to {@link MixpanelCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = MixpanelProperties.MIXPANEL_PREFIX)
@Data
public class MixpanelProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String MIXPANEL_PREFIX = "spring.openfeature.mixpanel";

	private boolean enabled = true;

	/**
	 * Mixpanel project token for the target project. Required when the starter is
	 * enabled.
	 */
	private String projectToken;

	/**
	 * Mixpanel API host for the target region (e.g. {@code api.mixpanel.com} or
	 * {@code api-eu.mixpanel.com}). Optional — the Mixpanel SDK default applies.
	 */
	private String apiHost;

	/**
	 * HTTP request timeout in seconds for flag evaluations. Optional — the Mixpanel SDK
	 * default applies.
	 */
	private Integer requestTimeoutSeconds;

}
