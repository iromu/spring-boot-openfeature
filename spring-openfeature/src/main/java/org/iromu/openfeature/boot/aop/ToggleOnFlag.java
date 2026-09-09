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

package org.iromu.openfeature.boot.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to conditionally toggle the execution of a method or all methods within a
 * class based on the value of a feature flag.
 *
 * <p>
 * If the condition evaluates to {@code true}, the original method will be executed. If
 * the condition evaluates to {@code false}, a fallback method specified by the
 * {@code orElse} attribute will be invoked.
 * </p>
 *
 * @author Ivan Rodriguez
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Documented
public @interface ToggleOnFlag {

	/**
	 * The key to evaluate the condition from the feature flags.
	 * @return the key for the feature flag
	 */
	String key();

	/**
	 * A map of attributes to be dynamically populated. Supports SpEL expressions to
	 * populate key-value pairs. e.g.: "{'userId': #arg0, 'role': 'admin'}"
	 * @return spel expression
	 */
	String attributes() default "{}";

	/**
	 * The name of the fallback method to be executed if the condition evaluates to
	 * {@code false}.
	 * @return the name of the fallback method
	 */
	String orElse() default "";

}
