## Context

Octopus Deploy publishes an OpenFeature Java provider for its feature-flag integration. The provider requires an API key and server URL to connect to the Octopus Deploy service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Octopus Deploy credentials on the host.
- API key and server URL are explicit, tested properties.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — API key and server URL are property-sourced: `api-key`, `server-url`.**
The auto-config SHALL fail-fast if either is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The back-off pattern is sufficient; the provider likely opens a network connection on construction.

**D4 — BOM prices the provider coordinate.**
Pinned at `octopusdeploy-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Findings (2026-11-09)

- **Coordinate:** `com.octopus.openfeature:octopus-openfeature-provider:2.1.0` (Maven Central, verified via raw POM).
- **SDK dependency:** The provider depends on `dev.openfeature:sdk:1.23.0`, which matches the repo's `sdk.version` (1.23.0) — no pinning needed.
- **Constructor behavior:** Lazy. `OctopusFeatureProvider(OctopusFeatureConfiguration)` only constructs the `FeatureFlagEvaluatorCache` and `FeatureFlagApiClient`; no network I/O. Network calls happen in `initialize()` (called by the OpenFeature SDK).
- **Configuration surface:** `OctopusFeatureConfiguration(String clientIdentifier, ProductMetadata productMetadata)`. The `clientIdentifier` is the API key (sent as Bearer token). `ProductMetadata(name, version)` identifies the calling app. `setServerUri` is package-private (not settable from the starter); the server defaults to `https://features.octopus.com`. `setCacheDuration` is public.
- **D2 adaptation:** The design originally specified `api-key` + `server-url`. The actual API uses `clientIdentifier` (not "api-key") and the server URL is not settable externally. The properties are therefore `client-identifier` + `product-name` (required) + `product-version` + `cache-duration` (optional).
