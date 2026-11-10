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
  - "spring-boot-starter-openfeature-kameleoon/src/main/java/org/iromu/openfeature/boot/kameleoon/KameleoonProperties.java"
  - "spring-boot-starter-openfeature-superposition/src/main/java/org/iromu/openfeature/boot/superposition/SuperpositionProperties.java"
updated_at: "2026-11-09"
updated_at: "2026-11-10"
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

## Confidence

`ConfidenceProperties` (`spring.openfeature.confidence.*`) — the Spotify Confidence service authenticates with a single API token; endpoint overrides are optional:

- `api-token` — required when enabled; the Confidence API token (client secret). Missing token fails fast with an `IllegalStateException` when the starter's own provider bean is created.
- `host` — optional; overrides the Confidence flag-resolver endpoint (defaults to the provider's built-in endpoint when unset).
- `port` — optional; the flag-resolver port used together with `host` (defaults to 443).

Everything else (resolver/event-sender deadlines, telemetry) goes through a `ConfidenceCustomizer` bean targeting the SDK's `Confidence.Builder` — see [[concepts/customizers]].

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

## Kameleoon

`KameleoonProperties` (`spring.openfeature.kameleoon.*`) — the vendor-published provider (`com.kameleoon:kameleoon-openfeature-java`) is constructed from a site code plus a `KameleoonClientConfig` that requires both a client ID and a client secret, so all three are required:

- `site-code` — required; the Kameleoon site code (passed as the provider's first constructor argument)
- `client-id` — required; the Kameleoon client ID
- `client-secret` — required; the Kameleoon client secret
- `environment` — optional; the target Kameleoon environment (default: production)
- `top-level-domain` — optional; top-level domain override for the Kameleoon endpoint
- `refresh-interval` — optional, in minutes; background configuration-refresh cadence
- `session-duration` — optional, in minutes; client session duration
- `default-timeout` — optional, in milliseconds; default request timeout
- `tracking-interval` — optional, in milliseconds; event-tracking cadence

A blank `site-code` fails fast with an `IllegalStateException` naming `spring.openfeature.kameleoon.site-code`; a missing `client-id` or `client-secret` is rejected by the vendor config builder (wrapped in an `IllegalStateException` naming the offending property) when the starter's own config bean is created. Any other `KameleoonClientConfig.Builder` option (e.g. a proxy host) goes through a `KameleoonCustomizer` bean — see [[concepts/customizers]].

## Octopus Deploy

`OctopusDeployProperties` (`spring.openfeature.octopusdeploy.*`) — the vendor-published provider (`com.octopus.openfeature:octopus-openfeature-provider`) identifies the calling application via a client identifier and product metadata:

- `client-identifier` — required; the Octopus Deploy API key (sent as `Authorization: Bearer <key>`)
- `product-name` — required; identifies the calling application in the `X-Octopus-Client` header
- `product-version` — optional; application build version for the `X-Octopus-Client` header
- `cache-duration` (default `1m`) — how often the provider re-checks for flag changes

Missing required values fail fast with an `IllegalStateException` naming the property path when the starter's own configuration bean is created. The provider constructor is lazy (no network I/O until `initialize()`). Any other `OctopusFeatureConfiguration` option goes through an `OctopusDeployCustomizer` bean — see [[concepts/customizers]].

## VWO

`VwoProperties` (`spring.openfeature.vwo.*`) — the vendor-published provider (`com.vwo.sdk:vwo-openfeature-provider-java`) authenticates to the VWO service with an account ID and an API key:

- `account-id` — required; the VWO account ID (must be numeric)
- `api-key` — required; the VWO API key (sdk key) used to initialize the VWO client

A missing or non-numeric `account-id` or a blank `api-key` fails fast with an `IllegalStateException` naming the property path when the starter's own provider bean is created (the provider would otherwise swallow the error and end up with an uninitialized client). Any other entry of the options map the `VWOProvider` is built from goes through a `VwoCustomizer` bean — see [[concepts/customizers]].

## Superposition

`SuperpositionProperties` (`spring.openfeature.superposition.*`) — the vendor-published provider (`io.juspay.superposition:openfeature-provider`) is built from an API token plus an organization and workspace ID, so all three are required:

- `api-key` — required; the Superposition API token (bound to the vendor options' `token`)
- `org-id` — required; the Superposition organization ID
- `workspace-id` — required; the Superposition workspace ID
- `endpoint` — optional; the Superposition API endpoint (default `https://api.superposition.dev`)
- `refresh-timeout-millis` — optional, in milliseconds; configuration-refresh timeout (default 10000)
- `refresh-interval-millis` — optional, in milliseconds; configuration-refresh polling interval (default 5000)

A missing `api-key`, `org-id` or `workspace-id` fails fast with an `IllegalStateException` naming the offending property when the starter's own options bean is created (the vendor options builder rejects null values with a bare `NullPointerException`, so the starter validates before building). Any other `SuperpositionProviderOptions.Builder` option (e.g. a fallback config or experimentation options) goes through a `SuperpositionCustomizer` bean — see [[concepts/customizers]].
## Ageris

`AgerisProperties` (`spring.openfeature.ageris.*`) — Ageris is a file-based platform with no API key: flags and configs are read from configuration files in a local directory and refreshed on a schedule:

- `application` — required; the name of the application, as the configuration file fetcher and the Ageris managers use it
- `path` — required; the base directory of the Ageris configuration files (the fetcher reads them from `<path>/<application>/`)
- `feature-flag-files` — required (at least one entry); names of the feature-flag configuration files (e.g. `featureflags.json`); boolean lookups are answered from these files
- `json-config-files` — optional; names of the JSON configuration files (e.g. `configs.json`); text, number and object lookups are answered from these files
- `targeting-key-type` (default `user`) — context type the targeting key of a caller is put under
- `refresh-interval` (default `10s`) — interval between two refreshes of the configuration files
- `first-load-timeout` (default `30s`) — how long provider initialization waits for the first attempt to load the configuration files

Missing required values fail fast with an `IllegalStateException` naming the property path when the starter's own configuration bean is created. The provider constructor is lazy (no I/O until `initialize()` starts the configuration loader). Any other `Ageris` option (policy bindings, metrics) goes through an `AgerisCustomizer` bean — see [[concepts/customizers]].
