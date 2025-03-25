package org.iromu.openfeature.boot.autoconfigure.envvar;

import dev.openfeature.contrib.providers.envvar.EnvironmentGateway;
import dev.openfeature.contrib.providers.envvar.EnvironmentKeyTransformer;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvVarAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class EnvVarAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, EnvVarAutoConfiguration.class));

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(EnvironmentGateway.class)
			.hasBean("environmentGateway")
			.hasSingleBean(EnvironmentKeyTransformer.class)
			.hasBean("environmentKeyTransformer")
			.hasSingleBean(FeatureProvider.class)
			.hasBean("envVarProvider")
			.hasSingleBean(Client.class)
			.hasBean("client"));
	}

}
