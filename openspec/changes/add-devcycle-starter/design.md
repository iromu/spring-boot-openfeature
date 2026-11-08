## Context

DevCycle publishes a feature-flag SDK for Java that integrates with OpenFeature. The provider requires a server-side key to connect to the DevCycle service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero DevCycle credentials on the host.
- The server-side key is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.
- Supporting multiple DevCycle environments.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — Server-side key is property-sourced: `spring.openfeature.devcycle.server-side-key`.**
The key is the primary credential. The auto-config SHALL fail-fast if the key is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The DevCycle provider likely constructs an HTTP client. Unit tests must never trigger that. The back-off pattern is sufficient.

**D4 — BOM prices the provider coordinate.**
Pinned at `devcycle-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Open Questions

- Exact Maven coordinate and latest stable version of the DevCycle OpenFeature provider.
- Whether the provider's constructor is lazy or eagerly connects.
