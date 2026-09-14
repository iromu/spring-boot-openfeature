## Why

The reactor's third-party versions have drifted behind upstream, and the drift is now load-bearing rather than cosmetic. The OpenFeature SDK is pinned at `1.15.1` while the contrib provider BOM (`dev.openfeature.contrib:openfeature-contrib:1.0.0`) and individual providers have moved to a generation that requires the SDK in the `[1.16.0,1.99999)` window — and `flagd`/`go-feature-flag` specifically require `[1.21.0,…)`. Because `spring-openfeature/pom.xml` pins the SDK explicitly, provider ranges do not raise the runtime SDK on their own, so the SDK and every provider must move together or risk an uncaught `NoSuchMethodError`/NPE at runtime (compilation will not catch a signature drift inside this window). Independently, two provider majors sit behind (configcat `0.1.0`→`0.2.1`, go-feature-flag `0.4.3`→`1.2.1`, the latter flipping the default evaluation mode to in-process WASM), the Unleash client is 11→12, and the build's JDK posture is ambiguous (the Debian `java-17-openjdk` package is a JRE with no `javac`/`javadoc`).

This change brings the dependency and build-plugin set to a single, coherent, CI-verifiable baseline and records the deliberately-deferred items so the upgrade can be landed and reviewed as one unit.

## What Changes

- **OpenFeature lockstep bump (one atomic change):** raise `sdk.version` `1.15.1`→`1.22.1` and move all contrib providers to their latest compatible release together — `flagd` `0.11.15`→`0.14.1`, `go-feature-flag` `0.4.3`→`1.2.1` (**BREAKING** — default `EvaluationType` becomes `IN_PROCESS`/WASM), `configcat` `0.1.0`→`0.2.1`, `flipt` `0.1.1`→`0.1.4`, `jsonlogic` `1.2.1`→`1.3.0`; `env-var`/`flagsmith`/`multiprovider`/`statsig`/`unleash`(provider)/`growthbook`(provider) are already at latest and stay. Rationale and the compatible set are pinned in `design.md`.
- **Two required test fixes ride with the lockstep bump** (both are runtime, not compile):
  - `MultiProviderAutoConfigurationTest` — stop `Mockito.mock(MultiProvider.class)`; under SDK `≥1.21` the mocked `EventProvider` skips its constructor so the `attachment` `AtomicReference` is null and `EventProvider.attach` NPEs. Build a real `new MultiProvider(List.of(), new FirstMatchStrategy())` (or an `EventProvider` stub) instead.
  - `GoFeatureFlagAutoConfigurationTest` — a bare `MockWebServer` never answers `/v1/flag/configuration`, so the 1.x default in-process provider hangs the build. Force `EvaluationType.REMOTE` via a `GoFeatureFlagCustomizer`, or stub the OFREP/WASM endpoints.
- **Build-plugin hygiene (behavior-preserving):** bump `jacoco-maven-plugin` `0.8.14`→`0.8.15`, `maven-compiler-plugin` `3.14.1`→`3.16.0`, `versions-maven-plugin` `2.20.1`→`2.21.0`, `flatten-maven-plugin` `1.7.3`→`1.8.0`, `maven-gpg-plugin` `3.2.7`→`3.2.8`, `central-publishing-maven-plugin` `0.8.0`→`0.11.0`, and the Checkstyle library `com.puppycrawl.tools:checkstyle` `10.21.4`→`14.1.0`.
- **Examples reactor:** bump `springdoc-openapi-starter` `2.8.4`→`3.1.1` (**BREAKING** major, the Boot-4 line) and add the `-Dspring-boot-openfeature.version=4.1.0-SNAPSHOT` override so the examples resolve the locally-built starter rather than a stale artifact.
- **Unleash client spike (optional, gated):** evaluate `io.getunleash:unleash-client-java` `11.2.1`→`12.3.0` as a separate, isolated spike; jar-diff evidence (in `design.md`) shows the API surface this project uses is binary-compatible, but provider `0.1.3-alpha` officially targets client `11.0.2`, so this is an unsupported pairing that must not be bundled with the core bump.
- **Deliberately deferred (documented, not executed here):** OkHttp `4.12.0`→`5.x` (blocked by the `flagsmith` provider pinning a `flagsmith-java-client:7.4.3` built for OkHttp 4 — forcing 5.x yields `NoClassDefFoundError: okhttp3/Protocol`), and the GrowthBook SDK `0.10.6`→`0.11.0` (its release notes mark the OkHttp `5.4.0` move as a breaking change). These are recorded as blocked-on-upstream tasks.

No production source changes; the only source edits are the two test files above.

## Capabilities

### New Capabilities
- `dependency-upgrades`: The project's dependency- and build-version-management contract — the OpenFeature SDK and all contrib providers move as one lockstep set within the shared compatibility window; the build is verified on the CI-aligned JDK 17 toolchain; provider auto-configuration contracts survive upstream upgrades; and the examples reactor resolves the locally-built starter.

### Modified Capabilities
<!-- No existing capabilities change requirements. `test-coverage` (the only existing capability) already owns the coverage-floor guarantee; this change adds no requirement to it and modifies none. -->

## Impact

- **Build config:** `pom.xml` — `<sdk.version>`, the per-provider `<*.version>` properties, and the `<plugins>` block versions. `spring-boot-openfeature-dependencies/pom.xml` — provider artifact versions (and the Unleash client override if the spike is taken).
- **Tests:** `spring-boot-starter-openfeature-multiprovider/src/test/…/MultiProviderAutoConfigurationTest.java` and `spring-boot-starter-openfeature-gofeatureflag/src/test/…/GoFeatureFlagAutoConfigurationTest.java` (the two required fixes).
- **Examples:** `examples/pom.xml` (`springdoc` version; `spring-boot-openfeature.version` override) and `examples/unleash-advanced/pom.xml` (explicitly-pinned `com.squareup.okhttp3:mockwebserver:5.3.2`, relevant only to the deferred OkHttp 5 work).
- **Verify gate:** full `./mvnw -B -Psonar clean verify` on the canonical Azul JDK 17 (`/home/wantez/.jdks/azul-17.0.20.1`), plus targeted `-pl` runs for the two fixed modules and the examples reactor.
- **No API/provider surface changes** in this change; the OkHttp 5 and GrowthBook 0.11 migrations are deferred with documented reasons and remain blocked on upstream.
