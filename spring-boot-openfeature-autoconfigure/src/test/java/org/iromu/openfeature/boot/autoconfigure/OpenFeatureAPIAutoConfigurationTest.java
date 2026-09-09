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

package org.iromu.openfeature.boot.autoconfigure;

import dev.openfeature.sdk.EventDetails;
import dev.openfeature.sdk.ImmutableMetadata;
import dev.openfeature.sdk.OpenFeatureAPI;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenFeatureAPIAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class OpenFeatureAPIAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OpenFeatureAPIAutoConfiguration.class));

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(OpenFeatureAPICustomizer.class)
			.hasBean("loggerOpenFeatureAPICustomizer")
			.hasSingleBean(OpenFeatureAPI.class)
			.hasBean("openFeatureAPI"));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void loggerCustomizerShouldWireAllProviderLifecycleHandlers() {
		this.contextRunner.run((context) -> {
			OpenFeatureAPICustomizer customizer = context.getBean(OpenFeatureAPICustomizer.class);

			OpenFeatureAPI api = Mockito.mock(OpenFeatureAPI.class);
			customizer.customize(api);

			ArgumentCaptor<Consumer> ready = ArgumentCaptor.forClass(Consumer.class);
			ArgumentCaptor<Consumer> error = ArgumentCaptor.forClass(Consumer.class);
			ArgumentCaptor<Consumer> stale = ArgumentCaptor.forClass(Consumer.class);
			ArgumentCaptor<Consumer> configurationChanged = ArgumentCaptor.forClass(Consumer.class);

			Mockito.verify(api).onProviderReady(ready.capture());
			Mockito.verify(api).onProviderError(error.capture());
			Mockito.verify(api).onProviderStale(stale.capture());
			Mockito.verify(api).onProviderConfigurationChanged(configurationChanged.capture());

			ImmutableMetadata metadata = Mockito.mock(ImmutableMetadata.class);
			Mockito.when(metadata.getString("flagA")).thenReturn("changed");

			EventDetails details = Mockito.mock(EventDetails.class);
			Mockito.when(details.getProviderName()).thenReturn("test-provider");
			Mockito.when(details.getFlagsChanged()).thenReturn(List.of("flagA"));
			Mockito.when(details.getEventMetadata()).thenReturn(metadata);

			// Firing each registered handler must exercise its body without throwing.
			ready.getValue().accept(details);
			error.getValue().accept(details);
			stale.getValue().accept(details);
			configurationChanged.getValue().accept(details);

			// The configuration-changed handler reads the changed flags and their
			// metadata.
			Mockito.verify(details, Mockito.atLeast(1)).getProviderName();
			Mockito.verify(details).getFlagsChanged();
			Mockito.verify(details).getEventMetadata();
			Mockito.verify(metadata).getString("flagA");
		});
	}

}
