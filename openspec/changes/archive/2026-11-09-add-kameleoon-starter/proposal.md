## Why

Kameleoon is a feature-flag platform that publishes an OpenFeature Java provider. A Spring Boot starter would let consumers wire Kameleoon flags with `spring.openfeature.kameleoon.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-kameleoon`: `KameleoonProperties` (`spring.openfeature.kameleoon.*`), a `KameleoonCustomizer`, and a `KameleoonAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is vendor-published (Kameleoon's Java SDK). The BOM prices it via a `kameleoon-openfeature.version` property.
- Configuration maps to the Kameleoon SDK surface: API key, and optional endpoint/environment.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `kameleoon-starter`: the auto-configuration contract of the Kameleoon starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-kameleoon/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `kameleoon-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed Kameleoon provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
