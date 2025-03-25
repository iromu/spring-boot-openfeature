package org.iromu.openfeature.boot.autoconfigure.flagd;

import dev.openfeature.contrib.providers.flagd.EnvironmentGateway;
import dev.openfeature.contrib.providers.flagd.EnvironmentKeyTransformer;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FlagdAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class FlagdAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, FlagdAutoConfiguration.class));

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
