## Context

Spotify publishes `confidence-sdk-java` which includes an OpenFeature provider. The provider requires an API token to connect to the Confidence service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Confidence credentials on the host.
- The API token is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — API token is property-sourced: `spring.openfeature.confidence.api-token`.**
The auto-config SHALL fail-fast if the token is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The back-off pattern is sufficient; the provider likely opens a network connection on construction.

**D4 — BOM prices the provider coordinate.**
Pinned at `confidence-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Open Questions

- Exact Maven coordinate and latest stable version of Spotify's Confidence OpenFeature provider.
- Whether the provider's constructor is lazy or eagerly connects.
- Whether the SDK is available on Maven Central or requires a private repository.
