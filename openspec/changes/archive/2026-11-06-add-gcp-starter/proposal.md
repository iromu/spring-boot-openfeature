## Why

Upstream `open-feature/java-sdk-contrib` ships 13 providers under `providers/`; this repo publishes starters for 11 of them (verified dir-for-dir against the GitHub contents API). The GCP Manager provider — flag definitions served from Google Secret Manager or Parameter Manager — is one of the three missing ones. It is also the only one whose testing story is genuinely non-obvious, and this exploration resolved that: upstream's own unit tests Mockito-mock the GAX service client (`SecretManagerServiceClient` / `ParameterManagerServiceClient`) and never touch the transport, and its integration tests self-skip via `@DisabledIfEnvironmentVariable(name = "GCP_PROJECT_ID", matches = "")` rather than fake anything. The artifact resolves from Maven Central today (`dev.openfeature.contrib.providers:gcp:0.0.2` via `./mvnw dependency:get`), and its API surface was read from the downloaded jar with `javap`.

## What Changes

- New module `spring-boot-starter-openfeature-gcp`: `GcpProperties` (`spring.openfeature.gcp.*`), a `GcpCustomizer`, and a `GcpAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`, following the established provider-starter pattern with **unleash** as the structural reference.
- One module serves both provider variants — `GcpSecretManagerProvider` and `GcpParameterManagerProvider` — selected by a `type` property (`secretmanager` | `parametermanager`), since both share `GcpProviderOptions` (`projectId`, `credentials`, `version`, `locationId`, `cacheExpiry`, `cacheMaxSize`, `namePrefix`).
- Credentials are a first-class property (`service-account` JSON path) wired to `GcpProviderOptions.credentials`, not an implicit ADC dependency: the jar shows `createClient()` runs from `initialize()`, making this the first starter in the repo whose provider initialization requires credentials — which is precisely why the credential path must be property-driven and test-substitutable (`design.md`).
- Test strategy is fixed by the jar's facts: the client-accepting constructor overloads are package-private, so starter tests use the established back-off pattern (user-supplied mock `FeatureProvider`) plus real construction under substitute credentials — **not** client injection through the public API.
- `spring-boot-openfeature-dependencies/pom.xml` gains a managed entry priced by a new `gcp.version` property (0.0.2), and BOM coverage for the `com.google.cloud` Secret Manager / Parameter Manager coordinates the provider's pom prices; the root `pom.xml` gains the `<module>` and the properties.
- The module carries the repo's per-module JaCoCo instruction floor (measured value once tests land, per the ratchet convention).

## Capabilities

### New Capabilities

- `gcp-starter`: the auto-configuration contract of the GCP Manager starter — variant selection, provider bean presence and ordering, back-off, enablement, property binding, credential sourcing, and a credential-free unit-test posture with an env-gated integration tier.

### Modified Capabilities

(none — new starter only; its coverage check is governed by `test-coverage`'s existing per-module floor requirement.)

## Impact

- New: `spring-boot-starter-openfeature-gcp/` (pom, `org.iromu.openfeature.boot.gcp.{GcpProperties,GcpCustomizer}`, `org.iromu.openfeature.boot.autoconfigure.gcp.GcpAutoConfiguration`, `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, `src/test/java`).
- `pom.xml` (root): `<module>` + `gcp.version` (+ `google-cloud-*.version` pricing approach per `design.md`); reactor grows from 17 to 18 modules (to 20 once the sibling `add-ofrep-starter` / `add-optimizely-starter` changes merge).
- `spring-boot-openfeature-dependencies/pom.xml`: managed `dev.openfeature.contrib.providers:gcp` and the `com.google.cloud` client coordinates.
- First starter whose `initialize()` requires credentials: the module's unit tests must be provably credential-free (CI has no ADC), and the integration tier must self-skip on a GCP-credential-less host exactly like upstream's — a `Skipped` there is by design, unlike the Unleash gate's promoted-to-failure finding, because the subject here is Spring wiring, not the provider's gRPC correctness.
- Lockstep watch: confirm the resolved `dev.openfeature.sdk` stays at the pinned `sdk.version` (`1.22.1`) rather than a contrib-parent-range (`[1.0,2.0)`) drift (see `design.md`).
- Out of scope, recorded so it is not mistaken for an omission: emulating Secret Manager via the GAPIC in-process emulators; upstream itself does not use them and the package-private client overloads make plain Mockito-on-the-client the upstream-native route.
