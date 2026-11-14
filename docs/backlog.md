# Backlog — unimplemented items carried over from the retired OpenSpec workflow

The OpenSpec workflow is retired and `openspec/changes/` + `openspec/specs/` are deleted. Everything that was
implemented is covered by the other `docs/` pages; the three proposals below were **never implemented** and this
page is their only surviving home. Original creation dates are from each change's `.openspec.yaml`.

| Item | Created | Status |
| --- | --- | --- |
| [Featurevisor starter](#1-featurevisor-starter--blocked) | 2026-11-08 | BLOCKED — artifact not on Maven Central |
| [Hyphen AI starter](#2-hyphen-ai-starter--blocked) | 2026-11-08 | BLOCKED — artifact never published |
| [Unleash real-endpoint gate](#3-unleash-real-endpoint-gate--not-started) | 2026-09-14 | NOT STARTED |

Recorded commands for the Unleash item pin `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1` — substitute your own
JDK 17 home (the build requires JDK 17; see [quality-and-testing.md](quality-and-testing.md)).

---

## 1. Featurevisor starter — BLOCKED

**Intended shape** (the standard provider-starter pattern, see
[provider-starter-contract.md](provider-starter-contract.md); reference module
`spring-boot-starter-openfeature-unleash`):

- New module `spring-boot-starter-openfeature-featurevisor`:
  - `FeaturevisorProperties` — prefix `spring.openfeature.featurevisor`, `enabled=true`, `apiKey`. The auto-config
    SHALL fail-fast if the key is unset while the starter is enabled (design D2).
  - `FeaturevisorCustomizer` — invoked against the assembled configuration after binding.
  - `FeaturevisorAutoConfiguration` — family-standard gates, options bean, `FeatureProvider` bean produced ahead of
    `ClientAutoConfiguration`; registered in `META-INF/spring/…AutoConfiguration.imports`.
- Root `pom.xml`: `<featurevisor-openfeature.version>` property + `<module>` entry.
- `spring-boot-openfeature-dependencies/pom.xml`: managed row for the provider coordinate (design D4).
- Module carries the repo's per-module JaCoCo instruction floor.
- Contract spec (as written): single `FeatureProvider` + `Client` when enabled; provider library absent ⇒ no
  contribution, not a startup failure; `enabled=false` ⇒ nothing contributed; a consumer-supplied
  `FeatureProvider` wins (starter backs off entirely); unit tests pass on a host with **no Featurevisor
  credentials** and make no network calls to Featurevisor.
- Test posture (design D3): back-off for the contract tier — the provider likely opens a network connection on
  construction, so the tests must never construct it.

**Blocker evidence** (recorded 2026-11-09, task 1.1):

Coordinate resolved as `com.featurevisor:featurevisor-openfeature:5.0.0` — groupId is `com.featurevisor`, **not**
`io.featurevisor`; tags v0.0.1 … v5.0.0 at `github.com/featurevisor/featurevisor-java`; the provider is the
`featurevisor-openfeature` module, depending on `com.featurevisor:featurevisor-java` at the same version and on
`dev.openfeature:sdk` pinned by the vendor at `1.23.0`.

The artifact is **GitHub Packages only, auth-required, not on Maven Central**:

- Distributed exclusively to `https://maven.pkg.github.com/featurevisor/featurevisor-java`; GitHub Packages
  requires authentication for Maven downloads even for public repos (anonymous GET of `maven-metadata.xml`
  returns 401).
- `repo1.maven.org/maven2/com/featurevisor/` and `.../io/featurevisor/` → 404 (never published to Central under
  either group).
- Maven Central search index: 0 hits for `featurevisor` and for `g:io.featurevisor`.
- `dev/openfeature/contrib/providers/` on Central: no Featurevisor provider (13 contrib providers, none is
  Featurevisor).
- The vendor's own Java SDK docs (`featurevisor.com/docs/sdks/java`) instruct consumers to add the GitHub Packages
  repository **and** a `settings.xml` `<server>` entry with a GitHub token (`read:packages` scope).
- `distributionManagement` in the featurevisor-java pom points at GitHub Packages on every tag (v0.1.0 → v5.0.0).

Consequences: the dependency would be unresolvable by this repo's CI (no GitHub Packages credentials in the build
workflow) and by any consumer without a personal GitHub token in `~/.m2/settings.xml` — every other starter in the
fleet depends on Maven Central artifacts. The jar cannot be downloaded on the authoring host (no `GITHUB_TOKEN`, no
`~/.m2/settings.xml` server entry), so **the API surface is uninspectable** and the constructor-laziness question
(task 1.2) plus the D2/D3 decisions that depend on it could not be verified. The lockstep check (1.3) is moot until
the artifact resolves; the vendor pom does pin `dev.openfeature:sdk` to `1.23.0`, which matches this repo's
`sdk.version` at v5.0.0.

**Exit options (decision needed, as recorded):**

(a) defer until Featurevisor publishes to Maven Central (watch the repo's release process / the OpenFeature
ecosystem listing); (b) implement against the GitHub Packages artifact with a documented consumer-side
repository + token requirement and a CI secret — a deviation from the fleet convention requiring explicit
sign-off; (c) drop the change.

**Remaining task sequence** (1.1 done-and-blocked; everything else untouched):

1. **Feasibility gate first** — 1.2 resolve the artifact and determine whether the provider constructor is lazy or
   eagerly connects, recording it here; 1.3 prove lockstep:
   `./mvnw -B -pl spring-boot-starter-openfeature-featurevisor dependency:list` shows `dev.openfeature:sdk` =
   `${sdk.version}`. Both are blocked by 1.1, so re-run 1.1 (availability check) first.
2. **Build wiring** — 2.1 root pom version property + `<module>`, BOM managed row; 2.2 module pom mirroring the
   Unleash reference.
3. **Starter classes** — 3.1 `FeaturevisorProperties` + `FeaturevisorCustomizer`; 3.2 `FeaturevisorAutoConfiguration`
   and its `AutoConfiguration.imports` registration.
4. **Test tier** — 4.1 `FeaturevisorAutoConfigurationTest` contract tests (disabled, back-off, customizer, default
   bean); 4.2 prove the credential-free guarantee.
5. **Coverage floor and full gate** — 5.1 measure and set `jacoco.instruction.minimum`; 5.2
   `./mvnw -B -Psonar clean verify`.

---

## 2. Hyphen AI starter — BLOCKED

**Intended shape** — identical to the Featurevisor item above, with `hyphen` in place of `featurevisor`:
module `spring-boot-starter-openfeature-hyphen`; `HyphenProperties` (prefix `spring.openfeature.hyphen`,
`enabled=true`, `apiKey`, fail-fast when the key is unset and the starter is enabled); `HyphenCustomizer`;
`HyphenAutoConfiguration` producing the `FeatureProvider` bean ahead of `ClientAutoConfiguration`; root pom
`<module>` + `<hyphen-openfeature.version>`; BOM managed row; per-module JaCoCo floor; the same five contract
requirements (presence/ordering, declarative disable, consumer back-off, property + customizer binding,
credential-free unit tier).

**Blocker evidence** (recorded 2026-11-10, task 1.1, from raw bytes):

Coordinate resolved as `dev.hyphen:openfeature-provider-java:1.0.0`, from the **`provider` branch** of
`github.com/Hyphen/openfeature-provider-java` ("The official Hyphen Toggle OpenFeature Provider"); the `main`
branch holds only a `LICENSE`.

The artifact is **not on Maven Central and there is no evidence it has ever been published anywhere**:

- `repo1.maven.org/maven2/dev/hyphen/openfeature-provider-java/maven-metadata.xml` → HTTP 404; the `1.0.0` POM and
  JAR URLs → HTTP 404.
- Maven Central search index (`search.maven.org`): 0 hits for `g:"dev.hyphen"` and for `hyphen openfeature`; a
  plain `hyphen` query returns only unrelated artifacts (`org.daisy.libs:hyphen`, `at.hyphen:android-sdk-ui`, …).
- The vendor pom declares **no `distributionManagement`** and no GPG/release plugins; the repo has **no git tags**
  and **no CI** (no `.github/` on the `provider` branch) — so there is no release pipeline that could have
  published to Central or to GitHub Packages.
- GitHub Packages cannot be ruled out positively: an anonymous GET of
  `maven.pkg.github.com/hyphen/openfeature-provider-java/dev/hyphen/openfeature-provider-java/maven-metadata.xml`
  returns 401 (auth required even for public repos) and no `GITHUB_TOKEN` is available on the authoring host. But
  the absence of tags/CI/`distributionManagement` means nothing is expected to be there.

**API surface was inspected from the `provider` branch source instead of the jar:**
`dev.hyphen.openfeature.HyphenProvider` with constructor `(String publicKey, HyphenProviderOptions options)`;
`HyphenProviderOptions` is builder-style and requires `application` and `environment` (both
`Objects.requireNonNull`), with optional `horizonUrls`, `enableToggleUsage` (default `true`) and
`cacheTtlSeconds` (default `300`). The vendor pom pins `dev.openfeature:sdk:1.14.0` — nine minor versions behind
this repo's `sdk.version` (`1.23.0`) — so a lockstep pin would be required in the starter (see
[dependency-management.md](dependency-management.md)).

Task 1.2 (constructor laziness) is answerable from source only: **the constructor is eager** — it builds a
`HyphenClient(publicKey, options)` (OkHttp-based per the vendor pom) at construction time, so the D3 back-off test
posture would have been the right one. Task 1.3 (lockstep `dependency:list`) is moot until the artifact resolves.

**Exit options (decision needed, as recorded):**

(a) defer until Hyphen publishes the provider to Maven Central (watch the repo — it currently has no CI, no tags,
and no `distributionManagement`); (b) implement against a non-Central artifact (GitHub Packages + consumer-side
token + CI secret, or vendoring the provider source) — a deviation from the fleet convention requiring explicit
sign-off; (c) drop the change.

**Remaining task sequence** — the same five stages as the Featurevisor item, with `hyphen` names: feasibility gate
(re-check availability, then 1.2 confirm laziness against a real jar rather than branch source, 1.3 lockstep
`dependency:list` on `spring-boot-starter-openfeature-hyphen`) → build wiring (root pom property + module, BOM row,
module pom mirroring Unleash) → starter classes (`HyphenProperties`, `HyphenCustomizer`,
`HyphenAutoConfiguration` + `AutoConfiguration.imports`) → credential-free contract tests
(`HyphenAutoConfigurationTest`: disabled, back-off, customizer, default bean) + proving that guarantee → coverage
floor (`jacoco.instruction.minimum`) and full `./mvnw -B -Psonar clean verify`.

---

## 3. Unleash real-endpoint gate — NOT STARTED

**The defect.** `spring-boot-starter-openfeature-unleash/src/test/java/org/iromu/openfeature/boot/autoconfigure/unleash/UnleashEndpointIntegrationTest.java`
is the only evidence that the promoted Unleash client (`12.3.0` at design time) works against a real Unleash server,
but it opens with `Assumptions.assumeTrue(isEndpointUp(), …)`, so an absent endpoint yields `Skipped` — which Maven
does not treat as a failure. `.github/workflows/pull_request.yml` runs
`./mvnw -B --no-transfer-progress clean verify` on `ubuntu-latest` with JDK 17 Temurin and starts nothing, so in CI
the test reports `Skipped` and the build goes **green without ever exercising the gate**. A gate that nothing runs
cannot fail a PR, so a `12.x` regression in flag fetching or evaluation would merge silently.

Still true as of this writing: the `Assumptions.assumeTrue(isEndpointUp(), …)` guard is present in the file, CI
still starts nothing, and the root pom now pins `unleash-client-java` **12.3.2** — the version under test has moved
past the `12.3.0` the gate's evidence was produced against, without the gate ever running.

**Planned remedy** (decisions as recorded):

- **D1 — self-provisioning from inside the test with Testcontainers** (Postgres plus the Unleash server), replacing
  the externally started endpoint. Rejected: a CI-only `docker compose` step (leaves the silent skip for
  contributors and moves the reproducibility contract into the workflow file) and doing both (two paths to sync).
- **D2 — declare `@Container` fields explicitly with an explicit wait strategy**, not Spring Boot's
  `@ServiceConnection` / `@TestcontainersCompose` abstraction: the subject is the OpenFeature provider talking HTTP
  to an endpoint, not Spring's wiring of managed service connections. The compose file stays as the
  manual/diagnostic route.
- **D3 — pin each image by immutable digest, derived rather than transcribed:**
  `docker image inspect --format '{{index .RepoDigests 0}}' <image>`, referenced as `name@sha256:…`. Tag pinning
  (`:8.2.0`) was rejected because a tag is mutable and the vendor's documented usage is `:latest`. The pinned
  image's OCI label `org.opencontainers.image.version` must be re-read and asserted to report **`8.2.0`** — the
  version this gate's evidence was produced against — so a moved mutable tag cannot silently change what runs.
- **D4 — Testcontainers dependencies declared unversioned and priced by the Spring Boot BOM**
  (`spring-boot-dependencies` declares `testcontainers-bom` under the `testcontainers.version` property, and ships
  `spring-boot-testcontainers`), following the repo's existing unversioned-dependency-plus-property pattern rather
  than introducing a version literal. Use `org.testcontainers:testcontainers`, `…:junit-jupiter`,
  `…:testcontainers-postgresql`; resolved versions are read back with `dependency:list`, not inferred from a green
  build. A generic container for Postgres was rejected (re-implements the module's wait strategy and JDBC
  exposure).
- **D5 — remove the self-skip guard outright; absence of runtime is a failure.** The escape hatch becomes explicit
  and visible (`-Dtest=…` / skipping the module) rather than a default that reports success. Rejected:
  warn-and-continue, which reintroduces the unexecuted-but-green outcome this exists to remove.
- **D6 — bounded, generous startup deadline plus an explicit wait on the server's own health endpoint** (not a
  fixed sleep), generous enough that a cold image pull in CI is not a failure — treated as a correctness property,
  because a flaky gate is disabled within a week.
- **D7 — expect no functional edit to `pull_request.yml`**; enforcement comes from the test being unconditionally
  executable. What it may need is diagnosability: uploading endpoint/container logs as a build artifact on failure
  (an explicit Docker-availability assertion step is kept only if the failure mode without it proves too opaque).
- **Credential seeding must be asserted, not assumed:** provision fresh endpoint state per run and assert the admin
  credential is **accepted by the server** before any flag is authored, so all-or-nothing seeding cannot leave the
  gate credential-less and vacuously green. Keep the default resource-reaper cleanup; container reuse was rejected
  as reintroducing the shared-state hazard.
- **`docker-compose.integration.yml` is retained** at
  `spring-boot-starter-openfeature-unleash/src/test/resources/unleash/docker-compose.integration.yml` for
  manual/diagnostic use; its pinned-image and token-seeding notes become the source of truth for the pinning
  choice. Open question as recorded: keep it in sync automatically or declare it diagnostic-only in its header.
- **Proof tasks — the gate must be shown able to fail**, not just green: baseline the defect from a run
  (`Skipped: 1` **and** `BUILD SUCCESS` in the same log, else stop); invert one expected outcome and see
  `Failures: 1` naming that flag; point at a non-existent container daemon and see `BUILD FAILURE` with
  `Skipped: 0`; point at an image that cannot start and see the endpoint named as the cause with its own log
  output, textually distinguishable from a red assertion (infrastructure must never masquerade as a regression);
  run the PR job's own command verbatim and see `Skipped: 0` for the gate class.

**Contributor impact.** Local contributor builds and any IDE test run of this module then **require Docker**; a
Docker-less build of the module fails by design. Disclose it in `CONTRIBUTING.md` (or the module note) and state
plainly that the only bypass is an explicit, visible skip rather than a default.

**Recorded gotchas — do not rediscover these:**

- The module is **`org.testcontainers:testcontainers-postgresql`**, not `-postgres`:
  `org.testcontainers:testcontainers-postgres` returns `404` from Central, so the intuitive name surfaces only as
  a resolution failure.
- **Testcontainers `2.x` compatibility on the pinned JDK 17 is unverified** and must be proven *before* adopting the
  BOM-priced release: resolve it with `dependency:list`, then compile a throwaway reference and confirm it is
  *executed*, not merely resolvable. If the managed release carries a class-file floor above Java 17, pin the
  managed `testcontainers.version` property **down** to the newest release that loads on 17 — reading the class-file
  majors across *every* class of the candidate jars, not a single proxy — and **never** raise `java.version` to make
  it pass.
- The two silent-failure-shaped endpoint traps recorded in the (now also deleted) archived change
  `2026-09-15-upgrade-dependencies-2026-09/design.md` Decision 5, which constrain any provisioning design:
  (1) seeding admin tokens via `INIT_ADMIN_API_TOKENS` is all-or-nothing — `api-token-service.initApiTokens` returns
  early once `api_tokens` is non-empty ("Not creating initial API tokens because tokens exist"), so a token added
  after the first boot never exists; recreate with `docker compose down -v`. (2) the admin create endpoint
  **ignores the `environments` block it is handed**, so a created flag keeps `enabled=false` until
  `POST .../features/{name}/environments/{env}/on` is called for the environment the client token is scoped to;
  without it every evaluation falls back and reads as a client regression while being an authoring defect.
- The existing test needs a `UnleashCustomizer` that sets `synchronousFetchOnInitialisation(true)` and a short
  `fetchTogglesInterval`, because `UnleashProperties` declares `synchronousFetchOnInitialisation` but
  `UnleashAutoConfiguration` never forwards it to the client builder — otherwise the client only starts its 10s
  poller and the context closes before the first tick.
- Separate "could not obtain the image" from "the gate is red" in the reported failure, so a registry hiccup or a
  cold pull is not read as a regression.

**Out of scope, recorded so it is not mistaken for an omission:** CI still runs `clean verify` without `-P sonar`,
so the aggregate coverage report is not built in CI. Open question as recorded: whether CI should also run
`-P sonar`. Separately, the tasks' acceptance text says "all 17 modules `SUCCESS`" — that was the reactor size at
planning time; the root `pom.xml` now lists 33 modules, so re-measure against the current reactor.

---

## How to pick one up

1. **Re-verify the blocker first — it may have cleared.** For the two starters, re-check artifact availability
   before writing any code: fetch the Central `maven-metadata.xml` for
   `com.featurevisor:featurevisor-openfeature` / `dev.hyphen:openfeature-provider-java`, query the Central search
   index, and check the vendor repos for the tags / CI / `distributionManagement` whose absence was the blocker
   evidence. Also watch the OpenFeature ecosystem listing. If the artifact still does not resolve, the only moves
   are the recorded exit options (b) or (c) — and (b) is a deviation from the fleet's Maven-Central-only convention
   that needs explicit sign-off.
2. **Then implement**, not re-plan: follow [provider-starter-contract.md](provider-starter-contract.md) for the
   starter shape (copy `spring-boot-starter-openfeature-unleash` as the reference module),
   [dependency-management.md](dependency-management.md) for the BOM/version-property and OpenFeature SDK lockstep
   rules, and [quality-and-testing.md](quality-and-testing.md) for the test tiers, the JaCoCo instruction floor and
   the `-Psonar` full gate. For the Unleash item, work the proof tasks in order and do not mark a proof task done
   from a green run alone.
3. **Or re-create an OpenSpec change** if you want the artifact trail back: the `openspec` CLI is still provisioned
   by `setup.sh` (`npm install -g @fission-ai/openspec@1.16.0`) and `openspec/config.yaml` is still in the repo, so
   `openspec` remains a viable route alongside plain implementation. Whatever route is used, update or delete this
   page when the item lands, so it does not outlive the work.
