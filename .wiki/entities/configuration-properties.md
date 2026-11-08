---
title: "Configuration Properties"
type: "entity"
status: "active"
language: "default"
source_paths:
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/properties/OpenFeatureProperties.java"
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/unleash/UnleashProperties.java"
updated_at: "2026-11-08"
---

# Configuration Properties

All configuration lives under the `spring.openfeature` prefix.

## Global

| Property | Default | Effect |
| --- | --- | --- |
| `spring.openfeature.enabled` | `true` | Global switch (`OpenFeatureProperties`) |

## Per-provider

Each provider starter binds its own sub-prefix: `spring.openfeature.unleash.*`, `spring.openfeature.configcat.*`, `spring.openfeature.flagd.*`, `spring.openfeature.gcp.*`, `spring.openfeature.ofrep.*`, `spring.openfeature.optimizely.*`, `spring.openfeature.launchdarkly.*`, and so on. Every provider has:

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
