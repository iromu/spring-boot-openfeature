## Context

Datadog publishes a feature-flag SDK for Java that integrates with OpenFeature. The provider requires an API key, application key, and site (e.g., `datadoghq.com`, `datadoghq.eu`) to connect to the Datadog service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Datadog credentials on the host.
- API key, application key, and site are explicit, tested properties.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.
- Supporting multiple Datadog sites/environments.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — Credentials are property-sourced: `api-key`, `app-key`, `site`.**
All three are required for the provider to function. The auto-config SHALL fail-fast if any are unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The Datadog provider likely constructs an HTTP client that connects to the Datadog API. Unit tests must never trigger that. The back-off pattern is sufficient.

**D4 — BOM prices the provider coordinate.**
Pinned at `datadog-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Feasibility findings (2026-11-08)

- **Coordinate:** `com.datadoghq:dd-openfeature` (part of the dd-trace-java project, Apache 2.0). Latest stable on Maven Central: **1.68.1** (verified from raw `maven-metadata.xml` bytes). It depends on `dev.openfeature:sdk:1.23.0`, which matches the pinned `sdk.version` — lockstep holds.
- **Provider class:** `datadog.trace.api.openfeature.Provider` (extends `dev.openfeature.sdk.EventProvider`). Public constructors: `Provider()` and `Provider(Provider.Options)`.
- **Options surface:** `Provider.Options` exposes only `initTimeout(long, TimeUnit)` (default 30s) and `enableEvaluationTelemetryOptOutParameter(boolean)`. There is NO programmatic credential surface (no api key / app key / site setters).
- **Credential path:** the provider is configured through the Datadog tracer settings: env vars `DD_API_KEY`, `DD_SITE`, `DD_ENV` (agentless delivery mode, `dd-java-agent` >= 1.66.0 must be loaded in the JVM). dd-trace's `ConfigProvider` reads system properties as well, so the starter maps `spring.openfeature.datadog.api-key` / `site` / `environment` to the `DD_API_KEY` / `DD_SITE` / `DD_ENV` system properties (only when not already set) so the bound properties actually reach the tracer.
- **Constructor eagerness:** LAZY. `javap -c` shows the constructor performs pure field setup (hooks, options, initialization state) with no network, no threads, and no credential access; `SpanEnrichmentGate.isEnabled()` swallows `Throwable` when the tracer bootstrap classes are absent. Confirmed by instantiating `new Provider()` and `new Provider(new Options())` on a credential-free host: both construct instantly and the JVM exits cleanly. Actual connection happens in `initialize()` (SDK-driven), which the unit tier never triggers. D3's back-off posture is therefore sufficient (the happy path is still testable in-process with dummy credentials since construction never touches the network).
