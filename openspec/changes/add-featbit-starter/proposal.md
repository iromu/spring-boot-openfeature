## Why

FeatBit is a feature-flag platform that publishes an OpenFeature Java server provider (`com.featbit:featbit-openfeature-provider-java-server`) outside the contrib monorepo. A Spring Boot starter would let consumers wire FeatBit flags with `spring.openfeature.featbit.*` properties, following the established provider-starter pattern.

## What Changes

- New module `spring-boot-starter-openfeature-featbit`: `FeatBitProperties` (`spring.openfeature.featbit.*`), a `FeatBitCustomizer`, and a `FeatBitAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`.
- The provider library is `com.featbit:featbit-openfeature-provider-java-server` (vendor-published). The BOM prices it via a `featbit-openfeature.version` property.
- Configuration maps to the FeatBit SDK surface: token (app token), and optional endpoint/region.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry; the root `pom.xml` gains the `<module>` and version properties.
- The module carries the repo's per-module JaCoCo instruction floor.

## Capabilities

### New Capabilities

- `featbit-starter`: the auto-configuration contract of the FeatBit starter — provider bean presence and ordering, back-off, enablement, property binding, and a credential-free unit-test posture.

### Modified Capabilities

(none — new starter only.)

## Impact

- New: `spring-boot-starter-openfeature-featbit/` (pom, properties, customizer, auto-configuration, `AutoConfiguration.imports`, tests).
- `pom.xml` (root): `<module>` + `featbit-openfeature.version`.
- `spring-boot-openfeature-dependencies/pom.xml`: managed `com.featbit:featbit-openfeature-provider-java-server`.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version`.
