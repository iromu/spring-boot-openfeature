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

/**
 * Assembled configuration for the Featureflip feature-flag provider, built from
 * {@link FeatureflipProperties} and adjusted by every registered
 * {@link FeatureflipCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@Data
public class FeatureflipProviderConfig {

	/**
	 * Featureflip SDK key for the target environment.
	 */
	private String apiKey;

	/**
	 * Optional Featureflip endpoint override; {@code null} lets the provider use its
	 * default endpoint.
	 */
	private String baseUrl;

}
