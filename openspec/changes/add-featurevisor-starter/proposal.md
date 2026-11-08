## Why

Featurevisor is an open-source feature-flag platform that publishes an OpenFeature Java provider. A Spring Boot starter would let consumers wire Featurevisor flags with `spring.openfeature.featurevisor.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-featurevisor`: `FeaturevisorProperties` (`spring.openfeature.featurevisor.*`), a `FeaturevisorCustomizer`, and a `FeaturevisorAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is vendor-published (Featurevisor's Java SDK). The BOM prices it via a `featurevisor-openfeature.version` property.
- Configuration maps to the Featurevisor SDK surface: API key, and optional endpoint/environment.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `featurevisor-starter`: the auto-configuration contract of the Featurevisor starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-featurevisor/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `featurevisor-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed Featurevisor provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
