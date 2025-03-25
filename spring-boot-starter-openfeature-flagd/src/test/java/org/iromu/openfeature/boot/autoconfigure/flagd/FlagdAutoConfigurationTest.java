package org.iromu.openfeature.boot.autoconfigure.flagd;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.flagd.FlagdProperties.FLAGD_PREFIX;

/**
 * Tests for {@link FlagdAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class FlagdAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, FlagdAutoConfiguration.class));

	static String[] requiredProperties = new String[] { FLAGD_PREFIX + ".resolverType=file",
			FLAGD_PREFIX + ".offlineFlagSourcePath=flags/testing-flags.json" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.hasBean("flagdProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

}
