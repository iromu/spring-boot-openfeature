## Why

DevCycle is a commercial feature-flag platform that publishes a dedicated OpenFeature Java provider. A Spring Boot starter would let consumers wire DevCycle flags with `spring.openfeature.devcycle.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-devcycle`: `DevCycleProperties` (`spring.openfeature.devcycle.*`), a `DevCycleCustomizer`, and a `DevCycleAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is vendor-published (DevCycle's Java SDK). The BOM prices it via a `devcycle-openfeature.version` property.
- Configuration maps to the DevCycle SDK surface: server-side key, optional environment, and connection settings.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `devcycle-starter`: the auto-configuration contract of the DevCycle starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-devcycle/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `devcycle-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed DevCycle provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
