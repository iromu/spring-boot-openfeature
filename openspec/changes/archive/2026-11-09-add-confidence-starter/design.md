## Context

Spotify publishes `confidence-sdk-java` which includes an OpenFeature provider. The provider requires an API token to connect to the Confidence service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Confidence credentials on the host.
- The API token is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — API token is property-sourced: `spring.openfeature.confidence.api-token`.**
The auto-config SHALL fail-fast if the token is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The back-off pattern is sufficient; the provider constructor is lazy (bytecode-verified — no connection until the first RPC), so the default-bean test constructs the genuine provider credential-free.

**D4 — BOM prices the provider coordinate.**
Pinned at `confidence-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Feasibility Gate Findings (resolved 2026-11-09)

- **Coordinate:** `com.spotify.confidence:openfeature-provider` on Maven Central — latest release `0.7.0` (verified from the raw `maven-metadata.xml`, the `0.7.0` POM, and a successful jar download; `openfeature-provider-0.7.0.jar` contains `com.spotify.confidence.ConfidenceFeatureProvider`). The provider is published by Spotify as part of the `confidence-sdk-java` multi-module build (parent `com.spotify.confidence:confidence-sdk-java`).
- **API surface:** `ConfidenceFeatureProvider` implements `dev.openfeature.sdk.FeatureProvider`. Constructors: `(String clientSecret)` (default endpoint `edge-grpc.spotify.com:443`), `(String clientSecret, String host, int port)` (custom endpoint, plaintext channel), and `(Confidence.Builder)` (full tuning). The SDK's `Confidence.Builder` (built via the static `Confidence.builder(String clientSecret)`) exposes `flagResolverManagedChannel(String, int)`, `resolveDeadlineMs(int)`, `eventSenderDeadlineMs(int)`, `disableTelemetry(boolean)` — the customizer targets this builder.
- **Lazy vs eager:** lazy. Bytecode inspection (`javap -c`) shows the constructors only build a gRPC `ManagedChannel` (`ManagedChannelBuilder.forAddress(...).build()`), which opens no connection until the first RPC, and `ConfidenceFeatureProvider` does not override `initialize()`, so `setProviderAndWait` runs the SDK's no-op default. The contract tests therefore construct the genuine provider credential-free with no network traffic.
- **Lockstep:** the provider transitively pulls `dev.openfeature:sdk:1.6.1`; the module pom declares `dev.openfeature:sdk` at `${sdk.version}` explicitly (DevCycle/FeatBit precedent) so the starter stays in lockstep.
