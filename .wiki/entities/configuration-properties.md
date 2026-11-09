---
title: "Configuration Properties"
type: "entity"
status: "active"
language: "default"
source_paths:
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/properties/OpenFeatureProperties.java"
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/unleash/UnleashProperties.java"
  - "spring-boot-starter-openfeature-datadog/src/main/java/org/iromu/openfeature/boot/datadog/DatadogProperties.java"
  - "spring-boot-starter-openfeature-featbit/src/main/java/org/iromu/openfeature/boot/featbit/FeatBitProperties.java"
updated_at: "2026-11-09"
---

# Configuration Properties

All configuration lives under the `spring.openfeature` prefix.

## Global

| Property | Default | Effect |
| --- | --- | --- |
| `spring.openfeature.enabled` | `true` | Global switch (`OpenFeatureProperties`) |

## Per-provider

Each provider starter binds its own sub-prefix: `spring.openfeature.unleash.*`, `spring.openfeature.configcat.*`, `spring.openfeature.flagd.*`, `spring.openfeature.gcp.*`, `spring.openfeature.ofrep.*`, `spring.openfeature.optimizely.*`, `spring.openfeature.launchdarkly.*`, `spring.openfeature.split.*`, `spring.openfeature.devcycle.*`, and so on. Every provider has:

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

## Featureflip

`FeatureflipProperties` (`spring.openfeature.featureflip.*`) — deliberately small, because the target environment is encoded in the SDK key itself (there is no separate environment option):

- `api-key` — required when enabled; the Featureflip SDK key for the target environment. Missing key fails fast with an `IllegalStateException` when the starter's own provider bean is created.
- `base-url` — optional; overrides the Featureflip endpoint (self-hosted deployments).

Everything else (streaming, polling, timeouts) goes through a `FeatureflipCustomizer` bean — see [[concepts/customizers]].

Settings not covered by properties should go through the provider customizer instead — see [[concepts/customizers]].

## DevCycle

`DevCycleProperties` (`spring.openfeature.devcycle.*`) — the DevCycle cloud client is configured around a server-side key:

- `server-side-key` — required when enabled; must start with `dvc_server` (the SDK validates the prefix and fails fast)
- `base-url-override` — point the DevCycle bucketing API at a different host (defaults to the production API)

Everything else (custom logger, REST options, eval hooks, edge DB) goes through a `DevCycleCustomizer` bean wrapping the SDK's `DevCycleCloudOptions.Builder` — see [[concepts/customizers]].

## FeatBit

`FeatBitProperties` (`spring.openfeature.featbit.*`) — the vendor-published provider (`co.featbit:featbit-openfeature-provider-java-server`) needs the app token plus the per-environment endpoints, and its `FBConfig` builder has no defaults for the URLs (a null URL NPEs during provider construction), so all three are required:

- `token` — required; the FeatBit app token (environment secret)
- `streaming-url` — required; the environment's streaming (WebSocket) endpoint
- `event-url` — required; the environment's event (analytics) endpoint
- `offline` (default `false`) — run the FeatBit client in offline mode; no connection is attempted (null data-synchronizer + null insight processor)
- `disable-events` (default `false`) — disable FeatBit event delivery entirely

Missing required values fail fast with an `IllegalStateException` naming all three property paths when the starter's own provider bean is created. Any other `FBConfig` option goes through a `FeatBitCustomizer` bean — see [[concepts/customizers]].

## Mixpanel

`MixpanelProperties` (`spring.openfeature.mixpanel.*`) — the project is identified by its token; the starter wires the vendor-published provider (`com.mixpanel:mixpanel-java-openfeature`) in remote-evaluation mode:

- `project-token` — required when enabled; the Mixpanel project token. Missing token fails fast with an `IllegalStateException` when the starter's own provider bean is created.
- `api-host` — optional; the Mixpanel API host for your region (SDK default `api.mixpanel.com`).
- `request-timeout-seconds` — optional; HTTP request timeout in seconds for flag evaluations (SDK default 10).

Everything else (e.g. the exposure executor) goes through a `MixpanelCustomizer` bean wrapping the SDK's `RemoteFlagsConfig.Builder` — see [[concepts/customizers]].
