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

## Findings (feasibility gate)

Resolved by fetching the raw Maven Central POM and inspecting the published sources
(`com.mixpanel:mixpanel-java-openfeature:0.2.0` + transitive `com.mixpanel:mixpanel-java:1.10.0`):

- **Coordinate / version:** `com.mixpanel:mixpanel-java-openfeature:0.2.0` on Maven Central
  (group id is `com.mixpanel`; published 2026-07-28, the latest release of three: 0.1.0,
  0.1.1, 0.2.0). The jar downloads cleanly from repo1.maven.org.
- **Provider class:** `com.mixpanel.openfeature.MixpanelProvider`, implements
  `dev.openfeature.sdk.FeatureProvider`. Constructors:
  - `MixpanelProvider(BaseFlagsProvider<?>)` — wraps a pre-built flags provider.
  - `MixpanelProvider(String token, LocalFlagsConfig config)` — local evaluation; **eager**:
    calls `localFlags.startPollingForDefinitions()` in the constructor (background polling
    thread).
  - `MixpanelProvider(String token, RemoteFlagsConfig config)` — remote evaluation; **lazy**:
    constructs a `MixpanelAPI` (endpoint strings + a `MessageBuilder`, no I/O) and a
    `RemoteFlagsProvider`; HTTP calls happen only per flag evaluation. The `token` parameter
    is unused by the SDK (the token is read from the config).
- **Chosen variant:** remote evaluation — `new MixpanelProvider(projectToken,
  RemoteFlagsConfig)`. D3's back-off is therefore unnecessary: the constructor is
  network-free, so the contract tests construct the real provider with a fake token and
  never open a connection.
- **Config type:** `com.mixpanel.mixpanelapi.featureflags.config.RemoteFlagsConfig`, built
  via `RemoteFlagsConfig.builder()` (`projectToken`, `apiHost` default `api.mixpanel.com`,
  `requestTimeoutSeconds` default 10, `exposureExecutor` default null).
- **Lockstep:** the provider POM depends on `dev.openfeature:sdk:1.20.1` (older than the
  repo's `sdk.version`), so the module declares `dev.openfeature:sdk` explicitly at
  `${sdk.version}` (DevCycle/FeatBit precedent).

## Open Questions

(none — resolved in Findings.)
