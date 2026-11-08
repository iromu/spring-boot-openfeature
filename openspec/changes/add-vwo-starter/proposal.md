## Why

VWO (Visual Website Optimizer) publishes an OpenFeature Java provider for its feature-flag and A/B testing capabilities. A Spring Boot starter would let consumers wire VWO flags with `spring.openfeature.vwo.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-vwo`: `VwoProperties` (`spring.openfeature.vwo.*`), a `VwoCustomizer`, and a `VwoAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is `com.vwo:vwo-openfeature-provider-java` (vendor-published). The BOM prices it via a `vwo-openfeature.version` property.
- Configuration maps to the VWO SDK surface: account ID, API key, and optional endpoint/environment.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `vwo-starter`: the auto-configuration contract of the VWO starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-vwo/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `vwo-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed VWO provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
