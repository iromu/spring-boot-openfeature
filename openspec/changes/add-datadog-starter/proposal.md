## Why

Datadog offers a feature-flag product with a dedicated OpenFeature Java provider. A Spring Boot starter would let consumers wire Datadog feature flags with `spring.openfeature.datadog.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-datadog`: `DatadogProperties` (`spring.openfeature.datadog.*`), a `DatadogCustomizer`, and a `DatadogAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is vendor-published (Datadog's Java SDK). The BOM prices it via a `datadog-openfeature.version` property.
- Configuration maps to the Datadog SDK surface: API key, application key, site/region, and optional environment.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `datadog-starter`: the auto-configuration contract of the Datadog starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-datadog/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `datadog-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed Datadog provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
