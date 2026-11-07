# ofrep-starter Specification

## Purpose

Lets a Spring Boot application obtain an OpenFeature `FeatureProvider` backed by an OFREP (OpenFeature Remote Evaluation Protocol) endpoint through auto-configuration and `spring.openfeature.ofrep.*` properties alone, with no manual provider wiring.

## Requirements

### Requirement: The starter auto-registers an OFREP-backed provider
When the OFREP provider library is on the classpath and the starter is enabled, the application context SHALL expose exactly one `FeatureProvider`, backed by the OFREP provider, and SHALL expose the OpenFeature `Client` built on it, with no consumer-defined provider bean required.

#### Scenario: Clean context with the starter present
- **WHEN** an application context runs with the starter's auto-configuration and the OpenFeature client auto-configuration, with no consumer provider bean
- **THEN** the context has a single `FeatureProvider` contributed by this starter
- **AND** the context has a single OpenFeature `Client`

#### Scenario: Provider library absent from the classpath
- **WHEN** the OFREP provider classes are not on the classpath
- **THEN** this starter contributes no `FeatureProvider` and does not fail context startup

### Requirement: The starter can be switched off declaratively
Setting the enablement property to false SHALL prevent the starter from contributing any `FeatureProvider`, leaving the context without one unless another source supplies it.

#### Scenario: Disabled via property
- **WHEN** `spring.openfeature.ofrep.enabled=false` is set
- **THEN** the context contains no `FeatureProvider` and no starter-contributed provider bean

### Requirement: A consumer-supplied provider wins
When the consumer defines their own `FeatureProvider` bean, the starter SHALL back off entirely rather than producing a second or competing provider.

#### Scenario: Consumer registers a provider
- **WHEN** the application defines its own `FeatureProvider` bean alongside the starter
- **THEN** the context has exactly one `FeatureProvider`, the consumer's
- **AND** the starter's provider bean is not present

### Requirement: Endpoint configuration is property-driven
The OFREP connection settings SHALL be reachable through `spring.openfeature.ofrep.*` properties, and a registered customizer SHALL be able to adjust the provider options after property binding; the effective options SHALL reflect both.

#### Scenario: Endpoint and timeouts bound from properties
- **WHEN** `spring.openfeature.ofrep.base-url`, `connect-timeout`, `request-timeout`, or `headers` are set
- **THEN** the provider options used at construction carry those values

#### Scenario: Customizer applied after binding
- **WHEN** a customizer bean is registered in the context
- **THEN** it is invoked against the options builder during construction, and its adjustments are observable in the constructed options
