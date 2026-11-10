## Why

Ageris is an open-source feature-flag/experimentation platform that publishes an OpenFeature Java provider. A Spring Boot starter would let consumers wire Ageris flags with `spring.openfeature.ageris.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-ageris`: `AgerisProperties` (`spring.openfeature.ageris.*`), an `AgerisCustomizer`, and an `AgerisAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is vendor-published (Ageris SDK). The BOM prices it via an `ageris-openfeature.version` property.
- Configuration maps to the Ageris SDK surface: application name, configuration-file directory, feature-flag and JSON-config file names, and optional targeting-key-type/refresh-interval/first-load-timeout. Ageris is file-based and has no API key (design.md finding F2).
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `ageris-starter`: the auto-configuration contract of the Ageris starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-ageris/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `ageris-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed Ageris provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
