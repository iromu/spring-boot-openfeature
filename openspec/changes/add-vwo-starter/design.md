## Context

VWO publishes an OpenFeature Java provider requiring an account ID and API key to connect to the VWO service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero VWO credentials on the host.
- Account ID and API key are explicit, tested properties.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — Account ID and API key are property-sourced: `account-id`, `api-key`.**
The auto-config SHALL fail-fast if either is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The back-off pattern is sufficient; the provider likely opens a network connection on construction.

**D4 — BOM prices the provider coordinate.**
Pinned at `vwo-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Open Questions

- Exact Maven coordinate and latest stable version.
- Whether the provider's constructor is lazy or eagerly connects.
