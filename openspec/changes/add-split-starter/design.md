## Context

Split.io publishes `com.split:split-openfeature-provider-java` as a standalone OpenFeature provider wrapping the Split Java SDK. The provider requires an authorization key to connect to the Split service and optionally accepts domain/connection configuration.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Split credentials on the host.
- The authorization key is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.
- Supporting multiple Split environments.

## Decisions

**D1 — One module, single provider variant.**
Split has one provider class. Straightforward properties → config → provider bean.

**D2 — Authorization key is property-sourced: `spring.openfeature.split.authorization-key`.**
The key is the primary credential. The auto-config SHALL fail-fast if the key is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The Split provider likely constructs a `SplitFactory` which opens a streaming connection. Unit tests must never trigger that. The back-off pattern is sufficient.

**D4 — BOM prices the provider coordinate.**
Pinned at `split-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this in unit tests.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Open Questions

- Exact Maven coordinate and latest stable version.
- Whether the provider's constructor is lazy or eagerly connects.
