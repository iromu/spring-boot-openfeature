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

package org.iromu.openfeature.boot.flagsmith;

import dev.openfeature.contrib.providers.flagsmith.FlagsmithProviderOptions;

/**
 * Callback interface that can be used to customize Flagsmith with a
 * {@link FlagsmithProviderOptions.FlagsmithProviderOptionsBuilder}.
 *
 * @author Ivan Rodriguez
 */
@FunctionalInterface
public interface FlagsmithCustomizer {

	/**
	 * Callback to customize a
	 * {@link FlagsmithProviderOptions.FlagsmithProviderOptionsBuilder} instance.
	 * @param builder an FlagsmithProviderOptions builder to customize
	 */
	void customize(FlagsmithProviderOptions.FlagsmithProviderOptionsBuilder builder);

}
