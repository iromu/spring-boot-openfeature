## Why

ConfigDirector is a feature-flag/configuration platform that publishes an OpenFeature Java provider. A Spring Boot starter would let consumers wire ConfigDirector flags with `spring.openfeature.configdirector.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-configdirector`: `ConfigDirectorProperties` (`spring.openfeature.configdirector.*`), a `ConfigDirectorCustomizer`, and a `ConfigDirectorAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is vendor-published (ConfigDirector's Java SDK). The BOM prices it via a `configdirector-openfeature.version` property.
- Configuration maps to the ConfigDirector SDK surface: API key, and optional endpoint/environment.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `configdirector-starter`: the auto-configuration contract of the ConfigDirector starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-configdirector/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `configdirector-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed ConfigDirector provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
