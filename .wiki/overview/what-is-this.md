---
title: "What Is This Project"
type: "overview"
status: "active"
language: "default"
source_paths:
  - "pom.xml"
  - "README.md"
  - "docs/architecture.md"
updated_at: "2026-11-14"
---

# What Is This Project

A Spring Boot starter library (Java 17, Spring Boot 4, Maven multi-module) that integrates the OpenFeature SDK. Consumers add one dependency and get feature flags with almost no code.

## Module map

The root `pom.xml` (aggregator, artifact `org.iromu.openfeature:spring-boot-openfeature`) lists these modules:

| Module | Role |
| --- | --- |
| `spring-openfeature` | Core library: `@ToggleOnFlag` annotation + aspect, health indicator, `OpenFeatureProperties` (`spring.openfeature.enabled`), aspect/health auto-configs |
| `spring-boot-openfeature-autoconfigure` | Generic auto-config: `ClientAutoConfiguration` (the `Client` bean), `OpenFeatureAPIAutoConfiguration` (SDK singleton + event logging), `SecurityAutoConfiguration` (auth-aware evaluation context) |
| `spring-boot-starter-openfeature` | Base starter (`pom` packaging) pulling in the generic auto-config |
| `spring-boot-openfeature-dependencies` | BOM pinning SDK + provider versions |
| `spring-boot-starter-openfeature-<provider>` | One module per backend: `unleash`, `configcat`, `envvar`, `flagd`, `flagsmith`, `flipt`, `gofeatureflag`, `jsonlogic`, `multiprovider`, `statsig`, `growthbook`, `gcp`, `ofrep`, `optimizely`, `launchdarkly`, `datadog`, `split`, `devcycle`, `featbit`, `featureflip`, `mixpanel`, `kameleoon`, `confidence`, `octopusdeploy`, `vwo`, `superposition`, `ageris`, `cloudbees`, `configdirector` |
| `examples` (separate reactor, `-P examples`) | 30 runnable apps: `unleash-simple`, `unleash-advanced`, and one `<provider>-simple` per remaining provider — full list in [[reference/build-and-modules]] |

## Who uses what

- Library consumers: `spring-boot-starter-openfeature` + one provider starter.
- The generic auto-config only activates when a `FeatureProvider` bean exists — that is what the provider starters produce. See [[features/provider-starters]] and [[flows/startup-flow]].

## Documentation

Every module ships its own `README.md`: the six core modules (`spring-openfeature`,
`spring-boot-openfeature-autoconfigure`, `spring-boot-starter-openfeature`,
`spring-boot-openfeature-dependencies`, `aggregate-report`, `examples`) and each
`spring-boot-starter-openfeature-<provider>` module. The root `README.md` links to the
per-provider READMEs; the core-module READMEs document the generic wiring, the BOM, the
`@ToggleOnFlag`/health features, coverage aggregation, and how to build and run the
[examples](../examples).

Beyond the READMEs, [`docs/`](../../docs) is the design home — the module map, the
provider starter contract, the quality/testing contract, the dependency rules, and the
backlog of unimplemented work. It replaced the OpenSpec planning workflow, whose trees
were deleted on 2026-11-14. See [[overview/design-docs]].
