## Why

Upstream `open-feature/java-sdk-contrib` ships 13 providers under `providers/`; this repo publishes starters for 10 of them (growthbook's starter is vendor-hosted, not a contrib artifact; the 13 upstream providers verified dir-for-dir against the GitHub contents API). Optimizely is one of the three missing ones, and unlike the other two it is at upstream `1.0.0` — a GA-level release, not an early `0.0.x` — so it is the most production-ready gap on the list. The provider artifact resolves from Maven Central today (`dev.openfeature.contrib.providers:optimizely:1.0.0` via `./mvnw dependency:get`), and its API surface was read from the downloaded jar with `javap`.

## What Changes

- New module `spring-boot-starter-openfeature-optimizely`: `OptimizelyProperties` (`spring.openfeature.optimizely.*`), an `OptimizelyCustomizer`, and an `OptimizelyAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`, following the established provider-starter pattern with the vendor-SDK-backed starters (**statsig**, **unleash**, **flagsmith**) as the structural family.
- The provider is obtained through its public constructor `new OptimizelyProvider(OptimizelyProviderConfig)`; the config is Lombok-builder-shaped and carries vendor collaborators (`ProjectConfigManager`, `ProjectConfig`, `EventHandler`, `EventProcessor`, `UserProfileService`, `ErrorHandler`, `ODPManager`, `datafile`, `defaultDecideOptions`) plus a `getOptimizely()` accessor — how those get built from properties is the change's central design question (`design.md`).
- The vendor SDK coordinates the provider's pom prices — `com.optimizely.ab:core-api` and `com.optimizely.ab:core-httpclient-impl` — get BOM-managed versions alongside the provider version, mirroring how `unleash-client-java.version` and `flagsmith-java-client.version` sit next to their provider versions.
- `spring-boot-openfeature-dependencies/pom.xml` gains managed entries priced by new `optimizely.version` (1.0.0) and `optimizely-sdk.version` properties; the root `pom.xml` gains the `<module>` and the properties.
- The module carries the repo's per-module JaCoCo instruction floor (measured value once tests land, per the ratchet convention).

## Capabilities

### New Capabilities

- `optimizely-starter`: the auto-configuration contract of the Optimizely starter — provider bean presence and ordering, back-off, enablement, property/customizer-driven config construction.

### Modified Capabilities

(none — new starter only; the new module's coverage check is already governed by `test-coverage`'s existing per-module floor requirement.)

## Impact

- New: `spring-boot-starter-openfeature-optimizely/` (pom, `org.iromu.openfeature.boot.optimizely.{OptimizelyProperties,OptimizelyCustomizer}`, `org.iromu.openfeature.boot.autoconfigure.optimizely.OptimizelyAutoConfiguration`, `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, `src/test/java`).
- `pom.xml` (root): `<module>` + `optimizely.version` + `optimizely-sdk.version`; reactor grows from 17 to 18 modules (to 20 once the sibling `add-ofrep-starter` / `add-gcp-starter` changes merge).
- `spring-boot-openfeature-dependencies/pom.xml`: managed `dev.openfeature.contrib.providers:optimizely` plus the two `com.optimizely.ab` vendor coordinates.
- New third-party group on the classpath (`com.optimizely.ab`) — `gitleaks`/Checkstyle need no change, but the BOM is now the sole price-setter for the vendor SDK, so its version must move with the provider's (the SDK-contrib lockstep convention, extended to the vendor SDK).
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version` (`1.22.1`) rather than a contrib-parent-range drift (see `design.md`).
- Out of scope, recorded so it is not mistaken for an omission: ODP/eventing feature surfacing beyond what the config builder already accepts.
