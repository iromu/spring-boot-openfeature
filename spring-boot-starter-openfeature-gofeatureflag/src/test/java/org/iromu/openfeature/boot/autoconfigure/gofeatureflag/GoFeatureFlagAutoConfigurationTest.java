package org.iromu.openfeature.boot.autoconfigure.gofeatureflag;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import okhttp3.mockwebserver.MockWebServer;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.gofeatureflag.GoFeatureFlagProperties.GOFEATUREFLAG_PREFIX;

/**
 * Tests for {@link GoFeatureFlagAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class GoFeatureFlagAutoConfigurationTest {

	private static MockWebServer mockWebServer;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, GoFeatureFlagAutoConfiguration.class));

	static String[] requiredProperties;

	@BeforeAll
	public static void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		requiredProperties = new String[] { GOFEATUREFLAG_PREFIX + ".endpoint=" + mockWebServer.url("/api") };
	}

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.hasBean("goFeatureFlagProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

}
