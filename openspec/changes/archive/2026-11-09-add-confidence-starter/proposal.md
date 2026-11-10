## Why

Spotify's Confidence is an internal feature-flag/experimentation platform that publishes an OpenFeature Java provider. A Spring Boot starter would let consumers wire Confidence flags with `spring.openfeature.confidence.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-confidence`: `ConfidenceProperties` (`spring.openfeature.confidence.*`), a `ConfidenceCustomizer`, and a `ConfidenceAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is vendor-published (Spotify's Confidence SDK). The BOM prices it via a `confidence-openfeature.version` property.
- Configuration maps to the Confidence SDK surface: API key/token, and optional endpoint/environment.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `confidence-starter`: the auto-configuration contract of the Confidence starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-confidence/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `confidence-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed Confidence provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
