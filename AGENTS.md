# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

---
## Working Principles

### Simplicity First

**Minimum code that solves the problem. Nothing speculative.**
**(This is the principle we care about most.)**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes,
simplify.

### Subagent Delegation

Always delegate to specialized subagents rather than doing everything yourself. This is the primary productivity
multiplier.

### What to Read First

1. `.wiki/index.md` — wiki with all the knowledge you need to know
---

## Project Overview

**Spring Boot OpenFeature** is a Spring Boot starter that integrates [OpenFeature](https://openfeature.dev/) — an open standard for feature-flag management — into Spring Boot applications. It provides auto-configuration, a `Client` bean, a health indicator, a declarative `@ToggleOnFlag` AOP annotation, and per-provider starters so consumers can wire up a feature-flag backend with minimal configuration.

- **Language / Runtime:** Java 17
- **Framework:** Spring Boot 4.0.1
- **Core SDK:** OpenFeature Java SDK (`dev.openfeature.sdk`) 1.15.1
- **Build tool:** Maven (multi-module, uses the Maven wrapper `./mvnw`)
- **License:** Apache 2.0 (every source file carries the Apache license header)
- **Coordinates:** `org.iromu.openfeature:spring-boot-openfeature`, version managed by `${revision}` (currently `4.0.1-SNAPSHOT`)

## Module Architecture

This is a multi-module Maven reactor. The root `pom.xml` is a `pom`-packaging aggregator (not a parent of the examples, which use `spring-boot-starter-parent`).

- **`spring-openfeature`** — the core library. Contains the AOP layer (`@ToggleOnFlag` annotation + `ToggleOnFlagAspect`), the `OpenFeatureHealthIndicator`, `OpenFeatureProperties` (`spring.openfeature.*`), and the `spring-openfeature` auto-configurations (aspect + health). Depends on `spring-boot-starter-aspectj`, `spring-boot-starter-actuator`, and the OpenFeature `sdk`.
- **`spring-boot-openfeature-autoconfigure`** — generic auto-configuration. `ClientAutoConfiguration` creates the `dev.openfeature.sdk.Client` bean, conditioned on a `FeatureProvider` bean being present (`@ConditionalOnBean(FeatureProvider.class)`) and a `multiProvider` bean being absent. Also `OpenFeatureAPIAutoConfiguration` and a `security` sub-package. Exposes `ClientCustomizer` / `OpenFeatureAPICustomizer` extension points.
- **`spring-boot-starter-openfeature`** — the base starter (`pom` packaging). Pulls in autoconfigure + the dependencies BOM. This is the artifact consumers add to use provider-agnostic pieces.
- **`spring-boot-openfeature-dependencies`** — the BOM (`spring-boot-openfeature-dependencies`) that pins versions of the OpenFeature SDK and all provider libraries. Consumers import this via `dependencyManagement`.
- **Provider starters** — one module per feature-flag backend, each `spring-boot-starter-openfeature-<provider>`: `configcat`, `envvar`, `flagd`, `flagsmith`, `flipt`, `gofeatureflag`, `jsonlogic`, `multiprovider`, `statsig`, `unleash`, `growthbook`.
- **`aggregate-report`** — JaCoCo/Sonar aggregation (only built under the `sonar` profile).
- **`examples`** — runnable sample apps (only built under the `examples` profile). Has its own parent (`spring-boot-starter-parent`) and its own `${revision}`. Contains `unleash-simple` and `unleash-advanced`.

### The Provider-Starter Pattern

Each provider module follows the same structure — use the **Unleash** module as the reference implementation when adding or modifying a provider:

- `<Provider>Properties` — `@ConfigurationProperties(prefix = "...")` for provider config (e.g. `UnleashProperties`).
- `<Provider>Customizer` — an extension interface consumers implement to tweak provider config.
- `autoconfigure/<provider>/<Provider>AutoConfiguration` — `@AutoConfiguration` that:
  - `@AutoConfigureBefore(ClientAutoConfiguration.class)` so the provider bean exists before the `Client` is built,
  - `@ConditionalOnClass({<ProviderProvider>.class})` so it only activates when the provider lib is on the classpath,
  - `@ConditionalOnProperty(prefix = ..., name = "enabled", havingValue = "true", matchIfMissing = true)`,
  - `@EnableConfigurationProperties(<Provider>Properties.class)`,
  - declares `@Bean @ConditionalOnMissingBean` for the provider config and the `dev.openfeature.sdk.FeatureProvider` bean.

The flow: provider auto-config produces a `FeatureProvider` → `ClientAutoConfiguration` (conditioned on it) builds and exposes the `Client`.

### `@ToggleOnFlag` Annotation

Defined in `spring-openfeature` (`org.iromu.openfeature.boot.aop`). Applied to a method or class to conditionally run based on a flag:
- `key()` — the feature-flag key.
- `attributes()` — a SpEL map for flag context, e.g. `"{'userId': #id}"`.
- `orElse()` — name of a fallback method invoked when the flag is false.

## Building, Testing, Running

All commands use the Maven wrapper from the repo root.

```bash
# Full build (compile, checkstyle, format-check, tests, JaCoCo report)
./mvnw clean verify

# Skip tests
./mvnw clean verify -DskipTests

# Build a single module and its dependencies
./mvnw -pl spring-openfeature -am clean verify

# Run tests for one module
./mvnw -pl spring-boot-starter-openfeature-unleash test

# Build the example applications (separate reactor; needs the starter to be installed/published first)
cd examples && mvn clean package
```

- The `validate` phase auto-applies **Spring Java Format** (`spring-javaformat-maven-plugin:apply`) and runs **Checkstyle** (`maven-checkstyle-plugin`, config `io/spring/javaformat/checkstyle/checkstyle.xml`). Code is reformatted in place during the build.
- **JaCoCo** runs `prepare-agent` and produces coverage reports in the `verify` phase.
- **Profiles:** `-P sonar` (adds `aggregate-report`), `-P examples` (adds `examples`), `-P publish` (GPG signing + Central Portal publishing).
- CI (`.github/workflows/pull_request.yml`) runs `./mvnw -B --no-transfer-progress clean verify` on JDK 17 (Temurin).

## Development Conventions

- **Formatting:** Spring Java Format is enforced and auto-applied. Match the existing style — tabs for indentation in Java source, 4-space in POM/other files. Do not fight the formatter; let `validate` reformat.
- **Checkstyle:** runs against main sources (not test sources). Pre-commit also runs checkstyle (`checkstyle.xml` at repo root).
- **License headers:** every Java and XML file begins with the Apache 2.0 header. New files must include it.
- **Pre-commit hooks** (`.pre-commit-config.yaml`): checkstyle, `gitleaks` (secret scanning), `end-of-file-fixer`, `trailing-whitespace`. Keep files clean of trailing whitespace and ensure a trailing newline.
- **Lombok** is used for boilerplate (`@Data`, `@Slf4j`, etc.) and `spring-boot-configuration-processor` generates `spring-configuration-metadata`. Both are configured as annotation processors in provider/core modules.
- **Tests:** JUnit via `spring-boot-starter-test`; provider auto-config tests use `ApplicationContextRunner`-style assertions and `mockwebserver` (OkHttp) for HTTP-based providers. Keep the test source directory layout mirrored under `src/test/java`.
- **Adding a new provider:** replicate the Unleash module structure (properties, customizer, auto-configuration, `pom.xml`, auto-config registration via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, plus tests) and add the module + version property to the root `pom.xml` and the BOM.

## Key Files

- `pom.xml` — root aggregator: module list, version properties (SDK + each provider), build plugins, profiles.
- `spring-boot-openfeature-autoconfigure/.../ClientAutoConfiguration.java` — the central `Client` bean wiring.
- `spring-openfeature/.../aop/ToggleOnFlag.java` + `ToggleOnFlagAspect.java` — the declarative flag annotation and its aspect.
- `spring-boot-starter-openfeature-unleash/.../UnleashAutoConfiguration.java` — reference provider implementation.
- `examples/unleash-simple/` — minimal runnable example (controller, `application.yaml`, fake provider config).
- `.github/workflows/` — CI (build, snapshots, release, CodeQL, Scorecard, dependency review).
