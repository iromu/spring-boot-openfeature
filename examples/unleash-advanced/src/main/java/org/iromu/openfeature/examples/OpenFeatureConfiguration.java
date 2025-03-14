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

package org.iromu.openfeature.examples;

import dev.openfeature.sdk.*;
import lombok.extern.slf4j.Slf4j;
import org.iromu.openfeature.boot.autoconfigure.OpenFeatureAPICustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Configuration
@Slf4j
public class OpenFeatureConfiguration {

	@Bean
	public OpenFeatureAPICustomizer openFeatureAPICustomizer() {
		return openFeatureAPI -> {
			openFeatureAPI.addHooks(new Hook<String>() {
				@Override
				public Optional<EvaluationContext> before(HookContext<String> ctx, Map<String, Object> hints) {
					log.info("before hook. {}", ctx.getCtx().asMap());
					if (ctx == null) {
						return Optional.empty();
					}

					MutableContext mutableContext = new MutableContext(ctx.getCtx().getTargetingKey(),
							ctx.getCtx().asMap());
					return Optional.of(mutableContext);
				}
			}, new Hook<String>() {
				@Override
				public void finallyAfter(HookContext<String> ctx, FlagEvaluationDetails<String> details,
						Map<String, Object> hints) {
					log.info("finallyAfter hook. {}", ctx.getCtx().asMap());
				}
			});

		};
	}

	@Bean
	public WebFilter requestFilter(OpenFeatureAPI openFeatureAPI) {
		return new WebFilter() {

			@Override
			public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
				String userId = exchange.getRequest().getHeaders().getFirst("user-id"); // Extract
																						// user
																						// ID
																						// from
																						// headers
				String ipAddress = exchange.getRequest().getRemoteAddress() != null
						? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";

				System.out.println("User ID: " + userId);
				System.out.println("IP Address: " + ipAddress);
				openFeatureAPI.setTransactionContextPropagator(new ThreadLocalTransactionContextPropagator());
				openFeatureAPI
					.setTransactionContext(new ImmutableContext(Map.of("remoteAddress", new Value(ipAddress))));

				// Continue with the filter chain
				return chain.filter(exchange);
			}
		};
	}

}
