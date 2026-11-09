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

package org.iromu.openfeature.boot.datadog;

import java.time.Duration;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the Datadog feature-flag provider.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = DatadogProperties.DATADOG_PREFIX)
@Data
public class DatadogProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String DATADOG_PREFIX = "spring.openfeature.datadog";

	/**
	 * Enable or disable the Datadog auto-configuration.
	 */
	private boolean enabled = true;

	/**
	 * Datadog API key (mapped to the tracer's {@code DD_API_KEY} setting).
	 */
	private String apiKey;

	/**
	 * Datadog application key.
	 */
	private String appKey;

	/**
	 * Datadog site (e.g. {@code datadoghq.com}, {@code datadoghq.eu}); mapped to the
	 * tracer's {@code DD_SITE} setting.
	 */
	private String site;

	/**
	 * Environment reported to Datadog (e.g. {@code production}); mapped to the tracer's
	 * {@code DD_ENV} setting.
	 */
	private String environment;

	/**
	 * Timeout for the provider to receive its initial configuration.
	 */
	private Duration initTimeout = Duration.ofSeconds(30);

	/**
	 * Expose the evaluation telemetry opt-out parameter on flag evaluations.
	 */
	private boolean evaluationTelemetryOptOut;

}
