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

package org.iromu.openfeature.boot.autoconfigure.security;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.Hook;
import dev.openfeature.sdk.HookContext;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.Value;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.OpenFeatureAPICustomizer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Autoconfiguration supporting {@link SecurityContextHolder}.
 *
 * @author Ivan Rodriguez
 */
@AutoConfiguration
@AutoConfigureBefore({ OpenFeatureAPI.class })
@ConditionalOnClass(SecurityContextHolder.class)
@Slf4j
public class SecurityAutoConfiguration {

	@Bean
	public OpenFeatureAPICustomizer openFeatureAPISecurityCustomizer() {
		return (openFeatureAPI) -> openFeatureAPI.addHooks(new Hook<String>() {
			@Override
			public Optional<EvaluationContext> before(HookContext<String> ctx, Map<String, Object> hints) {
				Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
				if (authentication != null && authentication.getPrincipal() != null) {
					List<Value> authorities = authentication.getAuthorities()
						.stream()
						.map((a) -> new Value(a.getAuthority()))
						.toList();
					EvaluationContext context = new ImmutableContext(Map.of("userId",
							new Value(authentication.getName()), "authorities", new Value(authorities)));
					return Optional.of(context);
				}
				else {
					return Optional.empty();
				}
			}
		});
	}

}
