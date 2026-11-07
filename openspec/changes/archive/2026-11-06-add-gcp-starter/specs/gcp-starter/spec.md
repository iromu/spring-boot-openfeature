## Purpose

Lets a Spring Boot application obtain an OpenFeature `FeatureProvider` that reads flag definitions from Google Secret Manager or Google Parameter Manager through auto-configuration and `spring.openfeature.gcp.*` properties alone, including in environments without ambient Google credentials, so that unit-level behavior of this starter never depends on credential availability.

## ADDED Requirements

### Requirement: The starter selects the requested backend variant
The starter SHALL expose exactly one `FeatureProvider` chosen by a `type` property between the Secret Manager-backed and Parameter Manager-backed variants, defaulting to Secret Manager, and SHALL reject a value naming neither variant with a startup failure that identifies the offending value.

#### Scenario: Default variant
- **WHEN** the starter is enabled and `type` is not set
- **THEN** the contributed `FeatureProvider` is the Secret Manager-backed variant

#### Scenario: Parameter Manager requested
- **WHEN** `spring.openfeature.gcp.type=parametermanager` is set
- **THEN** the contributed `FeatureProvider` is the Parameter Manager-backed variant

#### Scenario: Unknown variant requested
- **WHEN** `type` names neither supported variant
- **THEN** context startup fails with an error naming the offending value

### Requirement: The starter auto-registers the chosen provider
When the GCP provider library is on the classpath and the starter is enabled, the application context SHALL expose exactly one `FeatureProvider`, and SHALL expose the OpenFeature `Client` built on it, with no consumer-defined provider bean required; the provider library being absent from the classpath SHALL yield no contribution rather than a startup failure.

#### Scenario: Clean context with the starter present
- **WHEN** an application context runs with the starter's auto-configuration and the OpenFeature client auto-configuration, with no consumer provider bean
- **THEN** the context has a single `FeatureProvider` contributed by this starter
- **AND** the context has a single OpenFeature `Client`

### Requirement: The starter can be switched off declaratively
Setting the enablement property to false SHALL prevent the starter from contributing any `FeatureProvider`, leaving the context without one unless another source supplies it.

#### Scenario: Disabled via property
- **WHEN** `spring.openfeature.gcp.enabled=false` is set
- **THEN** the context contains no `FeatureProvider` and no starter-contributed provider bean

### Requirement: A consumer-supplied provider wins
When the consumer defines their own `FeatureProvider` bean, the starter SHALL back off entirely rather than producing a second or competing provider.

#### Scenario: Consumer registers a provider
- **WHEN** the application defines its own `FeatureProvider` bean alongside the starter
- **THEN** the context has exactly one `FeatureProvider`, the consumer's
- **AND** the starter's provider bean is not present

### Requirement: Credentials and lookup options are property-driven
The Google credentials used to reach the backend SHALL be sourceable from configuration — a service-account credential path — rather than relying solely on ambient defaults, and cache/lookup options (`version`, `location-id`, `cache-expiry`, `cache-max-size`, `name-prefix`) SHALL be bound from properties; a registered customizer SHALL be able to adjust the assembled options after property binding.

#### Scenario: Service-account path supplied
- **WHEN** a service-account credential path property is set
- **THEN** the assembled provider options carry credentials derived from that file rather than ambient defaults

#### Scenario: Customizer applied after binding
- **WHEN** a customizer bean is registered in the context
- **THEN** it is invoked against the options builder during assembly, and its adjustments are observable in the assembled options

### Requirement: Unit-level starter behavior requires no Google credentials
The tests that verify this starter's auto-configuration contract SHALL pass on a host with no Google credentials configured, and any test that contacts a real backend SHALL self-skip, rather than fail, when the gating project-identifier environment variable is absent.

#### Scenario: Credential-free host runs the unit tier
- **WHEN** the module's test suite runs on a host with no ambient Google credentials and no gating environment variable
- **THEN** the auto-configuration contract tests pass and any real-backend test reports itself skipped without failing the build

#### Scenario: Gating variable present
- **WHEN** the gating project-identifier environment variable is set to a usable value
- **THEN** the real-backend test executes against that project
