## Context

See `proposal.md` — Why. Design-relevant current state:

- `UnleashEndpointIntegrationTest` gates the client `12.3.0` promotion but begins with `Assumptions.assumeTrue(isEndpointUp(), …)`, so an absent endpoint yields `Skipped`, which Maven does not treat as a failure.
- The endpoint today comes from `src/test/resources/unleash/docker-compose.integration.yml` (`unleashorg/unleash-server` + `postgres:16-alpine`), started by hand.
- `.github/workflows/pull_request.yml` runs `./mvnw -B --no-transfer-progress clean verify` on `ubuntu-latest` with JDK 17 Temurin and starts nothing, so the gate is never executed in CI.
- Spring Boot `4.1.1` already prices Testcontainers: `spring-boot-dependencies` declares `testcontainers-bom` under the `testcontainers.version` property and ships `spring-boot-testcontainers`, so no new version literal is needed.
- Two hard-won operational facts about this endpoint, both silent-failure shaped, are recorded in `upgrade-dependencies-2026-09/design.md` Decision 5 and constrain any provisioning design: credential seeding is all-or-nothing once the token table is non-empty, and the admin create endpoint ignores the `environments` block, so a flag stays disabled until the per-environment enable call is made.

## Goals / Non-Goals

**Goals:**
- The gate executes in the ordinary PR build and can fail it.
- No manual step and no CI-specific wiring stands between a clean checkout and a green/red signal from the gate.
- The result is stable enough to be trusted as a merge signal.

**Non-Goals:**
- Does not extend the gate to other providers, and does not add endpoint gates for `flagd`, `gofeatureflag`, etc.
- Does not change what the gate asserts about flag evaluation; the assertions are already settled.
- Does not address CI not building the aggregate coverage report (it runs `clean verify` without `-P sonar`); tracked separately below.
- Does not raise `java.version`.

## Decisions

**D1 — Provision the endpoint from inside the test with Testcontainers.**
Chosen over a CI-only `docker compose` step and over doing both. A CI step would leave the gate skippable-by-default locally, which is the hole being closed, and would put the reproducibility contract in the workflow file rather than next to the test. Doing both adds a second path to keep in sync for no additional assurance. *Alternative rejected:* CI-only compose step (keeps the silent-skip hole for contributors); both (duplicate provisioning paths).

**D2 — Declare containers explicitly rather than through Spring Boot's service-connection abstraction.**
The subject under test is the OpenFeature provider talking HTTP to an endpoint, not Spring's wiring of managed service connections, so `@Container` fields with an explicit wait strategy keep the test honest about what it depends on and avoids importing an abstraction whose value (auto-configuring client beans for the endpoint) this test does not use. *Alternative considered:* `spring-boot-testcontainers` `@ServiceConnection`/`@TestcontainersCompose`, attractive for reusing the existing compose file verbatim, rejected as adding indirection between the test and the dependency it is verifying; the compose file stays as the manual/diagnostic route.

**D3 — Reference images by digest, derived rather than transcribed.**
Pin each image as `name@sha256:…`, taking the value from the image already pulled rather than copying it by hand:
`docker image inspect --format '{{index .RepoDigests 0}}' <image>`. The pulled server image's OCI label reports `org.opencontainers.image.version=8.2.0`, which is the version this change's evidence was produced against, so the pin must be the digest of that same image, and the label is re-checked after pinning to catch a mismatch. Tag pinning (`:8.2.0`) was rejected because a tag is mutable and because the vendor's documented usage is `:latest`, so a tag pin would still be the thing upstream moves; the mutable tag is also what makes a green run today not provably the same run tomorrow.

**D4 — Use the managed `postgresql` module, declared unversioned.**
Dependencies come from `org.testcontainers:testcontainers`, `…:junit-jupiter`, `…:testcontainers-postgresql`, unversioned and priced by the Spring Boot-managed BOM, matching this repo's existing pattern of letting the BOM price a dependency some pom requests; the resolved versions are then read back with `dependency:list` rather than inferred from a green build. Note the module is **`testcontainers-postgresql`**: `org.testcontainers:testcontainers-postgres` returns `404` from Central, so the intuitive name does not exist and would surface only as a resolution failure. *Alternative:* a generic container for Postgres, rejected as re-implementing what the module provides (correct wait strategy, JDBC exposure).

**D5 — Remove the self-skip guard outright; absence of runtime is a failure.**
Per the chosen failure policy, no endpoint means red. The escape hatch becomes explicit and visible (`-Dtest=…` / skipping the module) rather than a default that silently reports success, which is what makes the current state dangerous rather than merely weak. *Alternative rejected:* warn-and-continue when the endpoint cannot start — it reintroduces exactly the unexecuted-but-green outcome this change exists to remove.

**D6 — Give the gate a bounded, generous startup deadline and an explicit health wait.**
The endpoint needs Postgres plus migrations, so the wait targets the server's own health endpoint rather than a fixed sleep, and the deadline is generous enough that a cold image pull in CI is not a failure. A too-short deadline is the most likely way to make a real gate flaky, and a flaky gate is disabled within a week, so this is treated as a correctness property of the design rather than tuning.

**D7 — Expect no functional edit to `pull_request.yml`.**
Because the gate becomes reachable from the ordinary `clean verify`, the workflow needs no new step to enforce it. What it may need is failure diagnosability: uploading container/endpoint logs as a build artifact when the module fails, so a red PR is attributable without a local Docker reproduccion. *Alternative:* an explicit Docker-availability assertion step — kept only if the failure mode without it proves too opaque.

## Risks / Trade-offs

- **[Contributors without Docker now get a red build]** → This is the intended consequence, not collateral. It is disclosed in the module README/CONTRIBUTING note, and the only bypass is an explicit command-line skip that shows up in the diff and in the CI log rather than being a default.
- **[Testcontainers `2.x` may not run on the pinned JDK 17]** → Unverified as of writing; treat as an apply-time gate, not an assumption. If the BOM-priced release requires a newer runtime, pin the managed `testcontainers.version` property down to the newest release that loads on JDK 17 rather than raising `java.version`, exactly as the Checkstyle line was handled in `upgrade-dependencies-2026-09`. Distinguish a class-file floor from a rule/behaviour change before choosing.
- **[First CI run pulls the reaper and both images; latency or a registry hiccup looks like a broken gate]** → The reaper and both images are already cached locally, and CI caches layers; separate "could not obtain the image" from "the gate is red" in the reported failure so infrastructure never masquerades as a regression.
- **[Credential seeding silently no-ops on reused state]** → The all-or-nothing seeding rule means a leftover token table makes the admin credential absent while the test still appears to run. Provision fresh endpoint state per run and assert the credential is actually accepted before authoring, so this fails loudly (spec: reproducible provisioning).
- **[Digest pinning to an image that is not the one the evidence was produced on]** → Re-read the OCI version label after pinning and fail if it does not report `8.2.0`.
- **[Container lifecycle leaking between runs in CI]** → Keep the default resource-reaper cleanup rather than enabling container reuse; reuse would reintroduce the shared-state hazard above for a small time saving.

## Migration Plan

Land as one commit series: introduce the dependencies and the rewritten test, then flip the module's expectation from "may skip" to "must run", then the diagnosability touch in CI. Rollback is a single revert: the test returns to the compose-file recipe plus the self-skip guard, which remains in the tree, so no evidence or documented procedure is lost.

## Open Questions

- Whether CI should also run `-P sonar` so the aggregate coverage report is built there — genuinely separate, does not interact with this gate.
- Whether the manual compose file should be kept in sync automatically or declared as diagnostic-only in its header comment.
