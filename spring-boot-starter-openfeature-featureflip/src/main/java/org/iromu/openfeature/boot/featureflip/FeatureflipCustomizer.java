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

/**
 * Callback interface that can be used to customize the assembled
 * {@link FeatureflipProviderConfig} before the provider is created.
 *
 * @author Ivan Rodriguez
 */
@FunctionalInterface
public interface FeatureflipCustomizer {

	/**
	 * Callback to customize the Featureflip provider configuration.
	 * @param config the assembled configuration to customize
	 */
	void customize(FeatureflipProviderConfig config);

}
