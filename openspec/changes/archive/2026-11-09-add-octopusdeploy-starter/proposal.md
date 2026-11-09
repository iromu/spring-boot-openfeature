## Why

Octopus Deploy publishes an OpenFeature Java provider for its feature-flag capabilities. A Spring Boot starter would let consumers wire Octopus Deploy feature flags with `spring.openfeature.octopusdeploy.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-octopusdeploy`: `OctopusDeployProperties` (`spring.openfeature.octopusdeploy.*`), an `OctopusDeployCustomizer`, and an `OctopusDeployAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is `OctopusDeploy:openfeature-provider-java` (vendor-published). The BOM prices it via an `octopusdeploy-openfeature.version` property.
- Configuration maps to the Octopus Deploy SDK surface: API key, server URL, and optional project/space.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `octopusdeploy-starter`: the auto-configuration contract of the Octopus Deploy starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-octopusdeploy/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `octopusdeploy-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed Octopus Deploy provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
