# launchdarkly-starter Specification

## Purpose

Lets a Spring Boot application obtain an OpenFeature `FeatureProvider` backed by LaunchDarkly through auto-configuration and `spring.openfeature.launchdarkly.*` properties alone, so that unit-level behavior of this starter never depends on LaunchDarkly credentials.

## Requirements

### Requirement: The starter auto-registers the LaunchDarkly provider
When the LaunchDarkly provider library is on the classpath and the starter is enabled, the application context SHALL expose exactly one `FeatureProvider`, and SHALL expose the OpenFeature `Client` built on it, with no consumer-defined provider bean required; the provider library being absent from the classpath SHALL yield no contribution rather than a startup failure.

#### Scenario: Clean context with the starter present
- **WHEN** an application context runs with the starter's auto-configuration and the OpenFeature client auto-configuration, with no consumer provider bean
- **THEN** the context has a single `FeatureProvider` contributed by this starter
- **AND** the context has a single OpenFeature `Client`

### Requirement: The starter can be switched off declaratively
Setting the enablement property to false SHALL prevent the starter from contributing any `FeatureProvider`.

#### Scenario: Disabled via property
- **WHEN** `spring.openfeature.launchdarkly.enabled=false` is set
- **THEN** the context contains no `FeatureProvider` and no starter-contributed provider bean

### Requirement: A consumer-supplied provider wins
When the consumer defines their own `FeatureProvider` bean, the starter SHALL back off entirely.

#### Scenario: Consumer registers a provider
- **WHEN** the application defines its own `FeatureProvider` bean alongside the starter
- **THEN** the context has exactly one `FeatureProvider`, the consumer's
- **AND** the starter's provider bean is not present

### Requirement: Configuration is property-driven
The LaunchDarkly service key and any provider-specific options SHALL be bindable from `spring.openfeature.launchdarkly.*` properties; a registered customizer SHALL be able to adjust the assembled configuration after property binding.

#### Scenario: Service key supplied
- **WHEN** `spring.openfeature.launchdarkly.service-key` is set
- **THEN** the assembled provider configuration carries that key

#### Scenario: Customizer applied after binding
- **WHEN** a customizer bean is registered in the context
- **THEN** it is invoked against the configuration during assembly

### Requirement: Unit-level starter behavior requires no LaunchDarkly credentials
The tests that verify this starter's auto-configuration contract SHALL pass on a host with no LaunchDarkly credentials configured.

#### Scenario: Credential-free host runs the unit tier
- **WHEN** the module's test suite runs on a host with no LaunchDarkly credentials
- **THEN** the auto-configuration contract tests pass without network calls to LaunchDarkly
