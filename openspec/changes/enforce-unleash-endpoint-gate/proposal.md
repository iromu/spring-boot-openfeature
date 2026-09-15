## Why

`UnleashEndpointIntegrationTest` is the only evidence that the promoted Unleash client `12.3.0` works against a real Unleash endpoint, but it self-skips whenever no endpoint answers. `.github/workflows/pull_request.yml` runs `./mvnw -B --no-transfer-progress clean verify` and starts nothing, so in CI the test reports `Skipped` and the build goes green without ever exercising the gate. A gate that nothing runs is not a gate: it cannot fail a PR, so a `12.x` regression in flag fetching or evaluation would merge silently.

## What Changes

- The Unleash endpoint test provisions its own endpoint instead of depending on an externally started one, so it runs wherever Docker is present — including the existing `ubuntu-latest` PR job, with no CI-specific wiring and no manual developer step.
- **BREAKING** (for contributor builds): the "no endpoint ⇒ self-skip" guard is removed. An endpoint that cannot be started, or a red assertion, **fails the `verify` phase**. Builds on hosts without Docker will now fail rather than pass vacuously.
- The endpoint images are pinned to immutable references rather than `:latest`, so a mutable upstream tag or a registry hiccup cannot turn the gate red-or-green at random.
- The existing `docker-compose.integration.yml` stops being the only way to reach the gate; it remains for manual/diagnostic use.
- The Spring Boot-managed Testcontainers BOM is used to price the new test-scope dependencies, following this repo's existing unversioned-dependency-plus-property pattern rather than introducing a new version literal.

## Capabilities

### New Capabilities

(none — this is build/CI enforcement of an existing test, which `test-coverage` already governs via its "Build enforces a per-module coverage floor" requirement; adding a near-duplicate capability would fragment where build-enforced quality gates are specified.)

### Modified Capabilities

- `test-coverage`: adds requirements that (a) the PR build **executes** the real-endpoint Unleash gate and fails `verify` when it is red or cannot be started, and (b) the gate is **self-provisioning and reproducible**, with pinned immutable image references.

## Impact

- `spring-boot-starter-openfeature-unleash/src/test/java/org/iromu/openfeature/boot/autoconfigure/unleash/UnleashEndpointIntegrationTest.java` — self-provisioning endpoint, guard removed.
- `spring-boot-starter-openfeature-unleash/pom.xml` — new test-scope Testcontainers dependencies, unversioned, priced by the Spring Boot BOM (`testcontainers-bom`, managed via `testcontainers.version`). Verified present: `org.testcontainers:testcontainers`, `org.testcontainers:junit-jupiter`, `org.testcontainers:testcontainers-postgresql`. Note `org.testcontainers:testcontainers-postgres` returns **404** — the module is `…-postgresql`.
- `.github/workflows/pull_request.yml` — expected to need **no** functional change, since the gate becomes reachable from the ordinary `clean verify` run; it may need a Docker-availability assertion and a container-log upload for diagnosability.
- `spring-boot-starter-openfeature-unleash/src/test/resources/unleash/docker-compose.integration.yml` — retained for manual use; its pinned-image and token-seeding notes become the source of truth for the pinning choice.
- Local contributor builds and any IDE test run of this module now require Docker.
- Out of scope, recorded so it is not mistaken for an omission: CI still runs `clean verify` without `-P sonar`, so the aggregate coverage report is not built in CI (see `design.md`).
