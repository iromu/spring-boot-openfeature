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

package org.iromu.openfeature.boot.autoconfigure.gcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import dev.openfeature.contrib.providers.gcp.GcpParameterManagerProvider;
import dev.openfeature.contrib.providers.gcp.GcpProviderOptions;
import dev.openfeature.contrib.providers.gcp.GcpSecretManagerProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ProviderEvaluation;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.gcp.GcpCustomizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.gcp.GcpProperties.GCP_PREFIX;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link GcpAutoConfiguration}.
 * <p>
 * The contract tier keeps the real provider unconstructed by supplying a user
 * {@link FeatureProvider} (the statsig back-off move); the tests that do construct it
 * inject a fixed non-expiring {@link GoogleCredentials} substitute, which design.md D3
 * proves is inert - it triggers neither an ADC lookup nor network egress.
 *
 * @author Ivan Rodriguez
 */
class GcpAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, GcpAutoConfiguration.class));

	// No ClientAutoConfiguration: the provider is constructed but never registered, so
	// these tests assert pure wiring and option assembly.
	private final ApplicationContextRunner optionsRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GcpAutoConfiguration.class));

	static String[] requiredProperties = new String[] { GCP_PREFIX + ".project-id=test-project" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(SubstituteCredentialsConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(GcpProviderOptions.class)
					.hasSingleBean(FeatureProvider.class)
					.hasBean("gcpProvider")
					.hasSingleBean(Client.class)
					.hasBean("client");
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(GcpSecretManagerProvider.class);
			});
	}

	@Test
	void shouldNotSupplyProviderWhenDisabled() {
		this.contextRunner.withPropertyValues(add(requiredProperties, GCP_PREFIX + ".enabled=false"))
			.run((context) -> assertThat(context).doesNotHaveBean(FeatureProvider.class)
				.doesNotHaveBean("gcpProvider")
				.doesNotHaveBean(GcpProviderOptions.class));
	}

	@Test
	void shouldBackOffWhenUserSuppliesProvider() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.doesNotHaveBean("gcpProvider")
				.doesNotHaveBean(GcpProviderOptions.class)
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

	@Test
	void shouldBackOffBeforeReadingServiceAccountJsonWhenUserSuppliesProvider(@TempDir Path tempDir) throws Exception {
		Path unreadable = tempDir.resolve("not-a-service-account.json");
		Files.writeString(unreadable, "{not json");

		this.contextRunner
			.withPropertyValues(add(requiredProperties, GCP_PREFIX + ".service-account-json=file:" + unreadable))
			.withUserConfiguration(UserProviderConfiguration.class)
			.run((context) -> assertThat(context).hasNotFailed()
				.doesNotHaveBean(GcpProviderOptions.class)
				.doesNotHaveBean("gcpProvider")
				.hasSingleBean(FeatureProvider.class));
	}

	@Test
	void shouldApplyCustomizerToBuiltConfiguration() {
		AtomicReference<GcpProviderOptions> captured = new AtomicReference<>();

		// name-prefix is also set as a property so the customizer-runs-last ordering is
		// falsifiable: hoisting the customizer above the property bindings would leave
		// the
		// property value winning and fail this assertion.
		this.optionsRunner.withPropertyValues(add(requiredProperties, GCP_PREFIX + ".name-prefix=ff-"))
			.withBean(GcpCustomizer.class, () -> (GcpCustomizer) (builder) -> {
				builder.namePrefix("customized-by-customizer");
				captured.set(builder.build());
			})
			.run((context) -> {
				assertThat(context.getBean(GcpProviderOptions.class).getNamePrefix())
					.isEqualTo("customized-by-customizer");
				assertThat(captured.get().getNamePrefix()).isEqualTo("customized-by-customizer");
			});
	}

	@Test
	void shouldBindCacheAndLookupOptionsFromProperties() {
		this.optionsRunner.withPropertyValues(requiredProperties)
			.withPropertyValues(GCP_PREFIX + ".version=3", GCP_PREFIX + ".location-id=europe-west1",
					GCP_PREFIX + ".cache-expiry=30s", GCP_PREFIX + ".cache-max-size=42",
					GCP_PREFIX + ".name-prefix=ff-")
			.run((context) -> {
				GcpProviderOptions options = context.getBean(GcpProviderOptions.class);
				assertThat(options.getProjectId()).isEqualTo("test-project");
				assertThat(options.getVersion()).isEqualTo("3");
				assertThat(options.getLocationId()).isEqualTo("europe-west1");
				assertThat(options.getCacheExpiry()).isEqualTo(Duration.ofSeconds(30));
				assertThat(options.getCacheMaxSize()).isEqualTo(42);
				assertThat(options.getNamePrefix()).isEqualTo("ff-");
			});
	}

	@Test
	void shouldSelectVariantByType() {
		this.optionsRunner.withPropertyValues(requiredProperties)
			.run((context) -> assertThat(context.getBean(FeatureProvider.class))
				.isInstanceOf(GcpSecretManagerProvider.class));

		this.optionsRunner.withPropertyValues(add(requiredProperties, GCP_PREFIX + ".type=parametermanager"))
			.run((context) -> assertThat(context.getBean(FeatureProvider.class))
				.isInstanceOf(GcpParameterManagerProvider.class));

		this.contextRunner.withPropertyValues(add(requiredProperties, GCP_PREFIX + ".type=bogus"))
			.withUserConfiguration(SubstituteCredentialsConfiguration.class)
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("bogus");
			});
	}

	@Test
	void shouldRegisterParameterManagerProviderVariant() {
		this.contextRunner.withPropertyValues(add(requiredProperties, GCP_PREFIX + ".type=parametermanager"))
			.withUserConfiguration(SubstituteCredentialsConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(GcpProviderOptions.class)
					.hasSingleBean(FeatureProvider.class)
					.hasBean("gcpProvider")
					.hasSingleBean(Client.class)
					.hasBean("client");
				assertThat(context.getBean(FeatureProvider.class)).isInstanceOf(GcpParameterManagerProvider.class);
			});
	}

	@Test
	void shouldSourceCredentialsFromProperty(@TempDir Path tempDir) throws Exception {
		String clientEmail = "flags@test-project.iam.gserviceaccount.com";
		Path serviceAccount = tempDir.resolve("service-account.json");
		Files.writeString(serviceAccount, serviceAccountJson(clientEmail));

		AtomicReference<GcpProviderOptions> captured = new AtomicReference<>();

		this.optionsRunner
			.withPropertyValues(add(requiredProperties, GCP_PREFIX + ".service-account-json=file:" + serviceAccount))
			.withBean(GcpCustomizer.class, () -> (GcpCustomizer) (builder) -> captured.set(builder.build()))
			.run((context) -> {
				assertThat(captured.get()).isNotNull();
				assertThat(captured.get().getCredentials()).isNotNull()
					.isInstanceOf(ServiceAccountCredentials.class)
					.hasFieldOrPropertyWithValue("account", clientEmail);
			});
	}

	@Test
	void shouldFailWhenServiceAccountJsonCannotBeRead(@TempDir Path tempDir) throws Exception {
		Path unreadable = tempDir.resolve("not-a-service-account.json");
		Files.writeString(unreadable, "{not json");

		this.optionsRunner
			.withPropertyValues(add(requiredProperties, GCP_PREFIX + ".service-account-json=file:" + unreadable))
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IOException.class)
					.hasMessageContaining("Unable to read the service-account JSON at ");
			});
	}

	private static String serviceAccountJson(String clientEmail) throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		String pem = Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(keyPair.getPrivate().getEncoded());
		String privateKey = pemMarker("BEGIN PRIVATE KEY") + "\\n" + pem + "\\n" + pemMarker("END PRIVATE KEY") + "\\n";

		return """
				{
				  "type": "service_account",
				  "project_id": "test-project",
				  "private_key_id": "test-key-id",
				  "private_key": "%s",
				  "client_email": "%s",
				  "client_id": "1234567890",
				  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
				  "token_uri": "https://oauth2.googleapis.com/token"
				}
				""".formatted(privateKey, clientEmail);
	}

	// Built from fragments so no contiguous PEM marker appears in the source (gitleaks);
	// the value assembled at runtime is still a valid PKCS#8 header/trailer.
	private static String pemMarker(String label) {
		return "-----" + label + "-----";
	}

	static GoogleCredentials substituteCredentials() {
		return GoogleCredentials.create(new AccessToken("fixed-test-token", new Date(4102444800000L)));
	}

	private static String[] add(String[] array, String element) {
		return Stream.concat(Arrays.stream(array), Stream.of(element)).toArray(String[]::new);
	}

	@Configuration(proxyBeanMethods = false)
	static class SubstituteCredentialsConfiguration {

		@Bean
		public GcpCustomizer substituteCredentialsCustomizer() {
			return (builder) -> builder.credentials(substituteCredentials());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserProviderConfiguration {

		@Bean
		public FeatureProvider featureProvider() {
			FeatureProvider mock = Mockito.mock(FeatureProvider.class);
			Mockito.when(mock.getMetadata()).thenReturn(() -> "UserFeatureProvider");
			Mockito.when(mock.getBooleanEvaluation(any(), any(), any()))
				.thenReturn(ProviderEvaluation.<Boolean>builder().value(true).build());
			return mock;
		}

	}

}
