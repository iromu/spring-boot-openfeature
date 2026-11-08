## Why

LaunchDarkly is one of the most widely-used commercial feature-flag platforms and publishes a dedicated OpenFeature Java provider (`com.launchdarkly:openfeature-java-server`) outside the `open-feature/java-sdk-contrib` monorepo. A Spring Boot starter would let consumers wire LaunchDarkly flags into a Spring application with `spring.openfeature.launchdarkly.*` properties alone, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-launchdarkly`: `LaunchDarklyProperties` (`spring.openfeature.launchdarkly.*`), a `LaunchDarklyCustomizer`, and a `LaunchDarklyAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`, following the established provider-starter pattern with **unleash** as the structural reference.
- The provider library is `com.launchdarkly:openfeature-java-server` (vendor-published, not a contrib artifact). The BOM prices it via a `launchdarkly-openfeature.version` property.
- Configuration maps to the LaunchDarkly SDK's `Config`/`SdkConfig` surface: service key, environment, and optional streaming/polling settings.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `launchdarkly-starter`: the auto-configuration contract of the LaunchDarkly starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-launchdarkly/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `launchdarkly-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed `com.launchdarkly:openfeature-java-server`.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
