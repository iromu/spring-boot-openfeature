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

package org.iromu.openfeature.boot.unleash;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Root class representing the JSON structure for flags and features.
 *
 * @author Ivan Rodriguez
 */
@Data
public class UnleashBackFile {

	private int version;

	private List<Feature> features;

	/**
	 * Represents a single feature with its properties and nested structures.
	 */
	@Data
	public static class Feature {

		private String name;

		private String type;

		private boolean enabled;

		private boolean stale;

		private List<Strategy> strategies;

		private List<Variant> variants;

		/**
		 * Represents a strategy with a name, parameters, and optional constraints.
		 */
		@Data
		public static class Strategy {

			private String name;

			private Map<String, String> parameters; // Parameters as key-value pairs

			private List<Object> constraints; // Constraints (empty list in provided JSON)

		}

		/**
		 * Represents a variant with its properties, payload, and optional overrides.
		 */
		@Data
		public static class Variant {

			private String name;

			private int weight;

			private String weightType;

			private Payload payload;

			private List<Override> overrides;

			private String stickiness;

			/**
			 * Represents the payload of a variant.
			 */
			@Data
			public static class Payload {

				private String type;

				private String value;

			}

			/**
			 * Represents an override with context name and values.
			 */
			@Data
			public static class Override {

				private String contextName;

				private List<String> values;

			}

		}

	}

}
