## Why

Featureflip is a feature-flag platform that publishes an OpenFeature Java provider. A Spring Boot starter would let consumers wire Featureflip flags with `spring.openfeature.featureflip.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-featureflip`: `FeatureflipProperties` (`spring.openfeature.featureflip.*`), a `FeatureflipCustomizer`, and a `FeatureflipAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is vendor-published. The BOM prices it via a `featureflip-openfeature.version` property.
- Configuration maps to the Featureflip SDK surface: API key, and optional endpoint/environment.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `featureflip-starter`: the auto-configuration contract of the Featureflip starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-featureflip/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `featureflip-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed Featureflip provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
