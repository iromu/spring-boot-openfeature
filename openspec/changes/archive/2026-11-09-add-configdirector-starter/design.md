## Context

ConfigDirector publishes an OpenFeature Java provider requiring an API key to connect to the ConfigDirector service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero ConfigDirector credentials on the host.
- The API key is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — API key is property-sourced: `spring.openfeature.configdirector.api-key`.**
The auto-config SHALL fail-fast if the key is unset and the starter is enabled.

**D3 — Test posture: construct the real provider credential-free.**
The constructor is lazy (verified empirically: no I/O until OpenFeature calls `initialize()`), so the contract tier constructs the real provider with a fake API key; the one test that registers it with OpenFeature points the endpoint at a refused localhost URL and shuts the provider down afterwards.

**D4 — BOM prices the provider coordinate.**
Pinned at `configdirector-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [`initialize()` reaches the ConfigDirector endpoint] → D3 points the contract test at a refused localhost URL; the provider fails fast and `shutdown()` stops the background eventsource.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Feasibility findings (2026-11-10)

- **Coordinate (Maven Central):** `com.configdirector:configdirector-openfeature-server-provider`, latest stable `1.4.0`. Verified from raw bytes (`repo1.maven.org` directory listing, POM and jar); the vendor docs' `com.configdirector:openfeature-server-provider:1.9.0` lives only on the vendor repo `maven.configdirector.com` — the Central-published line stops at `1.4.0`, so this starter pins `1.4.0`.
- **API surface:** `com.configdirector.openfeature.ConfigDirectorProvider` extends the SDK's `EventProvider`; constructors `(String apiKey)` and `(String apiKey, Consumer<ClientOptions>)`. `ClientOptions` exposes `metadata(String,String)`, `connection(Consumer<ConnectionOptions.Builder>)`, `telemetry(...)` and `logger(...)`. `ConnectionOptions.Builder` exposes `mode(STREAMING|POLLING)`, `pollingInterval`, `timeout` and `url` (the endpoint).
- **Constructor posture: lazy.** Constructing the provider with a fake key completes in <200 ms with no network I/O. Network I/O starts only when OpenFeature calls `initialize()` (via `ClientAutoConfiguration`'s `setProviderAndWait`); with an unreachable endpoint initialization fails fast (~3 s) and `shutdown()` stops the background eventsource threads, so the JVM/test run terminates cleanly.
- **Transitive SDK:** the provider declares `dev.openfeature:sdk:1.22.1` — older than the repo's `sdk.version` — so the starter pins `dev.openfeature:sdk:${sdk.version}` (lockstep).
- **Broken transitive OkHttp on Maven:** the provider declares `com.squareup.okhttp3:okhttp:5.5.0`, OkHttp 5's Kotlin-multiplatform root artifact whose Central jar is empty (the JVM classes ship in `okhttp-jvm`; the vendor relies on Gradle module metadata). The starter therefore excludes the empty root and declares `okhttp-jvm` explicitly (version from the repo's `okhttp-bom` import).
- **Property surface:** `apiKey` (required, fail-fast) and `url` (optional endpoint); everything else (mode, polling interval, timeouts, metadata, telemetry) goes through `ConfigDirectorCustomizer`.
