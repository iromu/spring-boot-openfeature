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

(none — resolved below on 2026-11-08.)

## Findings (tasks 1.1–1.2, resolved 2026-11-08)

**Coordinate.** The Maven artifact is `com.launchdarkly:launchdarkly-openfeature-serverprovider` (latest stable **1.1.1**, published 2025-04-15). The proposal's `openfeature-java-server` is the GitHub repository name, not the artifact id. The provider's POM declares `dev.openfeature:sdk [1.13.0, 2.0.0)` and `com.launchdarkly:launchdarkly-java-server-sdk [7.1.0, 8.0.0)` — the repo's pinned `sdk.version` (1.23.0) is inside the range (lockstep check, task 1.3). Both provider dependencies are published in `runtime` scope (Gradle `implementation` publishing), so the module must declare `com.launchdarkly:launchdarkly-java-server-sdk` **explicitly** to compile against `LDConfig`.

**API surface** (via `javap` on 1.1.1):
- Provider class: `com.launchdarkly.openfeature.serverprovider.Provider` extends `dev.openfeature.sdk.EventProvider`.
- Constructors: `Provider(String sdkKey)`, `Provider(String sdkKey, LDConfig config)`, package-private `Provider(LDClientInterface)`.
- Config type: `com.launchdarkly.sdk.server.LDConfig`, built via `LDConfig.Builder`.

**Constructor eagerness.** `Provider(String, LDConfig)` is **eager**: it constructs `new LDClient(...)` immediately, which starts background data-source activity. D3's back-off posture therefore holds for the contract tier. A happy-path test is nevertheless feasible without any network: the v7 `LDConfig.Builder` accepts a file-based data source — `dataSource(FileData.dataSource().classpathResources(...))` — supplied through the starter's `LaunchDarklyCustomizer`, so the `LDClient` loads flags from a local file, reports the data source `VALID`/client `isInitialized()` synchronously, and `Provider.initialize()` completes without egress. The happy-path test uses exactly that (plus `diagnosticOptOut(true)`) with a fake service key. The default event-delivery data source would still POST evaluation events to the LaunchDarkly events endpoint even with a file data source (observed `HTTP 401 (invalid SDK key)` during the first test run), so the test also sets `events(Components.noEvents())` to keep the suite fully credential-free.

**Property surface (v7).** `LDConfig.Builder` has no `environment()`, `streaming()`, or `pollingIntervalMS()` setters: the environment is encoded in the per-environment server-side SDK key, and streaming vs. polling is a data-source component (`Components.streamingDataSource()` / `Components.pollingDataSource().pollingInterval(...)`) rather than a flat knob. Decision: the starter binds `service-key` and `offline` (`LDConfig.Builder.offline(boolean)`); data-source selection (streaming/polling/file) is delegated to the `LaunchDarklyCustomizer`, which is the v7-native extension point.
