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

import dev.openfeature.contrib.providers.gofeatureflag.GoFeatureFlagProviderOptions;

/**
 * Callback interface that can be used to customize GoFeatureFlag with a
 * {@link GoFeatureFlagProviderOptions.GoFeatureFlagProviderOptionsBuilder}.
 *
 * @author Ivan Rodriguez
 */
@FunctionalInterface
public interface GoFeatureFlagCustomizer {

	/**
	 * Callback to customize a
	 * {@link GoFeatureFlagProviderOptions.GoFeatureFlagProviderOptionsBuilder} instance.
	 * @param builder an GoFeatureFlagProviderOptions builder to customize
	 */
	void customize(GoFeatureFlagProviderOptions.GoFeatureFlagProviderOptionsBuilder builder);

}
