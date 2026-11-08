## Context

Mixpanel's `mixpanel-java` repository includes an `openfeature-provider` module. The provider requires a project token to connect to the Mixpanel feature-flag service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Mixpanel credentials on the host.
- The project token is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — Project token is property-sourced: `spring.openfeature.mixpanel.project-token`.**
The auto-config SHALL fail-fast if the token is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The back-off pattern is sufficient; the provider likely opens a network connection on construction.

**D4 — BOM prices the provider coordinate.**
Pinned at `mixpanel-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Open Questions

- Exact Maven coordinate and latest stable version of the Mixpanel OpenFeature provider module.
- Whether the provider's constructor is lazy or eagerly connects.
