---
title: "Provider Starters"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/autoconfigure/unleash/UnleashAutoConfiguration.java"
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/unleash/UnleashProperties.java"
updated_at: "2026-09-05"
---

# Provider Starters

One Maven module per flag backend: `unleash`, `configcat`, `envvar`, `flagd`, `flagsmith`, `flipt`, `gofeatureflag`, `jsonlogic`, `statsig`, `growthbook`, plus `multiprovider` ([[features/multi-provider]]).

## The shared pattern

`UnleashAutoConfiguration` is the reference implementation. Each provider auto-config:

1. `@AutoConfigureBefore({ClientAutoConfiguration.class, MultiProviderAutoConfiguration})` — the provider bean must exist before the client is built ([[flows/startup-flow]]).
2. `@ConditionalOnClass(<Provider>.class)` — activates only when the provider library is on the classpath.
3. `@ConditionalOnProperty(prefix = ..., name = "enabled", matchIfMissing = true)` — opt-out via `spring.openfeature.<provider>.enabled=false`.
4. `@EnableConfigurationProperties(<Provider>Properties.class)` — binds `spring.openfeature.<provider>.*`.
5. Declares `@Bean @ConditionalOnMissingBean` for the provider's config object and the `FeatureProvider`.

## Provider-specific notes

- **Unleash:** `UnleashProperties` (`spring.openfeature.unleash.*`) maps to `UnleashConfig` — API URL, token (sent as `Authorization` header), app name, environment, optional `backupFile` local cache.
- **EnvVar:** reads flags from environment variables via `System.getenv`; exposes `EnvironmentGateway` and `EnvironmentKeyTransformer` beans so consumers can swap them. No customizer interface.
- Most others (configcat, flagd, flagsmith, flipt, gofeatureflag, jsonlogic, statsig, growthbook) follow the same shape with their own `Properties` + `Customizer`.

## Adding a new provider

Copy the Unleash module: properties, customizer, auto-configuration, `pom.xml`, the `AutoConfiguration.imports` registration, and `ApplicationContextRunner`-style tests. Then add the module + version property to the root `pom.xml` and the BOM.

Extension points: [[concepts/customizers]]. Config reference: [[entities/configuration-properties]].
