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

import dev.openfeature.sdk.*;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.autoconfigure.OpenFeatureAPIAutoConfiguration;
import org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link SecurityAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class SecurityAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OpenFeatureAPIAutoConfiguration.class, SecurityAutoConfiguration.class,
				ClientAutoConfiguration.class))
		.withUserConfiguration(MockConfig.class);

	static ArgumentCaptor<EvaluationContext> argumentCaptor = ArgumentCaptor.forClass(EvaluationContext.class);

	@Test
	void testWithManualSecurityContext() {

		this.contextRunner.run((context) -> {
			assertThat(context).hasBean("openFeatureAPISecurityCustomizer")
				.hasSingleBean(FeatureProvider.class)
				.hasSingleBean(Client.class);

			Client client = context.getBean(Client.class);

			assertTrue(client.getBooleanValue("foo", false));
			assertThat(argumentCaptor.getValue().asMap()).isEqualTo(Map.of());

			List<GrantedAuthority> roleUser = AuthorityUtils.createAuthorityList("ROLE_USER");

			SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("testUser", "password", roleUser));
			assertTrue(client.getBooleanValue("foo", false));
			Value role_user = new Value(List.of(new Value("ROLE_USER")));
			assertThat(argumentCaptor.getValue().asMap())
				.isEqualTo(Map.of("userId", new Value("testUser"), "authorities", role_user));

			SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("testUser2", "password", roleUser));
			assertTrue(client.getBooleanValue("foo", false));
			assertThat(argumentCaptor.getValue().asMap())
				.isEqualTo(Map.of("userId", new Value("testUser2"), "authorities", role_user));
			;

			SecurityContextHolder.getContext()
				.setAuthentication(new WebAuthnAuthentication(
						ImmutablePublicKeyCredentialUserEntity.builder().name("testUser3").build(), roleUser));
			assertTrue(client.getBooleanValue("foo", false));
			assertThat(argumentCaptor.getValue().asMap())
				.isEqualTo(Map.of("userId", new Value("testUser3"), "authorities", role_user));
		});

	}

	@Configuration
	static class MockConfig {

		@Bean
		public FeatureProvider featureProvider() {
			FeatureProvider mock = Mockito.mock(FeatureProvider.class);

			Mockito.when(mock.getMetadata()).thenReturn(() -> "MockedFeatureProvider");
			Mockito.when(mock.getBooleanEvaluation(any(), any(), argumentCaptor.capture()))
				.thenReturn(ProviderEvaluation.<Boolean>builder().value(true).build());
			return mock;
		}

	}

}
