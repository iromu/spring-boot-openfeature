package org.iromu.openfeature.boot.autoconfigure.statsig;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.statsig.StatsigProperties.STATSIG_PREFIX;

/**
 * Tests for {@link StatsigAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class StatsigAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, StatsigAutoConfiguration.class));

	static String[] requiredProperties = new String[] { STATSIG_PREFIX + ".sdkKey=test",
			STATSIG_PREFIX + ".localMode=true" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.hasBean("statsigProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

}
