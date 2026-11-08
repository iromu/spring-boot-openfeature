## Context

Split.io publishes `io.split.openfeature:split-openfeature-provider` as a standalone OpenFeature provider wrapping the Split Java SDK. The provider requires an authorization key to connect to the Split service and optionally accepts domain/connection configuration.

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

## Resolved Questions

**Coordinate and version (task 1.1).** The artifact on Maven Central is
`io.split.openfeature:split-openfeature-provider` (the proposal's `com.split:split-openfeature-provider-java`
does not exist; `split-openfeature-provider-java` is only the GitHub repo name and the pom's `<name>`).
Latest stable: **1.2.2**. The provider class is `io.split.openfeature.SplitProvider`, which implements
`dev.openfeature.sdk.FeatureProvider` and exposes two constructors: `SplitProvider(SplitClient)` and
`SplitProvider(String apiKey)`. The provider ships **no options/config class of its own** — its only
configuration surface is the Split SDK's `io.split.client.SplitClientConfig.Builder` (from
`io.split.client:java-client`, 4.18.2, a transitive dependency of the provider).

**Constructor eagerness (task 1.2).** Both constructors are **eager**: `SplitProvider(SplitClient)` calls
`client.blockUntilReady()`, and `SplitProvider(String)` goes through a process-wide static singleton
(`SplitModule`) that builds a `SplitFactory` via `SplitFactoryBuilder.build(apiKey, config)` and blocks
until ready (10s timeout). The starter therefore assembles a starter-owned `SplitProviderOptions`
(authorization key + `SplitClientConfig`) so the "assembled configuration carries the key" scenario is
testable without constructing the provider, and a `SplitCustomizer` exposes the `SplitClientConfig.Builder`.

**Credential-free happy path.** The Split SDK has a **localhost (file-based) mode**: passing the literal
authorization key `localhost` to `SplitFactoryBuilder.build` yields a `SplitFactoryImpl` that loads flags
from a local file (`SplitClientConfig.Builder#splitFile`) with no HTTP client and no network. This was
verified empirically (a local YAML split file round-trips a flag value with zero egress). The default-bean
test therefore exercises the *real* `splitProvider` bean (and the OpenFeature `Client`) through this mode,
while the back-off/disabled/option-assembly tests keep the provider unconstructed. Both postures keep the
suite credential-free and offline.

**SDK lockstep.** The provider transitively declares `dev.openfeature:sdk:1.18.2` at dependency depth 2,
which would beat the 1.23.0 pulled by `spring-openfeature` at depth 3. The BOM gains a
`dev.openfeature:sdk` row pinned to `${sdk.version}` so every starter resolves the same SDK.
