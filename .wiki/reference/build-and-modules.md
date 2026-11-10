---
title: "Build and Modules"
type: "reference"
status: "active"
language: "default"
source_paths:
  - "pom.xml"
  - "examples/pom.xml"
updated_at: "2026-11-10"
---

# Build and Modules

Maven multi-module build (Java 17, Spring Boot 4). Use the wrapper: `./mvnw`.

## Key commands

```bash
./mvnw clean verify                  # full build: checkstyle, tests, JaCoCo
./mvnw -pl spring-openfeature -am clean verify   # one module + deps
./mvnw -P examples clean package     # build the example apps
```

## Profiles

| Profile | Adds |
| --- | --- |
| `sonar` | `aggregate-report` module (coverage aggregation) |
| `examples` | `examples` reactor (28 modules: `unleash-simple`, `unleash-advanced`, plus `<provider>-simple` for all other 26 providers) |
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

The `multiprovider-simple` example additionally pulls in `spring-boot-starter-openfeature-envvar` so the `MultiProvider` wraps two `FeatureProvider` beans.

## Quality gates

- `docs/dependency-versions.md` — version-audit table of every plugin/dependency version, regenerated from the POMs.
- Spring Java Format is applied automatically during `validate`; Checkstyle (Spring checkstyle ruleset) runs on main sources.
- JaCoCo coverage reports are generated in `verify`; pre-commit hooks run checkstyle, secret scanning, and whitespace fixes.
- CI builds on JDK 17 (Temurin) via `.github/workflows/`.
