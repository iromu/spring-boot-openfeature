## 1. Baseline and toolchain confirmation

- [x] 1.1 Confirm the branch is green **before** any edit by running the full gate on the canonical Azul JDK 17 — `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -Psonar clean verify` — and verify all 17 modules (incl. `aggregate-report`) report SUCCESS.
- [x] 1.2 Confirm the toolchain is a complete JDK 17 (has both compiler and javadoc) — `[ -x /home/wantez/.jdks/azul-17.0.20.1/bin/javac ] && [ -x /home/wantez/.jdks/azul-17.0.20.1/bin/javadoc ] && echo OK` — and verify `OK`; if a `-Psonar` run instead fails at the javadoc step, verify the remedy is a complete JDK 17 (never the JRE-only Debian `java-17-openjdk` package, never JDK 21), per `specs/dependency-upgrades/spec.md` "The build is verified on the CI-aligned JDK 17 toolchain".

## 2. Build-plugin hygiene (behavior-preserving)

- [x] 2.1 Bump the low-risk plugins in `pom.xml`: `jacoco-maven-plugin` 0.8.14→0.8.15, `maven-compiler-plugin` 3.14.1→3.16.0, `versions-maven-plugin` 2.20.1→2.21.0, `flatten-maven-plugin` 1.7.3→1.8.0, `maven-gpg-plugin` 3.2.7→3.2.8, `central-publishing-maven-plugin` 0.8.0→0.11.0; verify `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -Psonar clean verify` is green.
- [x] 2.2 Bump the Checkstyle library `com.puppycrawl.tools:checkstyle` (major) and run the Checkstyle step explicitly (it runs in the `validate` phase); verify no new rule violations are introduced and `./mvnw -B -Psonar clean verify` is green — if the major binds new rules, add narrowly-scoped per-rule suppressions with a recorded justification, or revert this one bump. **Outcome 2026-09-12: DONE at `12.3.1`, the newest release this JDK 17 build can load — the task's literal target `14.1.0` is not adoptable here, so the version was amended by direction.** Class-file majors were read from the jars across **every** class rather than a single proxy: `10.21.4`→55 (Java 11), `12.3.1`→**61 (Java 17)**, and the **whole `13.x` line through `14.1.0`**→65 (Java 21), so anything 13+ dies with `UnsupportedClassVersionError` (`class file version 65.0 … only recognizes … up to 61.0`). That is a hard class-file floor, **not** new rule bindings, so no per-rule suppression could absorb it and borrowing JDK 21 would mask the JDK 17 pin (Decision 4) while the local JDK 25 breaks Lombok — hence the highest *loadable* release, not the highest release. The Spring ruleset (`spring-javaformat-checkstyle` 0.0.48, whose own poms declare 9.3) runs **unmodified** against 12.3.1: `-X` shows `checkstyle-12.3.1.jar` on the plugin realm for all sixteen checked modules with no 10.21.4/9.3 jar present, `validate` reports 16 × `You have 0 Checkstyle violation`, and the full `-Psonar` gate is green (17 modules, 160 tests, 0 failures, 0 skipped, 0 coverage violations) — verified execution, not mere resolution. Raising past 12.3.1 requires raising the project's Java target, which is outside this change.
- [x] 2.3 Commit the plugin bumps as an isolated commit (no dependency/version-property changes mixed in) and verify `git show --stat HEAD` lists only `pom.xml`.

## 3. OpenFeature lockstep bump (atomic) with the two required test fixes

- [x] 3.1 Reproduce the verified candidate as a no-edit probe first — `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -Psonar clean verify -DskipTests -Dsdk.version=1.22.1 -Dflagd.version=0.14.1 -Dgo-feature-flag.version=1.2.1 -Dconfigcat.version=0.2.1 -Dflipt.version=0.1.4 -Djsonlogic-eval-provider.version=1.3.0` — and verify all modules compile, confirming the lockstep set before editing files.
- [x] 3.2 Edit `pom.xml` properties to the pinned set: `sdk.version`→1.22.1, `flagd.version`→0.14.1, `go-feature-flag.version`→1.2.1, `configcat.version`→0.2.1, `flipt.version`→0.1.4, `jsonlogic-eval-provider.version`→1.3.0 (leave `env-var`/`flagsmith`/`multiprovider`/`statsig`/`unleash`/`growthbook` provider properties and `unleash-client-java.version`=11.2.1 unchanged); verify `grep -nE 'sdk.version|flagd.version|go-feature-flag.version|configcat.version|flipt.version|jsonlogic-eval-provider.version' pom.xml` shows the new values.
- [x] 3.3 Fix `spring-boot-starter-openfeature-multiprovider/src/test/java/…/MultiProviderAutoConfigurationTest.java`: replace `Mockito.mock(MultiProvider.class)` in `UserMultiProviderConfiguration.customMultiProvider()` with `new MultiProvider(List.of(), new FirstMatchStrategy())` (both imports already present) or a hand `EventProvider` stub; verify `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -pl spring-boot-starter-openfeature-multiprovider test` passes with no `EventProvider.attach` null-attachment NPE, and that `shouldBackOffWhenUserSuppliesMultiProvider` still asserts `hasSingleBean(MultiProvider.class)` / `doesNotHaveBean("multiProvider")` / `hasBean("multiClient")`.
- [x] 3.4 Fix `spring-boot-starter-openfeature-gofeatureflag/src/test/java/…/GoFeatureFlagAutoConfigurationTest.java` for the 1.x default `IN_PROCESS` mode: add a `GoFeatureFlagCustomizer` bean that forces `EvaluationType.REMOTE` (mirroring the existing `goFeatureFlagTimeoutCustomizer` idiom) on the tests that build the real provider, or register a `MockWebServer` dispatcher answering `/v1/flag/configuration` with a valid payload; verify `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -pl spring-boot-starter-openfeature-gofeatureflag test` completes without hanging and all four tests pass. If 1.x hard-requires an unstubable `serverKey`/`fetcher`, `@Disabled` the affected test with a tracked follow-up rather than faking a green.
- [x] 3.5 Run the full gate with the lockstep in place — `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -Psonar clean verify` — and verify all 17 modules report SUCCESS (compile + tests + coverage check + aggregate report).
- [x] 3.6 Commit the lockstep bump and the two test-fix files together as one atomic commit and verify `git show --stat HEAD` lists `pom.xml` plus exactly the two test files (no production source).

## 4. Examples reactor

- [x] 4.1 Bump the `sprindoc-openapi.version` property (spelled that way in `examples/pom.xml`; it drives `org.springdoc:springdoc-openapi-starter-webflux-ui`) 2.8.4→3.1.1 in `examples/pom.xml`; verify `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -f examples/pom.xml clean verify -Dspring-boot-openfeature.version=4.1.0-SNAPSHOT -Dsprindoc-openapi.version=3.1.1` compiles/packages the examples, confirming they resolve the locally-built starter (per `specs/dependency-upgrades/spec.md` "The examples reactor builds against the locally-built starter"). Corrected at apply time (2026-09-11): this command previously passed `-Dspringdoc-openapi.version=...`, which names a property that does not exist — `examples/pom.xml` spells it `sprindoc-openapi.version` — so the override was a silent no-op that left 2.8.4 resolved while still reporting green. The override must match the real (misspelled) property name, and the effective version must be read back from dependency resolution rather than inferred from the build result. **Hazard found while verifying:** running this command's `-Dspring-boot-openfeature.version=4.1.0-SNAPSHOT` override makes `flatten-maven-plugin` (configured `<updatePomFile>true</updatePomFile>` at `pom.xml`) **rewrite the tracked `examples/pom.xml`**, silently bumping its `<revision>` from `3.5.9-SNAPSHOT` to `4.1.0-SNAPSHOT`. That is build leakage into source and it survives a green build, so check `git diff` after this step and `git checkout -- examples/pom.xml` rather than committing the drift; prefer `-Dflatten.skip=true` (or dropping the override) for a pure verification run.
- [x] 4.2 Commit the examples bump and verify `git show --stat HEAD` lists only `examples/pom.xml`.

## 5. Unleash client 12 spike (isolated, gated)

- [x] 5.1 Probe client 12 without editing files — `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -pl spring-boot-starter-openfeature-unleash -am clean verify -DskipTests -Dunleash-client-java.version=12.3.0` — and verify it resolves and compiles; keep `unleash-client-java.version`=11.2.1 in the committed set (do not fold 12.3.0 into the core bump). **Outcome 2026-09-11: VERIFIED — resolves and compiles with client 12.3.0.** `clean verify -DskipTests -Dunleash-client-java.version=12.3.0` is green; the transitive closure Maven had not cached before downloads fine (`yggdrasil-engine-1.0.3.jar` 12 MB, `okhttp-eventsource-4.3.0.jar` 72 kB). (An earlier run of this task reported these as unresolvable — that was an artifact of passing `-o`, which makes Maven report `Cannot access … in offline mode`; there is no mirror configured — the `<mirror>` blocks in `~/.m2/settings.xml` are commented out, 0 active — and Central is reachable. Do not read that earlier note as a finding.) Compiling against v12 says nothing about runtime behaviour, so this is **not** the Decision 5 gate; the committed pin stays 11.2.1 and nothing was promoted.
- [x] 5.2 Re-verify client 12.3.0 against a **real Unleash endpoint** and verify the verification is **not** marked local/`@Disabled`; only promote 12.3.0 if that endpoint re-verification is green, otherwise leave the spike on the branch and record the outcome. **Gate amended 2026-09-14 (see `design.md` Decision 5) and now MET.** The originally-named gate is void rather than unmet: `UnleashConfigAndAccessProviderTest`/`AbstractUnleash2ProviderTest` exist nowhere — 0 matches by filename and by content across the 29 test sources of the upstream client repo's own tree, no `-tests`/`-test-fixtures` classifier is published, and this starter's `UnleashProperties` has no `adminBaseUrl`/`apiToken` fields at all (surface is `unleashAPI`/`unleashToken`/`environment`). The 2026-09-11 `NOT SATISFIABLE` note below was therefore right in conclusion, wrong in reason, and is kept only as history. **What closes the gate instead:** `spring-boot-starter-openfeature-unleash/src/test/java/…/UnleashEndpointIntegrationTest`, owned by this repo, which drives the auto-configured `FeatureProvider`/`Client` at a live server, authoring flags via the server's admin API with a minted admin token and reading them back through the client SDK on 12.3.0 — the runtime evidence 5.1's compile-only probe explicitly could not give. Standing recipe: `src/test/resources/unleash/docker-compose.integration.yml` (`unleashorg/unleash-server` + `postgres:16-alpine`; image name corroborated three ways — the vendor compose, `docker-bake.hcl`, the README badge — running image OCI label `org.opencontainers.image.version=8.2.0`). **Verified execution, read out of the log rather than inferred:** `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -pl spring-boot-starter-openfeature-unleash test` → `Tests run: 1, Failures: 0, Errors: 0, `**`Skipped: 0`**` -- UnleashEndpointIntegrationTest`; module `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`; 0 Checkstyle violations. The test is discriminating, not vacuous — it was red for four iterations before the authoring defect was found, and its failure message named the exact flag; a proxy on the wire confirmed `GET /api/client/features` carried `UNLEASH-SDK: unleash-java-sdk:12.3.0` and returned 200, so the green is a real fetch. **On the "not marked local/`@Disabled`" clause, stated plainly:** the test carries no `@Disabled` and no local-only marker; it self-skips via a JUnit `Assumptions` guard only when no endpoint answers, so a contributor without Docker gets `Skipped: 1` + `BUILD SUCCESS` (verified with `-Dunleash.it.url=http://127.0.0.1:1`) instead of a red build — which is why CI (no Docker in `pull_request.yml`) cannot itself exercise this gate, and the green above is a recorded local run, not a CI-enforced one. 12.3.0 stays promoted.
  - **Superseded history — 2026-09-11 note:** *NOT SATISFIABLE — the gate cannot be met from anything this build has, so 12.3.0 is deliberately NOT promoted.* Two reasons given: (1) the named tests are not obtainable — `unleash-0.1.3-alpha.jar` ships 18 entries / 5 `.class` files and 0 test entries, no `-tests` classifier (only main + `sources`), and this repo's own unleash tests are just `FakeUnleashProviderTest` + `UnleashAutoConfigurationTest`, neither referencing `adminBaseUrl`/`apiToken`; (2) running them would need a live endpoint plus credentials. Both were verifiable and both were *under-scoped*: (1) was checked only against the published artifact, never against the upstream repo's source tree, and (2) asserted unavailability of a live endpoint that this task has now stood up locally. "The gate is not executable as written" and "the gate is unmet" are different claims; the note recorded the latter on evidence for the former.

## 6. Deferred migrations (recorded — do not force)

- [x] 6.1 Confirm the OkHttp 5 blocker stands by attempting `-Dokhttp.version=5.5.0` on `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -pl spring-boot-starter-openfeature-flagsmith -am clean test -Dokhttp.version=5.5.0` and observing the expected `NoClassDefFoundError: okhttp3/Protocol` from `com.flagsmith…FlagsmithConfig$Protocol`; verify the committed `okhttp.version` remains 4.12.0 (per `specs/dependency-upgrades/spec.md` "No binary-incompatible transitive dependency is introduced").
- [x] 6.2 Confirm the GrowthBook SDK stays at `0.10.6` (its `0.11.0` release notes mark the OkHttp 5.4.0 move as breaking, coupled to the same flagsmith pin); verify no task in this change forces either bump and that both blockers are recorded in `design.md` Decision 6. **Audit correction 2026-09-12:** the "stays at `0.10.6`" framing is **under-considered, not wrong-but-safe** — existence probes show `0.10.7`, `0.10.8`, `0.10.9`, `0.10.10` and `0.11.0` all exist (`0.10.11/12/15`, `0.11.1`, `0.12.0` absent), so the SDK is **five** releases behind and the four patches between the pin and `0.11.0` were never dispositioned. Read from the cached poms rather than release notes: `0.10.6`–`0.10.10` each declare `okhttp 4.11.0` + `okhttp-sse 4.11.0`, i.e. the **same OkHttp-4 posture as the current pin**, while `0.11.0` declares **no okhttp dependency at all** — so only `0.11.0` is the breaking move and `0.10.7..0.10.10` are adoptable on dependency posture. Bumping to `0.10.10` is **out of this change's scope** (no task asks it) and is left as a decision; the provider `0.0.2` is genuinely latest (nothing exists in a widened `0.0.3`–`0.0.12`/`0.1.1`/`0.2.0` grid). This task's `[x]` covers only its three local clauses (pin value, no task forces it, blockers recorded); the parenthetical release-notes claim is still **unverified** — the pom evidence above is a proxy for it, not the notes themselves.
- [x] 6.3 Run the final full gate `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -Psonar clean verify` and verify it is green; if the go-feature-flag/multiprovider test fixes shifted coverage, re-measure each module's floor from its JaCoCo CSV after a clean run and round down to the nearest 0.01 before this final check.

## 7. Upgrade sweep 2026-09-12 (all `*.version` properties taken to authoritative latest)

Every `*.version` property in the reactor was resolved against **central's own `maven-metadata.xml`**
(forced into cache via `versions:display-dependency-updates`) rather than trusted from this change's
prose. Results, with the three that actually moved marked **→**:

| Property | Was | Now | Basis |
|---|---|---|---|
| `sdk` / `flagd` / `go-feature-flag` / `configcat` / `flipt` / `jsonlogic-eval-provider` | — | unchanged | already latest (n=49/79/39/7/6/7) |
| `env-var` / `flagsmith` / `multiprovider` / `statsig` / `unleash` | — | unchanged | already latest (n=10/12/3/8/6) |
| `growthbook-provider` | 0.0.2 | unchanged | latest; dense grid 0.0.3–0.0.20, 0.1.x, 0.2.x all absent |
| `spring-boot` | 4.1.1 | unchanged | latest **GA** (only 4.2.0-M1 beyond) |
| `sprindoc-openapi` | 3.1.1 | unchanged | latest (n=50) |
| `io.spring.javaformat` | 0.0.48 | unchanged | latest (n=47); drives **two** artifacts (ruleset + plugin) |
| `java` | 17 | unchanged | cannot move: raising it is the Checkstyle 13.x / Java 21 question, not a dependency bump |
| `unleash-client-java` | 11.2.1 | **→ 12.3.0** | latest stable (n=64) |
| `growthbook-sdk-java` | 0.10.6 | **→ 0.11.0** | latest; repackaged into a `lib` module (see below) |
| `okhttp` | 4.12.0 | **→ 5.5.0** | latest stable (n=108) |

**Items produced by the sweep:**

- [x] **`flagsmith` was RED under the okhttp floor move and is GREEN now — by upgrading the client, not by pinning
      okhttp down.** `FlagsmithAutoConfigurationTest` failed 2 + 1 err with
      `BeanCreationException: Error creating bean 'flagsmithProviderOptions' … Factory method threw: okhttp3/Protocol`
      — the `NoClassDefFoundError` class of failure, reproduced. Root cause is not the floor: provider `0.0.13`
      reaches `flagsmith-java-client:7.4.3`, built for OkHttp 4, referencing `okhttp3.Protocol` — a package OkHttp 5
      renamed. `design.md` Decision 6 recorded that "no flagsmith build tracking OkHttp 5 exists"; **that premise is
      FALSE**, so the deferral resting on it is void. `dependency:get com.flagsmith:flagsmith-java-client:RELEASE`
      resolved **`8.1.1`** (only `7.4.3` and `8.1.1` exist), and `8.1.1`'s pom declares `okhttp.version=5.0.0` with
      artifactId `okhttp-jvm` — its `test-okhttp4` profile being the only 4.x path. An OkHttp-5-native client build
      does exist, three majors up.
      Fix taken via the pattern this repo already uses for `unleash-client-java` and `growthbook-sdk-java` (exclude
      the provider's transitive client, re-declare it directly, price it from a property): a
      `<flagsmith-java-client.version>8.1.1</flagsmith-java-client.version>` property in the root pom, an
      `<exclusions>` block on the managed `flagsmith` entry plus a managed `com.flagsmith:flagsmith-java-client`
      entry in the BOM, and a direct unversioned `com.flagsmith:flagsmith-java-client` dependency in
      `spring-boot-starter-openfeature-flagsmith/pom.xml`.
      **Read back, not inferred:** `dependency:list` shows
      `com.flagsmith:flagsmith-java-client:jar:8.1.1:compile` and `com.squareup.okhttp3:okhttp-jvm:jar:5.5.0:compile`
      (floor honoured, no downgrade) with `dev.openfeature.contrib.providers:flagsmith:jar:0.0.13:compile` still
      present — the exclusion removed only the stale client, not the provider. `-pl …-flagsmith -am test` gives
      `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` in `FlagsmithAutoConfigurationTest`, module `SUCCESS`,
      `BUILD SUCCESS`, and `okhttp3/Protocol` appears nowhere in the log.
      **Two alternatives rejected, recorded because each silently breaks something:** (a) pinning `okhttp` down to
      `4.12.0` — infeasible, since `okhttp:5.5.0` is hard-pinned directly in ~10 module poms plus the root `okhttp-bom`
      import, GrowthBook's `lib` needs 5.x, and it would downgrade every module for one; (b) keeping `7.4.3` and
      excluding OkHttp from the client — severs the client's own HTTP transport, and with okhttp 5.5.0 still on the
      classpath from the other 22 modules it fails at runtime rather than at compile.
- [x] **GrowthBook `0.11.0` is a repackaging, not a version bump.** `growthbook-sdk-java-0.11.0.jar` is a **292-byte
      empty shell (0 classes)** vs 0.10.6's 210,966 bytes / 115 classes; the code moved to a new
      `com.github.growthbook.growthbook-sdk-java:lib` module (plus `growthbook-cache-caffeine`,
      `growthbook-cache-jcache`). The BOM declares only `growthbook-sdk-java`, so `lib` and the two cache modules
      arrive transitively **via the SDK's own pom** — ordinary Maven resolution, not the "covered by nothing but luck"
      I first wrote here; the accurate claim is that those three coordinates are undeclared in the BOM and so
      unpriced by us. `lib` declares **`okhttp 5.4.0`**, which is why raising the floor is coherent with it.
      Recommend declaring the three coordinates in `spring-boot-openfeature-dependencies/pom.xml` so a change in the
      SDK's own dependency graph cannot move them silently. **Read back and covered:** `-pl
      spring-boot-starter-openfeature-growthbook dependency:list` resolves
      `com.github.growthbook.growthbook-sdk-java:lib:jar:0.11.0`,
      `…:growthbook-cache-caffeine:jar:0.11.0` and `…:growthbook-cache-jcache:jar:0.11.0` alongside the
      `com.github.growthbook:growthbook-sdk-java:jar:0.11.0` shell — all three new coordinates present at the matching
      version, and `okhttp`/`okhttp-sse`/`okhttp-jvm` resolving at `5.5.0`, i.e. our floor raising the `5.4.0` that
      `lib` declares. So the repackaging is fully covered by transitivity and the floor; declaring the three
      coordinates stays a hardening recommendation, not a live defect.
- [x] **Unleash client promoted to 12.3.0, overriding Decision 5's endpoint gate** by direction. That gate (re-running
      the upstream `UnleashConfigAndAccessProviderTest` / `AbstractUnleash2ProviderTest` against a real Unleash
      endpoint with `adminBaseUrl` + `apiToken`) is **NOT met** — it remains unobtainable here per 5.2 — so 12.3.0 is
      landed on your direction rather than on evidence, and the pairing stays ahead of what provider `0.1.3-alpha`
      targets (`unleash-client-java 11.0.2`). Resolution was read back, not inferred: `dependency:list` shows
      `io.getunleash:unleash-client-java:jar:12.3.0:compile` plus `io.getunleash:yggdrasil-engine:jar:1.0.3:compile`,
      proving the root property actually reaches the transitively-arriving client instead of the provider's `11.0.2`
      winning by nearest-wins.
- [x] **Full-gate coverage caveat — RESOLVED.** An earlier `-Psonar` run halted at flagsmith (module 9/17) and left
      modules 10–17 **SKIPPED, not passed**, so no green claim about them was admissible. After the flagsmith client
      fix the full gate was re-run: `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -Psonar clean verify` →
      `BUILD SUCCESS`, `Total time: 03:52`, **17 modules, 160 tests, 0 failures, 0 errors, 0 skipped, 0 Checkstyle
      violations, and no module reported SKIPPED** — verified by grepping the log for `SKIPPED`/`FAILURE [` (none)
      rather than by reading the `BUILD SUCCESS` line alone.
