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

package org.iromu.openfeature.boot.split;

import io.split.client.SplitClientConfig;
import lombok.Value;

/**
 * The assembled configuration for the Split feature provider: the authorization key plus
 * the Split client configuration the provider is built from.
 *
 * @author Ivan Rodriguez
 */
@Value
public class SplitProviderOptions {

	/**
	 * The Split authorization key used to authenticate against the Split service.
	 */
	String authorizationKey;

	/**
	 * The Split client configuration the provider is built from.
	 */
	SplitClientConfig clientConfig;

}
