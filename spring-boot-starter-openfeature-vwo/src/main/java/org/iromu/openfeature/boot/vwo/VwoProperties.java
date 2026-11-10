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

package org.iromu.openfeature.boot.vwo;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring properties for the VWO OpenFeature provider. The account ID and the API key are
 * required; any other option of the options map passed to {@link com.vwo.VWOProvider} is
 * delegated to {@link VwoCustomizer}.
 *
 * @author Ivan Rodriguez
 */
@ConfigurationProperties(prefix = VwoProperties.VWO_PREFIX)
@Data
public class VwoProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String VWO_PREFIX = "spring.openfeature.vwo";

	private boolean enabled = true;

	/**
	 * VWO account ID for the target account. Required when the starter is enabled.
	 */
	private String accountId;

	/**
	 * VWO API key (sdk key) used to initialize the VWO client. Required when the starter
	 * is enabled.
	 */
	private String apiKey;

}
