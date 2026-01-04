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

package org.iromu.openfeature.boot.autoconfigure.growthbook;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.growthbook.GrowthBookProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GrowthBookAutoConfigurationTest {

	static String[] requiredProperties;

	private static MockWebServer mockWebServer;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, GrowthBookAutoConfiguration.class));

	@BeforeAll
	static void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		requiredProperties = new String[] {
				GrowthBookProperties.GROWTHBOOK_PREFIX + ".apiHost=" + mockWebServer.url(""),
				GrowthBookProperties.GROWTHBOOK_PREFIX + ".clientKey=some-client" };
	}

	@Test
	void shouldSupplyDefaultBeans() {
		mockWebServer.enqueue(new MockResponse().setBody("""
				{
				  "features": [
				    {
				      "id": "string",
				      "dateCreated": "2019-08-24T14:15:22Z",
				      "dateUpdated": "2019-08-24T14:15:22Z",
				      "archived": true,
				      "description": "string",
				      "owner": "string",
				      "project": "string",
				      "valueType": "boolean",
				      "defaultValue": "string",
				      "tags": [
				        "string"
				      ],
				      "environments": {
				        "property1": {
				          "enabled": true,
				          "defaultValue": "string",
				          "rules": [
				            {
				              "description": "string",
				              "condition": "string",
				              "savedGroupTargeting": [
				                {
				                  "matchType": "all",
				                  "savedGroups": [
				                    null
				                  ]
				                }
				              ],
				              "prerequisites": [
				                {
				                  "id": "string",
				                  "condition": "string"
				                }
				              ],
				              "scheduleRules": [
				                {
				                  "enabled": true,
				                  "timestamp": null
				                },
				                {
				                  "enabled": false,
				                  "timestamp": "2025-06-23T16:09:37.769Z"
				                }
				              ],
				              "id": "string",
				              "enabled": true,
				              "type": "force",
				              "value": "string"
				            }
				          ],
				          "definition": "string",
				          "draft": {
				            "enabled": true,
				            "defaultValue": "string",
				            "rules": [
				              {
				                "description": "string",
				                "condition": "string",
				                "savedGroupTargeting": [
				                  {
				                    "savedGroups": []
				                  }
				                ],
				                "prerequisites": [
				                  {}
				                ],
				                "scheduleRules": [
				                  {
				                    "enabled": true,
				                    "timestamp": null
				                  },
				                  {
				                    "enabled": false,
				                    "timestamp": "2025-06-23T16:09:37.769Z"
				                  }
				                ],
				                "id": "string",
				                "enabled": true,
				                "type": "force",
				                "value": "string"
				              }
				            ],
				            "definition": "string"
				          }
				        },
				        "property2": {
				          "enabled": true,
				          "defaultValue": "string",
				          "rules": [
				            {
				              "description": "string",
				              "condition": "string",
				              "savedGroupTargeting": [
				                {
				                  "matchType": "all",
				                  "savedGroups": [
				                    null
				                  ]
				                }
				              ],
				              "prerequisites": [
				                {
				                  "id": "string",
				                  "condition": "string"
				                }
				              ],
				              "scheduleRules": [
				                {
				                  "enabled": true,
				                  "timestamp": null
				                },
				                {
				                  "enabled": false,
				                  "timestamp": "2025-06-23T16:09:37.769Z"
				                }
				              ],
				              "id": "string",
				              "enabled": true,
				              "type": "force",
				              "value": "string"
				            }
				          ],
				          "definition": "string",
				          "draft": {
				            "enabled": true,
				            "defaultValue": "string",
				            "rules": [
				              {
				                "description": "string",
				                "condition": "string",
				                "savedGroupTargeting": [
				                  {
				                    "savedGroups": []
				                  }
				                ],
				                "prerequisites": [
				                  {}
				                ],
				                "scheduleRules": [
				                  {
				                    "enabled": true,
				                    "timestamp": null
				                  },
				                  {
				                    "enabled": false,
				                    "timestamp": "2025-06-23T16:09:37.769Z"
				                  }
				                ],
				                "id": "string",
				                "enabled": true,
				                "type": "force",
				                "value": "string"
				              }
				            ],
				            "definition": "string"
				          }
				        }
				      },
				      "prerequisites": [
				        "string"
				      ],
				      "revision": {
				        "version": 0,
				        "comment": "string",
				        "date": "2019-08-24T14:15:22Z",
				        "publishedBy": "string"
				      },
				      "customFields": {}
				    }
				  ],
				  "limit": 0,
				  "offset": 0,
				  "count": 0,
				  "total": 0,
				  "hasMore": true,
				  "nextOffset": 0
				}
				"""));
		this.contextRunner.withPropertyValues(requiredProperties)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.hasBean("growthBookProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

}
