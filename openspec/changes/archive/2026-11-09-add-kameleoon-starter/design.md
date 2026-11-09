## Context

Kameleoon publishes an OpenFeature Java provider requiring an API key to connect to the Kameleoon service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Kameleoon credentials on the host.
- The API key is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — Credentials are property-sourced.**
The auto-config SHALL fail-fast if the required credentials are unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The back-off pattern is sufficient; the provider likely opens a network connection on construction.

**D4 — BOM prices the provider coordinate.**
Pinned at `kameleoon-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Findings (feasibility gate + API surface)

- **Coordinate:** `com.kameleoon:kameleoon-openfeature-java:0.0.1` is on Maven Central (raw
  `.pom` fetched; the jar downloads, ~16 KB). Vendor-published by Kameleoon
  (`github.com/Kameleoon/openfeature-java`), GPL-3.0 licensed.
- **Lockstep:** the provider transitively declares `dev.openfeature:sdk:1.8.0`; the module
  declares `dev.openfeature:sdk:${sdk.version}` explicitly and the BOM pins it, so
  `dependency:list` reports `dev.openfeature:sdk:1.23.0` (lockstep holds).
- **API surface:** the provider's public constructor is
  `KameleoonProvider(String siteCode, KameleoonClientConfig config)`. There is no
  "API key" parameter — the first argument is the **site code**. The `KameleoonClientConfig`
  (built via `KameleoonClientConfig.Builder`) **requires both a `clientId` and a
  `clientSecret`**: its constructor throws `KameleoonException$ConfigCredentialsInvalid`
  ("Client ID is null or empty" / "Client secret is null or empty") if either is absent.
  The builder also exposes `environment`, `topLevelDomain`, `refreshInterval` (minutes),
  `sessionDuration` (minutes), `defaultTimeout` (millis), `trackingInterval` (millis) and
  `proxyHost`.
- **Deviation from the original property sketch:** the design originally sketched a single
  `spring.openfeature.kameleoon.api-key` property. The real API needs three credentials, so the
  properties are `site-code` (required), `client-id` (required) and `client-secret` (required),
  plus the optional `environment` / `top-level-domain` / `refresh-interval` / `session-duration` /
  `default-timeout` / `tracking-interval`. Any other `KameleoonClientConfig` option (e.g. a proxy
  host) goes through a `KameleoonCustomizer`.
- **Constructor is eager:** `KameleoonClientImpl` (built by `KameleoonClientFactory.create`)
  starts a `ScheduledExecutorService` and schedules a background configuration-refresh task at
  construction, so the provider opens a network connection on construction. The contract tests
  therefore construct the provider through a Mockito `mockConstruction` (the disabled, back-off
  and missing-credential tests never construct the real provider at all), keeping the suite
  credential-free and network-free.
