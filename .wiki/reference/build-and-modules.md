---
title: "Build and Modules"
type: "reference"
status: "active"
language: "default"
source_paths:
  - "pom.xml"
  - "examples/pom.xml"
  - "Makefile"
  - "setup.sh"
  - ".pre-commit-config.yaml"
updated_at: "2026-11-14"
---

# Build and Modules

Maven multi-module build (Java 17, Spring Boot 4). Use the wrapper: `./mvnw`.

## Key commands

```bash
./mvnw clean verify                  # full build: checkstyle, tests, JaCoCo
./mvnw -pl spring-openfeature -am clean verify   # one module + deps
./mvnw -P examples clean package     # build the example apps
```

## Make targets

`Makefile` wraps the same Maven commands for agents and newcomers; `make help` lists
them. Requires JDK 17 — the machine default JDK may not be, and a wrong JDK breaks
Lombok.

| Target | Does |
| --- | --- |
| `build` / `build-skip-tests` / `compile` | `clean verify`, the same without tests, plain `compile` |
| `build-module` / `test-module` / `test-class` | `-pl $(MODULE) -am` variants; `test-class` also takes `TEST=<TestClass>` |
| `format` | `spring-javaformat:apply` in place |
| `lint` / `checkstyle` | `checkstyle:check@checkstyle-validation` — the suffix is what selects the build's Spring ruleset; see [[risks/fragile-behavior]] |
| `coverage` | `clean verify -P sonar` — builds the aggregate report |
| `install` / `clean` / `versions` | local install, remove artifacts, list dependency updates |

## First-time setup

`./setup.sh` is idempotent: installs the `pre-commit` CLI (pipx or pip3), reinstalls the
git hooks declared in `.pre-commit-config.yaml`, refreshes the project skills from
`skills-lock.json`, and installs the OpenSpec CLI if missing. Note that
`pre-commit install` refuses to run while `core.hooksPath` is set in git config.

Agent tooling is local scratch — `.agents`, `.claude` and `.qwen/*` are gitignored — with one
exception: [`.qwen/team-memory/`](../../.qwen/team-memory) is committed, because it carries the
conventions every contributor is expected to follow (build on JDK 17, verify with `-P sonar`,
keep the OpenFeature SDK and provider pins in lockstep). It stays tracked deliberately; ignore
`.qwen/*` rather than `.qwen`, or the whole directory silently drops out of the repository.

## Profiles

| Profile | Adds |
| --- | --- |
| `sonar` | `aggregate-report` module (coverage aggregation) |
| `examples` | `examples` reactor (30 modules: `unleash-simple`, `unleash-advanced`, plus `<provider>-simple` for all other 28 providers) |
| `publish` | GPG signing + Central Portal publishing |

## Examples

Every provider starter has a runnable `<provider>-simple` example under `examples/`. All share the same layout: a WebFlux app, a controller exercising `Client.getBooleanValue` + `@ToggleOnFlag` AOP, a fake in-memory `FeatureProvider` gated on `--example.fake-provider=true`, Swagger UI redirect, and port `9999`.

- `unleash-simple` / `unleash-advanced` — the original pair (port `9998`); advanced adds a mocked Unleash API server and native-image config.
- `gcp-simple`, `ofrep-simple`, `optimizely-simple` — the three contrib-backed starters added in PR #229.
- `configcat-simple`, `envvar-simple`, `flagd-simple`, `flagsmith-simple`, `flipt-simple`, `gofeatureflag-simple`, `growthbook-simple`, `jsonlogic-simple`, `multiprovider-simple`, `statsig-simple`, `datadog-simple` — the remaining eleven, completing full provider coverage (added 2026-11-08).
- `launchdarkly-simple` — the LaunchDarkly starter (vendor-published provider, added 2026-11-08).
- `split-simple` — the Split starter (vendor-published provider, added 2026-11-08).
- `devcycle-simple` — the DevCycle starter (vendor-published provider inside `com.devcycle:java-server-sdk`, added 2026-11-08).
- `featbit-simple` — the FeatBit starter (vendor-published provider, added 2026-11-09).
- `featureflip-simple` — the Featureflip starter (vendor-published provider, added 2026-11-09).
- `mixpanel-simple` — the Mixpanel starter (vendor-published provider, added 2026-11-09).
- `kameleoon-simple` — the Kameleoon starter (vendor-published provider, added 2026-11-09).
- `confidence-simple` — the Confidence starter (vendor-published provider, added 2026-11-09).
- `octopusdeploy-simple` — the Octopus Deploy starter (vendor-published provider, added 2026-11-09).
- `vwo-simple` — the VWO starter (vendor-published provider, added 2026-11-09).
- `superposition-simple` — the Superposition starter (vendor-published provider, added 2026-11-09).
- `ageris-simple` — the Ageris starter (vendor-published, file-based provider, added 2026-11-10).
- `cloudbees-simple` — the CloudBees starter (vendor-published provider, added 2026-11-10).
- `configdirector-simple` — the ConfigDirector starter (vendor-published provider, added 2026-11-10).

The `multiprovider-simple` example additionally pulls in `spring-boot-starter-openfeature-envvar` so the `MultiProvider` wraps two `FeatureProvider` beans.

## Quality gates

- [`docs/`](../../docs) holds the design documents — architecture, the provider starter
  contract, the quality/testing contract, dependency rules, and the backlog. See
  [[overview/design-docs]].
- `docs/dependency-versions.md` — version-audit table of every plugin/dependency version, regenerated from the POMs.
- Spring Java Format is applied automatically during `validate`; Checkstyle (Spring checkstyle ruleset) runs on main sources.
- JaCoCo coverage reports are generated in `verify`. Pre-commit (`.pre-commit-config.yaml`) runs gitleaks secret scanning, end-of-file and trailing-whitespace fixes, and a local `make lint` hook, so a commit is style-checked as well as a build; the standalone checkstyle hook was dropped in favour of that. What `make lint` invokes matters — see [[risks/fragile-behavior]].
- CI builds on JDK 17 (Temurin) via `.github/workflows/`.
