---
title: "Build and Modules"
type: "reference"
status: "active"
language: "default"
source_paths:
  - "pom.xml"
  - "examples/pom.xml"
updated_at: "2026-09-05"
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
| `examples` | `examples` reactor (`unleash-simple`, `unleash-advanced`) |
| `publish` | GPG signing + Central Portal publishing |

## Examples

- `examples/unleash-simple` — minimal app: Unleash provider via properties, a `Client`-injected controller, port `9998`.
- `examples/unleash-advanced` — adds `@ToggleOnFlag` usage, a mocked Unleash API server, and GraalVM native-image config for the Unleash yggdrasil engine.

## Quality gates

- Spring Java Format is applied automatically during `validate`; Checkstyle (Spring checkstyle ruleset) runs on main sources.
- JaCoCo coverage reports are generated in `verify`; pre-commit hooks run checkstyle, secret scanning, and whitespace fixes.
- CI builds on JDK 17 (Temurin) via `.github/workflows/`.
