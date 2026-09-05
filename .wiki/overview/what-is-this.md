---
title: "What Is This Project"
type: "overview"
status: "active"
language: "default"
source_paths:
  - "pom.xml"
  - "README.md"
updated_at: "2026-09-05"
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
| `spring-boot-starter-openfeature-<provider>` | One module per backend: `unleash`, `configcat`, `envvar`, `flagd`, `flagsmith`, `flipt`, `gofeatureflag`, `jsonlogic`, `multiprovider`, `statsig`, `growthbook` |
| `examples` (separate reactor, `-P examples`) | `unleash-simple` and `unleash-advanced` runnable apps |

## Who uses what

- Library consumers: `spring-boot-starter-openfeature` + one provider starter.
- The generic auto-config only activates when a `FeatureProvider` bean exists — that is what the provider starters produce. See [[features/provider-starters]] and [[flows/startup-flow]].
