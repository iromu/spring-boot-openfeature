## Context

Juspay's Superposition publishes an OpenFeature Java provider as part of its `superposition` multi-client SDK. The provider requires API credentials to connect to the Superposition service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Superposition credentials on the host.
- API credentials are explicit, tested properties.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — Credentials are property-sourced: `api-key`, `org-id`, `workspace-id` (all required).**
The vendor `SuperpositionProviderOptions` is built from an API token, an organization ID and a
workspace ID; the auto-config SHALL fail-fast if any of them is unset and the starter is enabled.
The endpoint (default `https://api.superposition.dev`) and the polling refresh timeout/interval
(defaults 10000/5000 ms) are optional.

**D3 — Test posture: real construction with fake credentials.**
The provider constructor is lazy — it builds the async client and the config-refresh jobs but
opens no connection; the refresh jobs only start in `initialize()`. So the contract tests
construct the real `SuperpositionOpenFeatureProvider` with fake credentials and never open a
network connection (Mixpanel/Octopus precedent). The native `libsuperposition_core` ships inside
the `io.juspay.superposition:bindings` jar and loads on this host.

**D4 — BOM prices the provider coordinate.**
Pinned at `superposition-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Resolved Questions

- **Maven coordinate / version:** `io.juspay.superposition:openfeature-provider:0.118.2`,
  verified on Maven Central from raw bytes
  (`https://repo1.maven.org/maven2/io/juspay/superposition/openfeature-provider/maven-metadata.xml`,
  HTTP 200, latest `0.118.2`). The provider class is
  `io.juspay.superposition.openfeature.SuperpositionOpenFeatureProvider`, built from
  `SuperpositionProviderOptions` (Lombok builder). The group `io.juspay.superposition` is NOT
  `com.juspay`; the Java client lives under `clients/java/` in the `juspay/superposition` repo
  and is published to Central via JReleaser.
- **Constructor behaviour:** lazy (no network I/O at construction; refresh jobs start in
  `initialize()`), confirmed by reading the vendor source. Real construction with fake
  credentials is therefore safe for the contract tier.
- **Lockstep:** the provider transitively declares `dev.openfeature:sdk:1.15.1`; the starter
  pins `dev.openfeature:sdk` to `${sdk.version}` (1.23.0) explicitly (Mixpanel/Confidence
  precedent).
