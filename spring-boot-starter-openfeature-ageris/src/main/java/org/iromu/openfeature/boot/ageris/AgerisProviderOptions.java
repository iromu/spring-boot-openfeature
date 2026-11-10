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

import io.github.lichtsinn.ageris.core.ConfigurationLoader;
import io.github.lichtsinn.ageris.core.FeatureFlagManager;
import io.github.lichtsinn.ageris.core.JsonConfigManager;
import lombok.Value;

/**
 * The assembled configuration for the Ageris feature provider: the feature-flag and
 * JSON-config managers, the not-yet-started configuration loader the provider will start,
 * and the provider builder settings.
 *
 * @author Ivan Rodriguez
 */
@Value
public class AgerisProviderOptions {

	/**
	 * The feature-flag manager that answers boolean lookups.
	 */
	FeatureFlagManager featureFlagManager;

	/**
	 * The JSON-config manager that answers text, number and object lookups.
	 */
	JsonConfigManager jsonConfigManager;

	/**
	 * The configuration loader, not started yet: the provider starts and stops it with
	 * OpenFeature.
	 */
	ConfigurationLoader configurationLoader;

	/**
	 * Context type the targeting key of a caller is put under.
	 */
	String targetingKeyType;

	/**
	 * How long provider initialization waits for the first attempt to load the
	 * configuration files.
	 */
	Duration firstLoadTimeout;

}
