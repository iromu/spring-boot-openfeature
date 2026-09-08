# test-coverage Specification

## Purpose

Defines the behavioral contract and the quality floor for the Spring Boot OpenFeature integration: how provider auto-configuration activates and backs off, how customizers are applied, how the `@ToggleOnFlag` aspect decides, and that the build enforces a per-module coverage floor.

## Requirements

### Requirement: Build enforces a per-module coverage floor
The build SHALL fail the `verify` phase when any module's instruction-coverage ratio falls below its recorded per-module floor, and SHALL pass when every module meets or exceeds its floor.

#### Scenario: Coverage drops below the floor
- **WHEN** a change reduces a module's covered instructions below its recorded floor
- **THEN** the build's `verify` phase fails

#### Scenario: Coverage meets the floor
- **WHEN** every module's instruction coverage is at or above its recorded floor
- **THEN** the build's `verify` phase passes

### Requirement: Provider auto-configuration activates only when enabled
Each backend provider auto-configuration SHALL create its configuration and `FeatureProvider` beans when the provider's `enabled` property is true or unset, and SHALL NOT create them when the property is false.

This requirement covers the backend providers — `configcat`, `envvar`, `flagd`, `flagsmith`, `flipt`, `gofeatureflag`, `jsonlogic`, `statsig`, `unleash`, and `growthbook` — and no others. The `multiprovider` auto-configuration is explicitly exempt: it is a composite aggregator over the providers it already aggregates, so its enablement is governed by those providers rather than by its own toggle; it has never exposed an `enabled` property; and introducing one would alter provider runtime behavior and the public configuration surface, which design.md's Non-Goals place out of this change's scope. The exemption names `multiprovider` alone — `envvar` and `jsonlogic` do expose an `enabled` gate and remain fully covered.

#### Scenario: Provider disabled
- **WHEN** a backend provider's `enabled` property is set to `false`
- **THEN** the application context contains no `FeatureProvider` bean for that provider

#### Scenario: Provider enabled by default
- **WHEN** a backend provider's `enabled` property is unset
- **THEN** the application context contains the provider's `FeatureProvider` bean

### Requirement: Auto-configuration yields to a user-supplied provider
When the application defines its own `FeatureProvider` bean, the auto-configuration SHALL NOT create its own provider bean for that provider.

#### Scenario: User supplies a provider bean
- **WHEN** the application context already defines a `FeatureProvider` bean
- **THEN** the auto-configuration does not register a competing provider bean

### Requirement: Provider customizers are applied
Where a provider exposes a customizer extension point, a registered customizer bean SHALL be applied to the provider configuration before it is built.

#### Scenario: Customizer mutates configuration
- **WHEN** a provider customizer bean is registered
- **THEN** the constructed provider configuration reflects the customization

### Requirement: ToggleOnFlag executes based on the flag value
The `@ToggleOnFlag` aspect SHALL invoke the target method when the flag resolves to true, SHALL invoke the declared fallback when the flag is false and a fallback is present, and SHALL proceed with the target method when the flag is false and no fallback is declared.

#### Scenario: Flag true
- **WHEN** the flag key resolves to `true`
- **THEN** the annotated method executes

#### Scenario: Flag false with fallback
- **WHEN** the flag key resolves to `false` and an `orElse` fallback is declared
- **THEN** the fallback method is invoked

#### Scenario: Flag false without fallback
- **WHEN** the flag key resolves to `false` and no `orElse` fallback is declared
- **THEN** the annotated method executes

### Requirement: ToggleOnFlag attribute resolution is validated
When the attribute expression does not resolve to a valid attribute map — either a duplicate key or a non-map result — the `@ToggleOnFlag` aspect SHALL raise a clear error rather than silently proceeding.

#### Scenario: Duplicate attribute key
- **WHEN** the attribute expression resolves to a map containing a duplicate key
- **THEN** the aspect raises an error

#### Scenario: Non-map attribute result
- **WHEN** the attribute expression does not resolve to a map
- **THEN** the aspect raises an error

### Requirement: OpenFeatureAPI provider lifecycle events are wired
The default API customizer SHALL register handlers for the ready, error, stale, and configuration-changed provider lifecycle events.

#### Scenario: Each lifecycle event fires
- **WHEN** each of the ready, error, stale, and configuration-changed events is fired
- **THEN** the corresponding registered handler is invoked
