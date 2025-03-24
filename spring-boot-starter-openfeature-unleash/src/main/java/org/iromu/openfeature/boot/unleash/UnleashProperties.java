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

package org.iromu.openfeature.boot.unleash;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import io.getunleash.CustomHttpHeadersProvider;
import io.getunleash.util.UnleashURLs;
import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Spring properties for {@link io.getunleash.util.UnleashConfig}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = UnleashProperties.UNLEASH_PREFIX)
@Data
public class UnleashProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String UNLEASH_PREFIX = "spring.openfeature.unleash";

	private boolean enabled = true;

	private final URI unleashAPI;

	private final String unleashToken;

	private final UnleashURLs unleashURLs;

	private final Map<String, String> customHttpHeaders;

	private final CustomHttpHeadersProvider customHttpHeadersProvider;

	private final String appName;

	private String environment = "default";

	private final String instanceId;

	private final String sdkVersion;

	private final Resource backupFile;

	private final String clientSpecificationVersion;

	private final String projectName;

	private final String namePrefix;

	private final long fetchTogglesInterval = 10;

	private final Duration fetchTogglesConnectTimeout = Duration.ofSeconds(10);

	private final Duration fetchTogglesReadTimeout = Duration.ofSeconds(10);

	private final boolean disablePolling;

	private final long sendMetricsInterval = 60;

	private final Duration sendMetricsConnectTimeout = Duration.ofSeconds(10);

	private final Duration sendMetricsReadTimeout = Duration.ofSeconds(10);

	private final boolean disableMetrics;

	private final boolean isProxyAuthenticationByJvmProperties;

	private final boolean synchronousFetchOnInitialisation;

}
