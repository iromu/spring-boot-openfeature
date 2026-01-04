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

package org.iromu.openfeature.boot.growthbook;

import java.util.Map;

import growthbook.sdk.java.repository.FeatureRefreshStrategy;
import growthbook.sdk.java.sandbox.CacheMode;
import lombok.Data;
import org.iromu.openfeature.boot.autoconfigure.growthbook.GrowthBookAutoConfiguration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for {@link GrowthBookAutoConfiguration}.
 *
 * @author Heiko Does
 */
@ConfigurationProperties(prefix = GrowthBookProperties.GROWTHBOOK_PREFIX)
@Data
public class GrowthBookProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String GROWTHBOOK_PREFIX = "spring.openfeature.growthbook";

	/**
	 * Whether to enable the functionality of the SDK. (default: true)
	 */
	private boolean enabled = true;

	/**
	 * (mandatory) Growthbook API host URL. (default: https://cdn.growthbook.io)
	 */
	private String apiHost = "https://cdn.growthbook.io";

	/**
	 * (mandatory) Decryption key to decrypt encrypted features. (default: null)
	 */
	private String clientKey;

	/**
	 * (optional) Decryption key to decrypt encrypted features. (default: null)
	 */
	private String decryptionKey;

	private FeatureRefreshStrategy refreshStrategy = FeatureRefreshStrategy.STALE_WHILE_REVALIDATE;

	/**
	 * Cache mode for the local cache. (default: AUTO)
	 */
	private CacheMode cacheMode = CacheMode.AUTO;

	/**
	 * Directory to store the local cache. (default: java.io.tmpdir)
	 */
	private String cacheDirectory = System.getProperty("java.io.tmpdir");

	/**
	 * Whether the SDK is in QA mode. Not for production use. If true, random assignment
	 * is disabled and only explicitly forced variations are used. (default: false)
	 */
	private boolean isQaMode = false;

	/**
	 * Whether the local cache is disabled. (default: false)
	 */
	private boolean isCacheDisabled;

	/**
	 * Force specific experiments to always assign a specific variation. (used for QA)
	 */
	private Map<String, Integer> globalForcedVariationsMap;

}
