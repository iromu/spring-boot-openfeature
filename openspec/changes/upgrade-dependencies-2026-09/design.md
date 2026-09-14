## Context

See `proposal.md` (Why) for motivation and `specs/dependency-upgrades/spec.md` for the requirements; this document covers only the *how*.

Current baseline (verified): the reactor is green on the canonical Azul JDK 17 — `./mvnw -B -Psonar clean verify` passes all 17 modules including `aggregate-report`. Every "latest" below was confirmed against Maven Central `maven-metadata.xml` and the provider POMs directly (the `versions-maven-plugin` `display-property-updates`/`display-plugin-updates` goals under-report and were not relied on), filtered to the latest **stable** (beta tags excluded).

Two structural constraints shape the approach:

- **The SDK pin masks provider ranges.** `spring-openfeature/pom.xml` declares `dev.openfeature:sdk` at an explicit `${sdk.version}`. The contrib parent `openfeature-contrib:1.0.0` declares the SDK only as `<scope>provided</scope>` with range `[1.16.0,1.99999)`, and `flagd`/`go-feature-flag` POMs narrow that to `[1.21.0,1.99999)`. Because the explicit pin wins, the runtime SDK does not rise with the providers — so an SDK-only or provider-only move can compile yet throw `NoSuchMethodError`/NPE at runtime. They must move together.
- **Two "already latest" providers are not movable alone.** `flagsmith:0.0.13` pins `flagsmith-java-client:7.4.3` which pins OkHttp `4.12.0`; the root BOM `okhttp.version` override does not reach it and forcing 5.x fails at class-init (`NoClassDefFoundError: okhttp3/Protocol` from `com.flagsmith…FlagsmithConfig$Protocol`). This is the hard blocker on the OkHttp 5 line.

## Goals / Non-Goals

**Goals:**
- Land one coherent, CI-green baseline for the OpenFeature runtime set + build plugins.
- Keep the two upstream behavior changes (go-feature-flag default mode, multiprovider event attachment) covered by tests, not papered over.
- Make the JDK/verification posture unambiguous and reproducible.
- Record the blocked OkHttp 5 / GrowthBook 0.11 work with its exact blocker so it is not lost or force-applied.

**Non-Goals:**
- No production source changes (only the two test files). No new providers, no public API changes.
- Do not adopt the SDK 1.21+ isolated-`OpenFeatureAPI` idiom — the three auto-configs keep using the global `OpenFeatureAPI.getInstance()` singleton; that is an unrelated modernisation.
- Do not force the OkHttp 5 or GrowthBook 0.11 bumps in this change (deferred, blocked on upstream).

## Decisions

### 1. Move the OpenFeature set as one atomic lockstep commit
- **Choice:** Bump `sdk.version` and every provider `<*.version>` property together in a single commit, then run the full gate.
- **Why:** The shared compatibility window is `[1.16.0,1.99999)` (parent) narrowed to `[1.21.0,…)` by `flagd`/`go-feature-flag`; per the SDK-pin-masks-ranges constraint, partial moves risk uncaught runtime linkage errors.
- **Alternatives considered:** per-provider PRs (rejected — leaves a window where an old SDK runs under a provider built for a newer one); SDK-only bump (rejected — providers' declared ranges exclude it).

### 2. Pin the verified candidate set (single source of truth)
`dev.openfeature:sdk` `1.15.1→1.22.1`; `flagd` `0.11.15→0.14.1`; `go-feature-flag` `0.4.3→1.2.1`; `configcat` `0.1.0→0.2.1` (pulls `configcat-java-client:9.4.3`); `flipt` `0.1.1→0.1.4`; `jsonlogic` `1.2.1→1.3.0`. Already-latest and unchanged: `env-var:0.0.12`, `flagsmith:0.0.13`, `multiprovider:0.0.3`, `statsig:0.2.1`, `unleash` provider `0.1.3-alpha`, `growthbook` provider `0.0.2`. This exact set is verified to compile across all modules (`clean verify -DskipTests`, `-Psonar`, Azul 17).
- **Alternatives considered:** latest-of-everything including Unleash 12 + OkHttp 5 (rejected — pulls the two blocked migrations; see decisions 5 and 6).

### 3. Test fixes are part of the bump, not a follow-up
- **`MultiProviderAutoConfigurationTest`** — replace `Mockito.mock(MultiProvider.class)` with `new MultiProvider(List.of(), new FirstMatchStrategy())` (both already imported) or a hand `EventProvider` stub. Mockito bypasses the constructor, so under SDK ≥1.21 `EventProvider.attach` reads a null `attachment` `AtomicReference` and NPEs at `EventProvider.java:61`. **Trade-off:** a real instance exercises the constructor + `getProviderType()`/event path the mock stubs out; the strategy/provider args become trivially real but nothing behavioral is lost (the `shouldBackOffWhenUserSuppliesMultiProvider` test only needs a present bean).
- **`GoFeatureFlagAutoConfigurationTest`** — the bare `MockWebServer` never answers `/v1/flag/configuration`, so 1.x's default `IN_PROCESS` mode blocks startup. Prefer forcing `EvaluationType.REMOTE` via a `GoFeatureFlagCustomizer` bean (mirrors the existing `goFeatureFlagTimeoutCustomizer` idiom; keeps the suite offline; matches how the 0.4.x provider behaved) or stubbing the endpoint with a valid payload. **Trade-off:** REMOTE under-strips `options`/`payload` validation (a full WASM strip needs a real `serverKey`) — acceptable for a context/bean-wiring test; if the 1.x constructor still eagerly fetches regardless, seed a resolvable `serverKey` or fall back to `@Disabled` with a tracked issue.
- **Alternatives considered:** `@Disabled`/skip (rejected — hides the regression); pinning the old provider versions to avoid the change (rejected — defeats the lockstep requirement).

### 4. Verification posture = the canonical Azul JDK 17, not JDK 21
- **Choice:** `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -Psonar clean verify`.
- **Why:** `-Psonar` runs the aggregate coverage report + the javadoc-bearing `verify`; the project targets `java.version=17` and CI uses JDK 17. The Debian `java-17-openjdk-amd64` is a JRE (no `javac`/`javadoc`) → `verify` fails at the javadoc step. Borrowing JDK 21 (which has `javadoc`) compiles but is not the CI target and masks the JDK-17 pin.
- **Alternatives considered:** JDK 21 (rejected — not the CI target, and it papers over the missing-toolchain issue); the default JDK 25 (rejected — breaks Lombok annotation processing).

### 5. Unleash client 11→12 is an isolated spike, gated on provider re-verification
- **Evidence:** a jar diff of `unleash-client-java` 11.2.1 vs 12.3.0 shows the v12 breaking changes (`UnleashSubscriber` callbacks became default; `AbstractUnleash2Provider`'s okio `ResponseBody` callback closed; `OkHttpUnleash2Provider`'s executor must be caller-supplied; `UnleashProxyConfig` endpoints required) do **not** touch the symbols this stack uses (`DefaultUnleash`/`FakeUnleash` ctor, `isEnabled`/`getVariant`/`submit`), so the used surface is binary-compatible. But `unleash:0.1.3-alpha` POM targets client `11.0.2`, so running it on 12.3.0 is an *unsupported upstream pairing*.
- **Choice:** keep 11.2.1 in the core bump; test 12.3.0 behind a `-Dunleash-client-java.version=12.3.0` override as a separate spike. **Spike gate:** the upstream `UnleashConfigAndAccessProviderTest`/`AbstractUnleash2ProviderTest` must be re-run against a real Unleash endpoint (adminBaseUrl + apiToken required); it must not be marked local.
- **Gate amended 2026-09-14 — the original gate was void, not merely unmet.** Its premise does not exist: `UnleashConfigAndAccessProviderTest` and `AbstractUnleash2ProviderTest` appear **nowhere** — 0 matches by filename and by content across the 29 test sources of the upstream client repo's own tree, no `-tests`/`-test-fixtures` classifier is published for either artifact, and `UnleashProperties` in this starter has **no `adminBaseUrl`/`apiToken` fields at all** (its surface is `unleashAPI`/`unleashToken`/`environment`). So the gate was not executable as written, and 5.2's `NOT SATISFIABLE` note was right in conclusion but wrong in reason. It is replaced by a real endpoint test that this repo owns: **`UnleashEndpointIntegrationTest`**, which authors flags through the live server's admin API and reads them back through the auto-configured provider on client 12.3.0. Recipe: `src/test/resources/unleash/docker-compose.integration.yml`; green run recorded at 5.2.
- **Two traps the replacement had to get right, both silent-failure shaped:** (1) seeding admin tokens via `INIT_ADMIN_API_TOKENS` is all-or-nothing — `api-token-service.initApiTokens` returns early once `api_tokens` is non-empty ("Not creating initial API tokens because tokens exist"), so a token added after the first boot never exists; recreate with `docker compose down -v`. (2) the admin create endpoint **ignores the `environments` block it is handed**, so a created flag keeps `enabled=false` until `POST .../features/{name}/environments/{env}/on` is called for the environment the client token is scoped to; without it every evaluation falls back and reads as a client regression while being an authoring defect.
- **Alternatives considered:** bundle 12.3.0 into the lockstep (rejected — unsupported pairing + the streaming/fetching split means apps coding against `Unleash` must migrate; needs its own review).

### 6. OkHttp 5 and GrowthBook 0.11 are deferred with their blocker recorded
- **Choice:** hold `okhttp.version` at `4.12.0` and GrowthBook SDK at `0.10.6`; record the blocker (flagsmith's transitive `okhttp 4.12.0` via `flagsmith-java-client:7.4.3`; GrowthBook `0.11.0` release notes mark the OkHttp `5.4.0` move as a breaking change).
- **Why:** forcing `-Dokhttp.version=5.5.0` breaks flagsmith at class-init; a same-class upstream pin cannot be patched in-tree, so the fix belongs upstream.
- **Alternatives considered:** bump OkHttp + pin a newer flagsmith (blocked — no flagsmith build tracking OkHttp 5 exists).

### 7. Plugin bumps are behavior-preserving; treat the Checkstyle lib major as the only watch item
- **Choice:** bump JaCoCo/compiler/versions/flatten/gpg/central-publishing freely; take `com.puppycrawl.tools:checkstyle` `10.21.4→14.1.0` as a major — verify the build's Checkstyle invocation still passes and no new rules bind. The `maven-plugin-updates` "no updates found" result is not authoritative (it resolves from a possibly-staleness local cache; a metadata probe is required).
- **Alternatives considered:** defer the Checkstyle major (acceptable fallback if it introduces new rule violations).
- **Resolved 2026-09-12 — the real constraint is a class-file floor, not rule binding.** `14.1.0` cannot be adopted while this build targets JDK 17: reading class-file majors across **every** class in each jar, the whole `13.x` line through `14.1.0` is major 65 (Java 21) and fails with `UnsupportedClassVersionError`, while `12.3.1` is major 61 (Java 17) and `10.21.4` is major 55 (Java 11). No suppression can address a class-file floor, and running the gate on JDK 21 would mask the JDK 17 pin Decision 4 protects. So the engine moves `10.21.4 → 12.3.1` — the newest **loadable** release, not the newest release — and the Spring ruleset runs against it unmodified, verified by `-X` showing `checkstyle-12.3.1.jar` on the plugin realm for all sixteen checked modules plus 16 × `You have 0 Checkstyle violation` and a green `-Psonar` gate. Anything past 12.3.1 is gated on raising the project's Java target.

### 8. Examples reactor: springdoc 3.x + local-starter override
- **Choice:** bump the springdoc property `2.8.4→3.1.1` (the real property is `sprindoc-openapi.version`, and the artifact it drives is `springdoc-openapi-starter-webflux-ui`) and align `examples/pom.xml`'s `<revision>` to `4.1.0-SNAPSHOT` so the examples derive the starter version from their own `${revision}`.
- **Verified:** `./mvnw -f examples/pom.xml clean verify` with **no `-D` at all** builds all three modules as `4.1.0-SNAPSHOT`, and `dependency:list` confirms the effective resolved versions (`…unleash:4.1.0-SNAPSHOT`, `springdoc-openapi-starter-webflux-ui:3.1.1`). The earlier form of this command — `-Dspring-boot-openfeature.version=… -Dspringdoc-openapi.version=3.1.1` — was doubly defective: the second flag named a non-existent property and was a silent no-op, and the first drove `flatten` (`updatePomFile=true`) to rewrite the tracked `<revision>` in `examples/pom.xml` behind the build.
- **Note:** `examples/unleash-advanced/pom.xml` pins `com.squareup.okhttp3:mockwebserver:5.3.2` with an explicit `<version>`, so a root `-Dokhttp.version` override does not reach it — relevant only to the deferred OkHttp 5 work.

## Risks / Trade-offs

- **[Big-bang lockstep is a large single diff]** → it is one atomic, CI-verified commit; the candidate set is already compile-verified. If any module fails the gate, bisect within the set by reverting the offending provider property to its previous pin and re-running — do not revert the whole set.
- **[go-feature-flag 1.x is a major with in-process WASM evaluation]** → confirm no test relies on the old remote-only behavior; prefer forcing REMOTE mode. If 1.x hard-requires a `serverKey`/`fetcher` the stub cannot satisfy, `@Disabled` with a tracked follow-up rather than fake a green.
- **[multiprovider real instance changes what's under test]** → the back-off test only needs a present bean; the real `MultiProvider` initializes its `EventProvider` state so it can be attached. Verify context load and the `doesNotHaveBean("client")`/`hasBean("multiClient")` assertions still hold.
- **[Checkstyle 10→14 may bind new rules]** → run the Checkstyle step explicitly; if it introduces violations, either add narrowly-scoped suppressions (justified, per-rule) or defer the major.
- **[Unleash 12 unsupported pairing]** → the pairing was promoted to 12.3.0 by direction, ahead of what provider `0.1.3-alpha` targets (`11.0.2`). The residual risk is now carried by `UnleashEndpointIntegrationTest` against a live server rather than by the void upstream-test gate; the mitigation is real but local-only, since CI has no Docker.
- **[SNAPSHOT cross-reactor dependency]** → the examples resolve the starter from the `4.1.0-SNAPSHOT` local build via the override; do not publish/consume artifacts outside the reactor.

## Migration Plan

1. Branch off `chore/version_bump`; run the full `-Psonar` gate once to confirm the branch is green **before** any edit (baseline, Azul 17).
2. Commit the build-plugin bumps (decision 7) — behavior-preserving, isolated.
3. Commit the OpenFeature lockstep bump (decisions 1–2) **together with** the two test fixes (decision 3). Gate: full `./mvnw -B -Psonar clean verify` green; then targeted `-pl spring-boot-starter-openfeature-multiprovider test` and `-pl spring-boot-starter-openfeature-gofeatureflag test`.
4. Commit the examples bump (decision 8). Gate: examples reactor `clean verify` with the overrides.
5. Unleash 12 spike (decision 5) as its own commit, gated on `UnleashEndpointIntegrationTest` against a locally-run Unleash server (amended 2026-09-14); 12.3.0 is promoted.
6. OkHttp 5 / GrowthBook 0.11 (decision 6): no commit — recorded deferred with blocker.
- **Rollback:** each commit is a single property set; revert the offending `<*.version>` property (and, for the lockstep, its paired test-fix file) and re-run the gate.

## Open Questions

- The exact per-module JaCoCo floors are unaffected (this change adds no production code), but if the go-feature-flag/multiprovider test fixes shift coverage, re-measure via the JaCoCo CSV after a clean `-Psonar` run and round each floor down to the nearest 0.01. Deferrable to implementation; does not change the specs, approach, or task breakdown.
- ~~Whether to fold the Unleash 12 spike into this change's tip or leave it on the spike branch~~ — **resolved 2026-09-14:** folded, 12.3.0 promoted, gate met by `UnleashEndpointIntegrationTest`. One question remains genuinely open: the gate is only exercised where Docker exists, so a CI-side option (a scheduled job, or a Testcontainers-based variant) is still worth deciding.
