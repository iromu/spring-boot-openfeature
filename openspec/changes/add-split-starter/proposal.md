## Why

Split.io is a major commercial feature-flag platform that publishes a dedicated OpenFeature Java provider (`com.split:split-openfeature-provider-java`) outside the contrib monorepo. A Spring Boot starter would let consumers wire Split flags with `spring.openfeature.split.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-split`: `SplitProperties` (`spring.openfeature.split.*`), a `SplitCustomizer`, and a `SplitAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is `com.split:split-openfeature-provider-java` (vendor-published). The BOM prices it via a `split-openfeature.version` property.
- Configuration maps to the Split SDK's `SplitFactory`/`SplitClientConfig` surface: authorization key, optional domain, and connection settings.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `split-starter`: the auto-configuration contract of the Split starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-split/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `split-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed `com.split:split-openfeature-provider-java`.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
