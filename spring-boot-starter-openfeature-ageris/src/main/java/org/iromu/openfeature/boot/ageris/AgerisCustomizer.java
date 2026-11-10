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

package org.iromu.openfeature.boot.ageris;

import io.github.lichtsinn.ageris.core.Ageris;

/**
 * Callback interface that can be used to customize the {@link Ageris} instance the
 * provider is built from — binding policy values, metrics, and the other options the
 * properties do not cover. Customizers run before the managers are built, as Ageris
 * requires every binding to be in place before the loader starts.
 *
 * @author Ivan Rodriguez
 */
@FunctionalInterface
public interface AgerisCustomizer {

	/**
	 * Callback to customize the {@link Ageris} instance before its managers are built.
	 * @param ageris the Ageris instance to customize
	 */
	void customize(Ageris ageris);

}
