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

(none — resolved below.)

## Findings (task 1.1 / 1.2)

**Coordinate.** DevCycle publishes its OpenFeature integration inside the DevCycle Java
server SDK: `com.devcycle:java-server-sdk:2.10.0` (latest release on Maven Central).
There is no separate provider artifact. The OpenFeature provider class is
`com.devcycle.sdk.server.openfeature.DevCycleProvider` (constructor: `DevCycleProvider(IDevCycleClient)`);
it wraps the client rather than holding credentials itself.

**Client surface.** The cloud (server-side) client is
`com.devcycle.sdk.server.cloud.api.DevCycleCloudClient` with constructors
`DevCycleCloudClient(String sdkKey)` and `DevCycleCloudClient(String sdkKey, DevCycleCloudOptions options)`.
The key must start with `dvc_server` (or `server`) — the constructor rejects anything
else with `IllegalArgumentException`. `DevCycleCloudOptions` (Lombok `@Builder`) exposes:
`enableEdgeDB` (default `false`), `baseURLOverride`, `customLogger`, `restOptions`,
`hooks`. The starter binds `server-side-key` and `base-url-override`; everything else is
delegated to the `DevCycleCustomizer` (`DevCycleCloudOptions.Builder`).

**Eager vs lazy (1.2).** The client constructor is **lazy with respect to the network**:
it only builds an OkHttp client and a Retrofit service bound to the bucketing API
(`https://bucketing-api.devcycle.com/` unless `baseURLOverride` is set). No connection is
opened at construction time — HTTP calls happen per flag evaluation / event track,
synchronously on the calling thread. The cloud client runs **no background threads**
(no event delivery, no polling), so there is nothing to switch off for the
credential-free guarantee; `DevCycleProvider.initialize()` blocks at most 2s waiting
for `isInitialized()`, which the cloud client reports `true` immediately.

**Test posture (D3 refinement).** Because construction is network-free, the contract
tier goes beyond back-off: the happy-path test redirects the client to a local
`MockWebServer` via `base-url-override` and evaluates a real flag through the
auto-configured `Client` — proving the full bean wiring with zero DevCycle credentials
and zero network calls to DevCycle.
