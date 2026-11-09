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

package org.iromu.openfeature.boot.featbit;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the FeatBit OpenFeature provider. The environment is identified
 * by the app token; any other {@link co.featbit.server.FBConfig} option is delegated to
 * {@link FeatBitCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = FeatBitProperties.FEATBIT_PREFIX)
@Data
public class FeatBitProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String FEATBIT_PREFIX = "spring.openfeature.featbit";

	private boolean enabled = true;

	/**
	 * FeatBit app token (environment secret) for the target environment. Required when
	 * the starter is enabled.
	 */
	private String token;

	/**
	 * Run the FeatBit client in offline mode: no connection to FeatBit is attempted.
	 * Useful for local development and credential-free test hosts.
	 */
	private boolean offline = false;

	/**
	 * Disable FeatBit event delivery (analytics tracking) entirely.
	 */
	private boolean disableEvents = false;

	/**
	 * FeatBit streaming (WebSocket) endpoint for the target environment. Required when
	 * the starter is enabled — each FeatBit environment has its own endpoint, obtained
	 * from the FeatBit dashboard.
	 */
	private String streamingUrl;

	/**
	 * FeatBit event (analytics) endpoint for the target environment. Required when the
	 * starter is enabled — each FeatBit environment has its own endpoint, obtained from
	 * the FeatBit dashboard.
	 */
	private String eventUrl;

}
