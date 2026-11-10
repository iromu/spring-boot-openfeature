## Purpose

Lets a Spring Boot application obtain an OpenFeature `FeatureProvider` backed by Ageris through auto-configuration and `spring.openfeature.ageris.*` properties alone, so that unit-level behavior of this starter never depends on Ageris configuration files or credentials.

## ADDED Requirements

### Requirement: The starter auto-registers the Ageris provider
When the Ageris provider library is on the classpath and the starter is enabled, the application context SHALL expose exactly one `FeatureProvider`, and SHALL expose the OpenFeature `Client` built on it; the provider library being absent SHALL yield no contribution rather than a startup failure.

#### Scenario: Clean context with the starter present
- **WHEN** an application context runs with the starter's auto-configuration and the OpenFeature client auto-configuration, with no consumer provider bean
- **THEN** the context has a single `FeatureProvider` contributed by this starter
- **AND** the context has a single OpenFeature `Client`

### Requirement: The starter can be switched off declaratively
Setting the enablement property to false SHALL prevent the starter from contributing any `FeatureProvider`.

#### Scenario: Disabled via property
- **WHEN** `spring.openfeature.ageris.enabled=false` is set
- **THEN** the context contains no `FeatureProvider` and no starter-contributed provider bean

### Requirement: A consumer-supplied provider wins
When the consumer defines their own `FeatureProvider` bean, the starter SHALL back off entirely.

#### Scenario: Consumer registers a provider
- **WHEN** the application defines its own `FeatureProvider` bean alongside the starter
- **THEN** the context has exactly one `FeatureProvider`, the consumer's

### Requirement: Configuration is property-driven
The Ageris application name, configuration-file directory, feature-flag file names, and any provider-specific options SHALL be bindable from `spring.openfeature.ageris.*` properties; a registered customizer SHALL be able to adjust the assembled Ageris configuration. The starter is file-based and has no API key: when the starter is enabled, the application name, the directory path and at least one feature-flag file name are required, and the auto-configuration SHALL fail fast with an error naming the missing property path when any of them is unset.

#### Scenario: Required inputs supplied
- **WHEN** `spring.openfeature.ageris.application`, `spring.openfeature.ageris.path` and `spring.openfeature.ageris.feature-flag-files` are set
- **THEN** the assembled provider configuration carries those values and the provider is built

#### Scenario: Required input missing
- **WHEN** the starter is enabled and any of the required properties is unset
- **THEN** context startup fails with an error naming the missing `spring.openfeature.ageris.*` property

#### Scenario: Customizer applied after binding
- **WHEN** a customizer bean is registered in the context
- **THEN** it is invoked against the assembled Ageris configuration before the managers are built

### Requirement: Unit-level starter behavior requires no Ageris configuration
The tests SHALL pass on a host with no Ageris configuration files or credentials configured.

#### Scenario: Configuration-free host runs the unit tier
- **WHEN** the module's test suite runs on a host with no Ageris configuration
- **THEN** the auto-configuration contract tests pass without reading any Ageris configuration file or making network calls
