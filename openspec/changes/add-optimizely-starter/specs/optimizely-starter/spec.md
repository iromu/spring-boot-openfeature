## Purpose

Lets a Spring Boot application obtain an OpenFeature `FeatureProvider` backed by Optimizely through auto-configuration and `spring.openfeature.optimizely.*` properties alone, with the provider's vendor-SDK configuration assembled by the starter rather than hand-wired by the consumer.

## ADDED Requirements

### Requirement: The starter auto-registers an Optimizely-backed provider
When the Optimizely provider library is on the classpath and the starter is enabled, the application context SHALL expose exactly one `FeatureProvider`, backed by the Optimizely provider, and SHALL expose the OpenFeature `Client` built on it, with no consumer-defined provider bean required.

#### Scenario: Clean context with the starter present
- **WHEN** an application context runs with the starter's auto-configuration and the OpenFeature client auto-configuration, with no consumer provider bean
- **THEN** the context has a single `FeatureProvider` contributed by this starter
- **AND** the context has a single OpenFeature `Client`

#### Scenario: Provider library absent from the classpath
- **WHEN** the Optimizely provider classes are not on the classpath
- **THEN** this starter contributes no `FeatureProvider` and does not fail context startup

### Requirement: The starter can be switched off declaratively
Setting the enablement property to false SHALL prevent the starter from contributing any `FeatureProvider`, and SHALL not construct the vendor-backed provider — construction that starts vendor machinery must not happen for a disabled starter.

#### Scenario: Disabled via property
- **WHEN** `spring.openfeature.optimizely.enabled=false` is set
- **THEN** the context contains no `FeatureProvider` and no starter-contributed provider bean

### Requirement: A consumer-supplied provider wins
When the consumer defines their own `FeatureProvider` bean, the starter SHALL back off entirely rather than producing a second or competing provider.

#### Scenario: Consumer registers a provider
- **WHEN** the application defines its own `FeatureProvider` bean alongside the starter
- **THEN** the context has exactly one `FeatureProvider`, the consumer's
- **AND** the starter's provider bean is not present

### Requirement: Provider configuration is property- and customizer-driven
The provider configuration the starter assembles SHALL derive from `spring.openfeature.optimizely.*` properties, and a registered customizer SHALL be able to adjust that configuration after property binding; the assembled configuration SHALL reflect both.

#### Scenario: Datafile supplied via property
- **WHEN** `spring.openfeature.optimizely.datafile` is set
- **THEN** the provider configuration handed to the provider carries that datafile

#### Scenario: Customizer applied after binding
- **WHEN** a customizer bean is registered in the context
- **THEN** it is invoked against the configuration builder during assembly, and its adjustments are observable in the assembled configuration
