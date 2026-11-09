---
title: "Configuration Properties"
type: "entity"
status: "active"
language: "default"
source_paths:
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/properties/OpenFeatureProperties.java"
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/unleash/UnleashProperties.java"
  - "spring-boot-starter-openfeature-datadog/src/main/java/org/iromu/openfeature/boot/datadog/DatadogProperties.java"
updated_at: "2026-11-09"
---

# Configuration Properties

All configuration lives under the `spring.openfeature` prefix.

## Global

| Property | Default | Effect |
| --- | --- | --- |
| `spring.openfeature.enabled` | `true` | Global switch (`OpenFeatureProperties`) |

## Per-provider

Each provider starter binds its own sub-prefix: `spring.openfeature.unleash.*`, `spring.openfeature.configcat.*`, `spring.openfeature.flagd.*`, `spring.openfeature.gcp.*`, `spring.openfeature.ofrep.*`, `spring.openfeature.optimizely.*`, `spring.openfeature.launchdarkly.*`, `spring.openfeature.split.*`, and so on. Every provider has:

| Property | Default | Effect |
| --- | --- | --- |
| `<prefix>.enabled` | `true` | Disables that provider's auto-config when `false` |

## Unleash (reference)

`UnleashProperties` (`spring.openfeature.unleash.*`) maps to the Unleash client config:

- `unleash-api` (URI) — required, the Unleash API base URL
- `unleash-token` — sent as the `Authorization` header
- `app-name`, `environment` (default `default`), `instance-id`, `project-name`
- `backup-file` — optional local JSON cache file
- polling/metrics tuning: `fetch-toggles-interval`, `send-metrics-interval`, `disable-polling`, `disable-metrics`, timeouts, `synchronous-fetch-on-initialisation`

## LaunchDarkly

`LaunchDarklyProperties` (`spring.openfeature.launchdarkly.*`) — deliberately small, because the v7 `LDConfig.Builder` has no flat environment/streaming/polling knobs:

- `service-key` — required when enabled; the LaunchDarkly environment is encoded in the per-environment server-side SDK key
- `offline` (default `false`) — run the client in offline mode, no connection attempted

Everything else (streaming vs polling vs file data source, endpoint overrides) goes through a `LaunchDarklyCustomizer` bean — see [[concepts/customizers]].

## Datadog

`DatadogProperties` (`spring.openfeature.datadog.*`) — the provider (`datadog.trace.api.openfeature.Provider`) has no programmatic credential API, so credentials reach it through dd-trace settings. The starter publishes the bound values as system properties only when the corresponding environment variable or system property is absent (env/system always win):

- `api-key` — required; published as `DD_API_KEY`
- `app-key` — required (validated, not published)
- `site` — required; published as `DD_SITE`
- `environment` — published as `DD_ENV`
- `init-timeout` (default `30s`) — provider `Options.initTimeout`
- `evaluation-telemetry-opt-out` (default `false`) — provider `Options.enableEvaluationTelemetryOptOutParameter`

Missing required credentials fail fast with an `IllegalStateException` when the starter's own provider bean is created (not when a consumer supplies its own `FeatureProvider`).

Settings not covered by properties should go through the provider customizer instead — see [[concepts/customizers]].
