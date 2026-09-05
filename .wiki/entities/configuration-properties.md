---
title: "Configuration Properties"
type: "entity"
status: "active"
language: "default"
source_paths:
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/properties/OpenFeatureProperties.java"
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/unleash/UnleashProperties.java"
updated_at: "2026-09-05"
---

# Configuration Properties

All configuration lives under the `spring.openfeature` prefix.

## Global

| Property | Default | Effect |
| --- | --- | --- |
| `spring.openfeature.enabled` | `true` | Global switch (`OpenFeatureProperties`) |

## Per-provider

Each provider starter binds its own sub-prefix: `spring.openfeature.unleash.*`, `spring.openfeature.configcat.*`, `spring.openfeature.flagd.*`, and so on. Every provider has:

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

Settings not covered by properties should go through the provider customizer instead — see [[concepts/customizers]].
