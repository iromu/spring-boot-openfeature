## Context

Featureflip publishes an OpenFeature Java provider requiring an API key to connect to the Featureflip service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Featureflip credentials on the host.
- The API key is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — API key is property-sourced: `spring.openfeature.featureflip.api-key`.**
The auto-config SHALL fail-fast if the key is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The back-off pattern is sufficient; the provider likely opens a network connection on construction.

**D4 — BOM prices the provider coordinate.**
Pinned at `featureflip-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Feasibility findings (task 1)

- **Coordinate:** `io.featureflip:featureflip-openfeature`, latest release `0.2.1`
  (also `0.1.0`, `0.1.1`, `0.2.0`). Verified present on Maven Central
  (`repo1.maven.org/maven2/io/featureflip/featureflip-openfeature/`) with a
  `maven-metadata.xml` listing the versions and resolvable jar + pom. Not listed in the
  OpenFeature `java-sdk-contrib` repo; published by Canopy Labs via Sonatype Central Portal.
- **Main class:** `io.featureflip.openfeature.FeatureflipProvider`, which extends
  `dev.openfeature.sdk.EventProvider` (which `implements FeatureProvider`), so it is
  assignable to the `FeatureProvider` bean type.
- **Constructors:** `FeatureflipProvider(String sdkKey)`,
  `FeatureflipProvider(String sdkKey, FeatureFlagConfig config)`, and
  `FeatureflipProvider(FeatureflipClient client)`. The starter uses the
  `(String, FeatureFlagConfig)` form.
- **Constructor is lazy (bytecode-verified):** `FeatureflipProvider(String)` delegates to
  `(String, FeatureFlagConfig)`, which only calls `EventProvider.<init>()`, allocates a
  plain `Object lifecycleLock`, and stores `sdkKey`, `config`, `ownsClient=true`. No
  `FeatureflipClient` is created and no I/O happens at construction. The client is created
  in `initialize(EvaluationContext)`, which `ClientAutoConfiguration` reaches through
  `OpenFeatureAPI.setProviderAndWait(...)`.
- **SDK surface:** the API key is the `sdkKey` constructor argument. Optional provider
  options live in `io.featureflip.client.FeatureFlagConfig` (built via
  `FeatureFlagConfig.builder()`): `baseUrl` (endpoint override), `connectTimeout`,
  `readTimeout`, `streaming`, `pollInterval`, `flushInterval`, `flushBatchSize`,
  `initTimeout`, `inspectors`, `wrapper`. The environment is encoded in the SDK key itself
  (there is no separate environment option), so the starter exposes `api-key` +
  `base-url`.
- **Lockstep (task 1.3):** the provider's published pom depends on
  `dev.openfeature:sdk:1.23.0`, which equals the repo's `${sdk.version}` (1.23.0). No
  transitive override of the SDK is expected; confirmed again by
  `dependency:list` after wiring (task 5).
- **Credential-free posture (task 4.2):** construction is safe (lazy), so the only network
  path is `initialize()` via `setProviderAndWait`. The "default bean" contract test stubs
  `OpenFeatureAPI.getInstance()` (the Datadog precedent) so the real provider is constructed
  but never initialized; the disabled and back-off tests never construct the provider.
