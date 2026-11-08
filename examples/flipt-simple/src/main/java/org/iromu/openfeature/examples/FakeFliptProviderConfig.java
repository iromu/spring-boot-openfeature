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

package org.iromu.openfeature.examples;

import dev.openfeature.sdk.FeatureProvider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Registers {@link FakeFliptProvider} in place of the Flipt provider when
 * {@code example.fake-provider=true}, which makes the sample boot with no Flipt
 * endpoint. The Flipt auto-configuration backs off entirely once another
 * {@link FeatureProvider} is present.
 *
 * @author Ivan Rodriguez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "example.fake-provider", havingValue = "true")
public class FakeFliptProviderConfig {

	@Bean
	@Primary
	public FeatureProvider fakeFliptProvider() {
		return new FakeFliptProvider();
	}

}
