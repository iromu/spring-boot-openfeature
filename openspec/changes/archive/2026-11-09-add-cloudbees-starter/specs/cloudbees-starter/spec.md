## Purpose

Lets a Spring Boot application obtain an OpenFeature `FeatureProvider` backed by CloudBees through auto-configuration and `spring.openfeature.cloudbees.*` properties alone, so that unit-level behavior of this starter never depends on CloudBees credentials.

## ADDED Requirements

### Requirement: The starter auto-registers the CloudBees provider
When the CloudBees provider library is on the classpath and the starter is enabled, the application context SHALL expose exactly one `FeatureProvider`, and SHALL expose the OpenFeature `Client` built on it; the provider library being absent SHALL yield no contribution rather than a startup failure.

#### Scenario: Clean context with the starter present
- **WHEN** an application context runs with the starter's auto-configuration and the OpenFeature client auto-configuration, with no consumer provider bean
- **THEN** the context has a single `FeatureProvider` contributed by this starter
- **AND** the context has a single OpenFeature `Client`

### Requirement: The starter can be switched off declaratively
Setting the enablement property to false SHALL prevent the starter from contributing any `FeatureProvider`.

#### Scenario: Disabled via property
- **WHEN** `spring.openfeature.cloudbees.enabled=false` is set
- **THEN** the context contains no `FeatureProvider` and no starter-contributed provider bean

### Requirement: A consumer-supplied provider wins
When the consumer defines their own `FeatureProvider` bean, the starter SHALL back off entirely.

#### Scenario: Consumer registers a provider
- **WHEN** the application defines its own `FeatureProvider` bean alongside the starter
- **THEN** the context has exactly one `FeatureProvider`, the consumer's

### Requirement: Configuration is property-driven
The CloudBees API key and any provider-specific options SHALL be bindable from `spring.openfeature.cloudbees.*` properties; a registered customizer SHALL be able to adjust the assembled configuration.

#### Scenario: API key supplied
- **WHEN** `spring.openfeature.cloudbees.api-key` is set
- **THEN** the assembled provider configuration carries that key

#### Scenario: Customizer applied after binding
- **WHEN** a customizer bean is registered in the context
- **THEN** it is invoked against the configuration during assembly

### Requirement: Unit-level starter behavior requires no CloudBees credentials
The tests SHALL pass on a host with no CloudBees credentials configured.

#### Scenario: Credential-free host runs the unit tier
- **WHEN** the module's test suite runs on a host with no CloudBees credentials
- **THEN** the auto-configuration contract tests pass without network calls to CloudBees
