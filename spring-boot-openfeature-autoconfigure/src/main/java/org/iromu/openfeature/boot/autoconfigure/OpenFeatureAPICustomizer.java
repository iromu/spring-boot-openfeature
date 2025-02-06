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

package org.iromu.openfeature.boot.autoconfigure;

import dev.openfeature.sdk.OpenFeatureAPI;

/**
 * Callback interface that can be used to customize {@link OpenFeatureAPI}.
 *
 * @author Ivan Rodriguez
 */
@FunctionalInterface
public interface OpenFeatureAPICustomizer {

	/**
	 * Callback to customize a {@link OpenFeatureAPI} instance.
	 * @param openFeatureAPI to customize
	 */
	void customize(OpenFeatureAPI openFeatureAPI);

}
