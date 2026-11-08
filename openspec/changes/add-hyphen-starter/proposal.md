## Why

Hyphen AI publishes an OpenFeature Java provider for its AI-focused feature-flag capabilities. A Spring Boot starter would let consumers wire Hyphen AI flags with `spring.openfeature.hyphen.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-hyphen`: `HyphenProperties` (`spring.openfeature.hyphen.*`), a `HyphenCustomizer`, and a `HyphenAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is vendor-published (Hyphen AI's Java SDK). The BOM prices it via a `hyphen-openfeature.version` property.
- Configuration maps to the Hyphen AI SDK surface: API key, and optional endpoint/environment.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `hyphen-starter`: the auto-configuration contract of the Hyphen AI starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-hyphen/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `hyphen-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed Hyphen AI provider coordinate.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
