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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.Metadata;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.Value;

/**
 * In-memory {@link FeatureProvider} so the sample can boot without an OFREP endpoint.
 *
 * @author Ivan Rodriguez
 */
public class FakeOfrepProvider implements FeatureProvider {

	private final Map<String, Boolean> flags = new ConcurrentHashMap<>();

	@Override
	public Metadata getMetadata() {
		return () -> "ofrep-fake";
	}

	@Override
	public void initialize(EvaluationContext evaluationContext) {
		this.flags.put("random-boolean-flag", true);
	}

	@Override
	public ProviderEvaluation<Boolean> getBooleanEvaluation(String key, Boolean defaultValue,
			EvaluationContext evaluationContext) {
		return evaluation(this.flags.getOrDefault(key, defaultValue));
	}

	@Override
	public ProviderEvaluation<String> getStringEvaluation(String key, String defaultValue,
			EvaluationContext evaluationContext) {
		return evaluation(defaultValue);
	}

	@Override
	public ProviderEvaluation<Integer> getIntegerEvaluation(String key, Integer defaultValue,
			EvaluationContext evaluationContext) {
		return evaluation(defaultValue);
	}

	@Override
	public ProviderEvaluation<Double> getDoubleEvaluation(String key, Double defaultValue,
			EvaluationContext evaluationContext) {
		return evaluation(defaultValue);
	}

	@Override
	public ProviderEvaluation<Value> getObjectEvaluation(String key, Value defaultValue,
			EvaluationContext evaluationContext) {
		return evaluation(defaultValue);
	}

	@Override
	public void shutdown() {
		this.flags.clear();
	}

	/**
	 * Flips the given flag, disabling it when enabled and enabling it when absent or
	 * disabled.
	 * @param key the flag key to flip
	 */
	public void toggle(String key) {
		this.flags.merge(key, true, (current, ignored) -> !current);
	}

	private static <T> ProviderEvaluation<T> evaluation(T value) {
		return ProviderEvaluation.<T>builder().value(value).build();
	}

}
