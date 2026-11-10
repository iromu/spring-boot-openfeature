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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the Ageris OpenFeature provider. Ageris is a file-based platform:
 * flags and configs are read from configuration files in a local directory, so the
 * starter needs an application name, the directory path and the feature-flag file names.
 * Every other option is delegated to {@link AgerisCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = AgerisProperties.AGERIS_PREFIX)
@Data
public class AgerisProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String AGERIS_PREFIX = "spring.openfeature.ageris";

	private boolean enabled = true;

	/**
	 * Name of the application, as the configuration file fetcher and the Ageris managers
	 * use it. Required when the starter is enabled.
	 */
	private String application;

	/**
	 * Directory that holds the Ageris configuration files. Required when the starter is
	 * enabled.
	 */
	private String path;

	/**
	 * Names of the feature-flag configuration files, such as {@code featureflags.json}.
	 * At least one entry is required when the starter is enabled; boolean flag lookups
	 * are answered from these files.
	 */
	private List<String> featureFlagFiles = new ArrayList<>();

	/**
	 * Names of the JSON configuration files, such as {@code configs.json}. Optional —
	 * text, number and object lookups are answered from these files when present.
	 */
	private List<String> jsonConfigFiles = new ArrayList<>();

	/**
	 * Context type that the targeting key of an OpenFeature caller is put under. Optional
	 * — the Ageris provider default ({@code user}) applies.
	 */
	private String targetingKeyType = "user";

	/**
	 * Interval between two refreshes of the configuration files. Optional — the Ageris
	 * starter default ({@code 10s}) applies.
	 */
	private Duration refreshInterval = Duration.ofSeconds(10);

	/**
	 * How long provider initialization waits for the first attempt to load the
	 * configuration files. Optional — the Ageris provider default ({@code 30s}) applies.
	 */
	private Duration firstLoadTimeout = Duration.ofSeconds(30);

}
