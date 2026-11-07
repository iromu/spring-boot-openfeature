---
title: "Provider Starters"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/autoconfigure/unleash/UnleashAutoConfiguration.java"
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/unleash/UnleashProperties.java"
updated_at: "2026-11-07"
---

# Provider Starters

One Maven module per flag backend: `unleash`, `configcat`, `envvar`, `flagd`, `flagsmith`, `flipt`, `gofeatureflag`, `jsonlogic`, `statsig`, `growthbook`, `gcp`, `ofrep`, `optimizely`, plus `multiprovider` ([[features/multi-provider]]).

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
- **GCP:** `GcpProperties` (`spring.openfeature.gcp.*`) points at a Google Secret Manager secret holding a flag config; a fake provider backs the runnable `gcp-simple` example.
- **OFREP:** binds to an OpenFeature Remote Evaluation Protocol endpoint (`spring.openfeature.ofrep.*`). The provider has no public constructor — the starter builds it via the `constructProvider` factory, which validates the base URL.
- **Optimizely:** `OptimizelyProperties` (`spring.openfeature.optimizely.*`) assembles the vendor SDK's `Configuration` (datafile or sdk-key). The SDK's config parser imports `org.json` without declaring it, so the BOM manages `org.json:json` explicitly.
- Most others (configcat, flagd, flagsmith, flipt, gofeatureflag, jsonlogic, statsig, growthbook) follow the same shape with their own `Properties` + `Customizer`.

## Adding a new provider

Copy the Unleash module: properties, customizer, auto-configuration, `pom.xml`, the `AutoConfiguration.imports` registration, and `ApplicationContextRunner`-style tests. Then add the module + version property to the root `pom.xml` and the BOM.

Extension points: [[concepts/customizers]]. Config reference: [[entities/configuration-properties]].
