## Why

Mixpanel's Java SDK includes an OpenFeature provider module. A Spring Boot starter would let consumers wire Mixpanel feature flags with `spring.openfeature.mixpanel.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-mixpanel`: `MixpanelProperties` (`spring.openfeature.mixpanel.*`), a `MixpanelCustomizer`, and a `MixpanelAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is part of Mixpanel's Java SDK (`com.mixpanel:mixpanel-openfeature-provider` or similar). The BOM prices it via a `mixpanel-openfeature.version` property.
- Configuration maps to the Mixpanel SDK surface: project token, and optional endpoint/environment.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `mixpanel-starter`: the auto-configuration contract of the Mixpanel starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-mixpanel/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `mixpanel-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed Mixpanel provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
