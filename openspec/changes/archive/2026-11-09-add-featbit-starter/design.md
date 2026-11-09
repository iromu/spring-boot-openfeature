## Context

FeatBit publishes `co.featbit:featbit-openfeature-provider-java-server` (note the `co.featbit` group id, not `com.featbit`) as a standalone OpenFeature provider. The provider requires an app token (environment secret) to connect to the FeatBit service.

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

**D2 — Credentials are property-sourced under `spring.openfeature.featbit.*`.**
The app token is the primary credential; the per-environment `streaming-url` and `event-url` are
also required (see Findings). The auto-config SHALL fail-fast if any of `token`, `streaming-url`
or `event-url` is unset and the starter is enabled.

**D3 — Test posture: construct in offline mode, back off otherwise.**
The FeatBit provider's constructor is eager (always builds an `FBClient`), so the contract tests
that must expose a real provider construct it in offline mode (null synchronizer / null insight
processor — no connection, no blocking); the disabled, back-off and missing-credential tests never
construct it at all.

**D4 — BOM prices the provider coordinate.**
Pinned at `featbit-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Findings (feasibility gate)

Resolved by inspecting the published artifact (`javap` on the 1.0.0 jar + the transitive
`co.featbit:featbit-java-sdk:1.4.4`):

- **Coordinate / version:** `co.featbit:featbit-openfeature-provider-java-server:1.0.0` (the only
  published release; group id is `co.featbit`).
- **Provider class:** `co.featbit.openfeature.FBProvider`, extends `dev.openfeature.sdk.EventProvider`.
  Constructor: `FBProvider(String token, co.featbit.server.FBConfig config)`.
- **Config type:** `co.featbit.server.FBConfig`, built via `FBConfig.Builder`
  (`new FBConfig.Builder().offline(...).disableEvents(...).streamingURL(...).eventURL(...).startWaitTime(...).build()`).
- **Constructor is EAGER:** the `FBProvider` constructor always constructs an
  `FBClientImp`, whose constructor always calls `dataSynchronizer.start()` and spins up a
  `ScheduledThreadPoolExecutor`. It is NOT lazy.
- **Offline mode is network-free:** with `offline=true` the SDK skips the env-secret / URL
  validation, wires a `NullDataSynchronizer` (whose `start()` returns an already-completed
  `Future`, so no blocking and no connection) and a `NullInsightProcessor` (no-op event
  delivery). With `disable-events=true` event tracking is a no-op even when online.
- **Cleanup:** `FBProvider.shutdown()` → `FBClientImp.close()` shuts down the thread pool; Spring
  infers `shutdown` as the `@Bean` destroy method, so the context close tears it down.
- **Transitive SDK range:** the provider declares `dev.openfeature:sdk` as the open range
  `[1.7.0,2.0.0)` and `co.featbit:featbit-java-sdk` as `[1.4.0,2.0.0)`. Maven resolves the range to
  the highest available, so the module pins `dev.openfeature:sdk` at `${sdk.version}` to keep
  lockstep (proven by `dependency:list` → `dev.openfeature:sdk:1.23.0`).
- **`streamingURL` / `eventURL` are required, not optional:** the `FBConfig.Builder` sets no
  defaults, and the SDK's `BasicConfig` constructor NPEs (`StringUtils.stripEnd(...).concat(...)`)
  when either URL is null — even in offline mode. FeatBit's own offline test uses placeholder
  URLs (`ws://fake-url` / `http://fake-url`). The endpoints are per-environment (obtained from the
  FeatBit dashboard), so there is no safe production default to hardcode. The auto-config
  therefore treats `token`, `streaming-url` and `event-url` as required and fails fast with a clear
  message naming all three when any is unset.

## Test posture (resolved)

The back-off pattern alone would not satisfy the "clean context exposes one provider" scenario,
which requires the provider to actually be constructed. Because the constructor is eager, the
contract tests construct it in **offline mode** (`spring.openfeature.featbit.offline=true`,
`disable-events=true`) with a fake ASCII token — that path uses the null synchronizer / null
insight processor and opens no connection and blocks nothing. The disabled, back-off, and
missing-token tests never construct the real provider.
