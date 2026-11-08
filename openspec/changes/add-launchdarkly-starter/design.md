## Context

LaunchDarkly publishes `com.launchdarkly:openfeature-java-server` as a standalone OpenFeature provider. It wraps the LaunchDarkly Java SDK's `LDClientInterface` behind the `FeatureProvider` interface. The provider requires a service key (and optionally environment/segment settings) to connect to the LaunchDarkly service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero LaunchDarkly credentials on the host.
- The service key is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness (that is LaunchDarkly's contract).
- Supporting multiple LaunchDarkly environments in a single application.

## Decisions

**D1 — One module, single provider variant.**
Unlike GCP (two variants), LaunchDarkly has one provider class. The module is straightforward: properties → options → provider bean.

**D2 — Service key is property-sourced: `spring.openfeature.launchdarkly.service-key`.**
The key is the primary credential. Without it, the provider cannot initialize. The auto-config SHALL fail-fast with a clear message if the key is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier; the provider constructor is NOT called in unit tests.**
The LaunchDarkly provider likely constructs an `LDClient` which opens a streaming connection. Unit tests must never trigger that. The back-off pattern (user mock `FeatureProvider`) is sufficient for the contract tier; a happy-path construction test is only viable if the provider supports a dry-run or the constructor is lazy.

**D4 — BOM prices the provider coordinate.**
`com.launchdarkly:openfeature-java-server` is pinned at a `launchdarkly-openfeature.version` property. The Lockstep check asserts the resolved `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off posture avoids this in unit tests; a happy-path test is only added if the constructor is proven inert.
- [`0.x` provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Open Questions

- Exact Maven coordinate and latest stable version of `com.launchdarkly:openfeature-java-server`.
- Whether the provider's constructor is lazy or eagerly connects (determines whether a happy-path test is feasible).
