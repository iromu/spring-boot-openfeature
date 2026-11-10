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

package org.iromu.openfeature.boot.configdirector;

import com.configdirector.ClientOptions;

/**
 * Callback interface that can be used to customize the {@link ClientOptions} the
 * {@code ConfigDirectorProvider} is built with — connection mode and endpoint, polling
 * interval, timeouts, metadata and telemetry — everything the properties do not cover.
 *
 * @author Ivan Rodriguez
 */
@FunctionalInterface
public interface ConfigDirectorCustomizer {

	/**
	 * Callback to customize the {@link ClientOptions} before the provider is built.
	 * @param options the ClientOptions to customize
	 */
	void customize(ClientOptions options);

}
