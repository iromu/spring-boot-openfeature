## Why

Upstream `open-feature/java-sdk-contrib` ships 13 providers under `providers/`; this repo publishes starters for 11 of them (verified: the two lists were compared dir-for-dir against the GitHub contents API, and every local starter maps to an upstream dir except `growthbook`, which is tracked separately). OFREP — the OpenFeature Remote Evaluation Protocol — is one of the three missing ones. It is the standards-track HTTP protocol for remote flag evaluation (the same family flagd serves), so consumers who want a protocol-based, non-vendor-specific evaluation backend have no starter to reach for. The provider artifact is published and consumable today: `dev.openfeature.contrib.providers:ofrep:0.0.2` resolved from Maven Central via `./mvnw dependency:get` during this exploration, and its API surface was read from the downloaded jar with `javap`.

## What Changes

- New module `spring-boot-starter-openfeature-ofrep`: a `OfrepProperties` (`spring.openfeature.ofrep.*`), an `OfrepCustomizer`, and an `OfrepAutoConfiguration` that produces the `FeatureProvider` bean ahead of `ClientAutoConfiguration`, following the established provider-starter pattern with **flagd** as the structural template.
- The provider is obtained through `OfrepProvider.constructProvider(OfrepProviderOptions)` — the jar shows no public constructor, so the factory is the only supported route.
- Properties bind the options surface the jar actually exposes: `baseUrl`, `connectTimeout`, `requestTimeout`, `headers` (plus the standard `enabled` flag).
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry for `ofrep` priced by a new `ofrep.version` property; the root `pom.xml` gains the `<module>` and the property.
- The module carries the repo's per-module JaCoCo instruction floor (set from a measured value once tests land, per the ratchet convention).

## Capabilities

### New Capabilities

- `ofrep-starter`: the auto-configuration contract of the OFREP starter — provider bean presence and ordering, back-off, enablement, property binding, and customizer application.

### Modified Capabilities

(none — this adds a new starter; `test-coverage`'s existing per-module floor requirement already governs the new module's coverage check without a requirement change, and `dependency-upgrades` requirements are unaffected by adding a managed coordinate.)

## Impact

- New: `spring-boot-starter-openfeature-ofrep/` (pom, `org.iromu.openfeature.boot.ofrep.{OfrepProperties,OfrepCustomizer}`, `org.iromu.openfeature.boot.autoconfigure.ofrep.OfrepAutoConfiguration`, `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, `src/test/java`).
- `pom.xml` (root): `<module>spring-boot-starter-openfeature-ofrep</module>` + `<ofrep.version>0.0.2</ofrep.version>`; reactor grows from 17 to 18 modules (to 20 once the sibling `add-optimizely-starter` / `add-gcp-starter` changes merge).
- `spring-boot-openfeature-dependencies/pom.xml`: managed `dev.openfeature.contrib.providers:ofrep`.
- CI (`pull_request.yml`) needs no edit: enforcement comes from the new module being in the ordinary `clean verify` reactor.
- Lockstep watch: the contrib parent resolves the SDK through a version range, so the build must confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version` (`1.22.1`), not a range-selected drift (see `design.md`).
- Out of scope, recorded so it is not mistaken for an omission: no eventing/streaming surface beyond what `OfrepProvider` itself exposes; the `growthbook` provenance gap is tracked outside this change.
