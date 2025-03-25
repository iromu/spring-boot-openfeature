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

package org.iromu.openfeature.boot.flagd;

import dev.openfeature.contrib.providers.flagd.Config;
import lombok.Data;
import org.iromu.openfeature.boot.autoconfigure.flagd.FlagdAutoConfiguration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Spring properties for {@link FlagdAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = FlagdProperties.FLAGD_PREFIX)
@Data
public class FlagdProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String FLAGD_PREFIX = "spring.openfeature.flagd";

	private boolean enabled = true;

	/**
	 * flagd resolving type.
	 */
	private Config.Resolver resolverType = Config.Resolver.RPC;

	/**
	 * Connection deadline in milliseconds. For RPC resolving, this is the deadline to
	 * connect to flagd for flag evaluation. For in-process resolving, this is the
	 * deadline for sync stream termination.
	 */
	private int deadline = 500;

	/**
	 * File source of flags to be used by offline mode. Setting this enables the offline
	 * mode of the in-process provider.
	 */
	private Resource offlineFlagSourcePath;

}
