---
title: "Project Wiki"
type: "index"
status: "active"
language: "default"
last_commit: "a0dc578aa4b3e811126d7ec3d9d7e7b1d9b298cd"
updated_at: "2026-09-05"
---

# Spring Boot OpenFeature — Wiki

## 1. What is this?

A **Spring Boot starter** that plugs [OpenFeature](https://openfeature.dev/) — an open standard for feature-flag management — into Spring Boot apps. It gives you a ready-made `Client` bean, a health indicator, a declarative `@ToggleOnFlag` annotation, and one starter module per flag backend (Unleash, ConfigCat, Flagsmith, flagd, Flipt, GrowthBook, Statsig, and more).

It is a Java 17 / Spring Boot 4 multi-module Maven library, not an application. See [[overview/what-is-this]] for the module map.

## 2. Get started

- **Main command:** `./mvnw clean verify` (builds all modules, runs checkstyle + tests).
- **Required environment:** Java 17, no external services for the library build. The runnable examples need a Unleash instance (or use the bundled fake/mock servers).
- **Expected output:** all modules `BUILD SUCCESS`; the `unleash-simple` example answers on port `9998` with flag-driven responses.
- **First files to read:**
  1. `spring-boot-openfeature-autoconfigure/.../ClientAutoConfiguration.java` — the central wiring
  2. `spring-boot-starter-openfeature-unleash/.../UnleashAutoConfiguration.java` — the reference provider pattern
  3. `spring-openfeature/.../aop/ToggleOnFlagAspect.java` — the annotation feature
- **Safe first change:** add a new provider starter by copying the Unleash module (see [[features/provider-starters]]).
- **Tempting dangerous change:** editing `ClientAutoConfiguration` conditions or the `@AutoConfigureBefore` ordering — breaking these silently disables flag evaluation (see [[risks/fragile-behavior]]).

## 3. Why does it exist?

Spring Boot apps want feature flags, but wiring the OpenFeature SDK by hand means managing provider lifecycle, event logging, evaluation context, and health checks per backend. This project makes that wiring automatic and provider-agnostic: add one starter dependency, set a few `spring.openfeature.*` properties, and flags work.

## 4. What happens when I run it?

Spring Boot auto-configuration kicks in (registered via `META-INF/spring/...AutoConfiguration.imports`):

1. The **provider auto-config** (e.g. `UnleashAutoConfiguration`) builds a `FeatureProvider` bean from `spring.openfeature.<provider>.*` properties.
2. `OpenFeatureAPIAutoConfiguration` exposes the OpenFeature SDK's global `OpenFeatureAPI` and installs event logging.
3. If Spring Security is on the classpath, `SecurityAutoConfiguration` adds a hook that injects `userId`/`authorities` into every flag evaluation.
4. `ClientAutoConfiguration` registers the provider (waiting for readiness) and exposes the `Client` bean.

The full sequence: [[flows/startup-flow]].

## 5. Where is data saved?

The library itself stores nothing. Flag data lives in the external provider (Unleash server, env vars, etc.). Optional local state: Unleash's `backupFile` property writes a local JSON cache of fetched flags.

## 6. What are the important moving parts?

- [[features/toggle-on-flag]] — `@ToggleOnFlag` AOP annotation for declarative flag-gated methods
- [[features/provider-starters]] — the per-backend starter modules and their shared pattern
- [[features/health-indicator]] — Actuator health check for the flag provider
- [[features/security-integration]] — automatic `userId`/`authorities` evaluation context
- [[features/multi-provider]] — combining several providers behind one client
- [[concepts/customizers]] — extension points (`ClientCustomizer`, `OpenFeatureAPICustomizer`, provider customizers)
- [[entities/configuration-properties]] — all `spring.openfeature.*` property prefixes

## 7. What should I avoid breaking?

- The **auto-configuration ordering** (`@AutoConfigureBefore`) — providers must exist before the `Client` bean is built.
- The **`OpenFeatureAPI` singleton** — it is global SDK state; the project intentionally funnels all mutations through auto-config.
- The **Unleash module as reference implementation** — new providers are expected to mirror it.
- Details: [[risks/fragile-behavior]].

## 8. Where do I look first?

| Question | Page |
| --- | --- |
| How does a provider become a `Client`? | [[flows/startup-flow]] |
| How does `@ToggleOnFlag` decide to run a method? | [[flows/flag-evaluation]] |
| How do I add a new provider? | [[features/provider-starters]] |
| What build commands and profiles exist? | [[reference/build-and-modules]] |
