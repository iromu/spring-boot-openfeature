## Context

FeatBit publishes `com.featbit:featbit-openfeature-provider-java-server` as a standalone OpenFeature provider. The provider requires an app token to connect to the FeatBit service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero FeatBit credentials on the host.
- The app token is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.
- Supporting multiple FeatBit environments.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — App token is property-sourced: `spring.openfeature.featbit.token`.**
The token is the primary credential. The auto-config SHALL fail-fast if the token is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The FeatBit provider likely constructs an HTTP client. Unit tests must never trigger that. The back-off pattern is sufficient.

**D4 — BOM prices the provider coordinate.**
Pinned at `featbit-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Open Questions

- Exact Maven coordinate and latest stable version.
- Whether the provider's constructor is lazy or eagerly connects.
