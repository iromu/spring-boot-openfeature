## Why

CloudBees publishes an OpenFeature Java provider for its CI/CD platform's feature-flag capabilities. A Spring Boot starter would let consumers wire CloudBees flags with `spring.openfeature.cloudbees.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-cloudbees`: `CloudBeesProperties` (`spring.openfeature.cloudbees.*`), a `CloudBeesCustomizer`, and a `CloudBeesAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is `com.cloudbees:cloudbees-openfeature-provider-java` (vendor-published). The BOM prices it via a `cloudbees-openfeature.version` property.
- Configuration maps to the CloudBees SDK surface: API key, and optional endpoint/environment.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `cloudbees-starter`: the auto-configuration contract of the CloudBees starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-cloudbees/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `cloudbees-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed CloudBees provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
