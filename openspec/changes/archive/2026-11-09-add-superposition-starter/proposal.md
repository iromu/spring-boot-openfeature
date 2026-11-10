## Why

Superposition (by Juspay) is a fintech feature-flag platform that publishes an OpenFeature Java provider. A Spring Boot starter would let consumers wire Superposition flags with `spring.openfeature.superposition.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-superposition`: `SuperpositionProperties` (`spring.openfeature.superposition.*`), a `SuperpositionCustomizer`, and a `SuperpositionAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is vendor-published (Juspay's Superposition Java client). The BOM prices it via a `superposition-openfeature.version` property.
- Configuration maps to the Superposition SDK surface: API key/secret, and optional endpoint/environment.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `superposition-starter`: the auto-configuration contract of the Superposition starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-superposition/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `superposition-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed Superposition provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
