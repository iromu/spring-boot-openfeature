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

package org.iromu.openfeature.boot.aop;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.Value;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Aspect to handle the conditional execution of methods or classes annotated with
 * {@link ToggleOnFlag}.
 *
 * <p>
 * This aspect evaluates the condition defined by the feature flag's key. If the condition
 * evaluates to {@code true}, the original method is executed. Otherwise, a fallback
 * method specified by the {@code orElse} attribute is invoked.
 * </p>
 *
 * @author Ivan Rodriguez
 */
@Aspect
@Component
@ConditionalOnBean(Client.class)
@RequiredArgsConstructor
public class ToggleOnFlagAspect {

	private final Client client;

	/**
	 * Around advice to handle conditional execution based on the {@link ToggleOnFlag}
	 * annotation.
	 * @param joinPoint the join point representing the method being executed
	 * @return the result of the primary or fallback method execution
	 * @throws Throwable if any error occurs during method invocation
	 */
	@Around("@annotation(org.iromu.openfeature.boot.aop.ToggleOnFlag) || @within(org.iromu.openfeature.boot.aop.ToggleOnFlag)")
	public Object toggleOnFlagExecution(ProceedingJoinPoint joinPoint) throws Throwable {
		// Retrieve method and class level annotations
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		Object[] args = joinPoint.getArgs();

		ToggleOnFlag methodAnnotation = method.getAnnotation(ToggleOnFlag.class);
		ToggleOnFlag classAnnotation = joinPoint.getTarget().getClass().getAnnotation(ToggleOnFlag.class);

		// Determine which annotation to use
		ToggleOnFlag toggleOnFlag = (methodAnnotation != null) ? methodAnnotation : classAnnotation;

		if (toggleOnFlag != null) {
			String key = toggleOnFlag.key();
			boolean condition;
			// Parse SpEL expression for attributes
			String spelAttributes = toggleOnFlag.attributes();
			if (spelAttributes != null && !"{}".equals(spelAttributes)) {
				Map<String, Value> resolvedAttributes = resolveAttributesSpEL(spelAttributes, method, args);
				condition = evaluateCondition(key, resolvedAttributes);
			}
			else {
				condition = evaluateCondition(key);
			}

			if (condition) {
				// Execute the original method if the condition is true
				return joinPoint.proceed();
			}
			else if (Strings.isNotEmpty(toggleOnFlag.orElse())) {
				// Invoke the fallback method specified in the `orElse` attribute
				String fallbackMethodName = toggleOnFlag.orElse();
				Object targetBean = joinPoint.getTarget();
				Method fallbackMethod = targetBean.getClass().getMethod(fallbackMethodName, method.getParameterTypes());
				return fallbackMethod.invoke(targetBean, joinPoint.getArgs());
			}
		}

		// Proceed normally if no annotation is found
		return joinPoint.proceed();
	}

	/**
	 * Resolves a SpEL expression to generate attributes map.
	 * @param spelExpression the SpEL expression for attributes.
	 * @param method the original method.
	 * @param args the method arguments.
	 * @return the resolved attributes as a Map.
	 */
	@SneakyThrows
	@SuppressWarnings("unchecked")
	private Map<String, Value> resolveAttributesSpEL(String spelExpression, Method method, Object[] args) {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();

		// Add method arguments to the SpEL context using parameter names
		java.lang.reflect.Parameter[] parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			System.out.println("Adding to SpEL context: " + parameters[i].getName() + " = " + args[i]);
			context.setVariable(parameters[i].getName(), args[i]);
		}

		// Parse and evaluate the SpEL expression into a Map
		Object result = parser.parseExpression(spelExpression).getValue(context);

		if (result instanceof Map) {
			Map<String, Value> map = new HashMap<>();
			for (Map.Entry<String, Object> entry : ((Map<String, Object>) result).entrySet()) {
				if (map.put(entry.getKey(), new Value(entry.getValue())) != null) {
					throw new IllegalStateException("Duplicate key");
				}
			}
			return map;
		}
		else {
			throw new IllegalArgumentException("SpEL expression did not resolve to a Map<String, Value>");
		}
	}

	/**
	 * Evaluates the condition based on the provided feature flag key.
	 * @param key the key of the feature flag
	 * @return {@code true} if the feature flag is enabled, {@code false} otherwise
	 */
	private boolean evaluateCondition(String key) {
		return this.client.getBooleanValue(key, false);
	}

	/**
	 * Evaluates the condition based on the provided feature flag key.
	 * @param key the key of the feature flag
	 * @param attributes evaluation context attributes
	 * @return {@code true} if the feature flag is enabled, {@code false} otherwise
	 */
	private boolean evaluateCondition(String key, Map<String, Value> attributes) {
		return this.client.getBooleanValue(key, false, new ImmutableContext(attributes));
	}

}
