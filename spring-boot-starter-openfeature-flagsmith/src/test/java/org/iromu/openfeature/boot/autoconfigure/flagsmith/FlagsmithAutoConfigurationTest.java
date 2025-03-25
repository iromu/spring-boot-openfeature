package org.iromu.openfeature.boot.autoconfigure.flagsmith;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import org.iromu.openfeature.boot.autoconfigure.ClientAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.iromu.openfeature.boot.flagsmith.FlagsmithProperties.FLAGSMITH_PREFIX;

/**
 * Tests for {@link FlagsmithAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class FlagsmithAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ClientAutoConfiguration.class, FlagsmithAutoConfiguration.class));

	static String[] requiredProperties = new String[] { FLAGSMITH_PREFIX + ".apiKey=API_KEY" };

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.withPropertyValues(requiredProperties)
			.run((context) -> assertThat(context).hasSingleBean(FeatureProvider.class)
				.hasBean("flagsmithProvider")
				.hasSingleBean(Client.class)
				.hasBean("client"));
	}

}
