package org.iromu.openfeature.boot.autoconfigure.multiprovider;

import dev.openfeature.contrib.providers.multiprovider.MultiProvider;
import dev.openfeature.contrib.providers.multiprovider.Strategy;
import dev.openfeature.sdk.Client;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.iromu.openfeature.boot.autoconfigure.envvar.EnvVarAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultiProviderAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class MultiProviderAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, EnvVarAutoConfiguration.class,
				MultiProviderAutoConfiguration.class));

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(Strategy.class)
			.hasBean("firstMatchStrategy")
			.hasSingleBean(MultiProvider.class)
			.hasBean("multiProvider")
			.hasSingleBean(Client.class)
			.hasBean("multiClient")
			.doesNotHaveBean("client"));
	}

}
