## Context

See `proposal.md` — Why. Design-relevant current state, all read from the resolved artifact and upstream sources rather than assumed:

- `javap` on `gcp-0.0.2.jar`: `GcpSecretManagerProvider(GcpProviderOptions)` is public, but the client-accepting overload `GcpSecretManagerProvider(GcpProviderOptions, SecretManagerServiceClient)` is **package-private**; same shape for the Parameter Manager variant. Client creation lives in package-private `SecretManagerClientFactory` / `ParameterManagerClientFactory`, invoked from `createClient()` which `initialize()` calls — so **provider initialization needs credentials**, the first starter in this repo for which that is true.
- `GcpProviderOptions` builder: `projectId`, `credentials`, `version`, `locationId`, `cacheExpiry`, `cacheMaxSize`, `namePrefix`.
- Upstream's own test strategy (read from `providers/gcp/src/test/...`): unit tests Mockito-mock `SecretManagerServiceClient` and stub `accessSecretVersion(SecretVersionName)` returning real protobuf responses (`AccessSecretVersionResponse` → `SecretPayload.data`), injected through the package-private overload; the integration test is `@Tag("integration")` + `@DisabledIfEnvironmentVariable(name = "GCP_PROJECT_ID", matches = "")`, uses ADC, and asserts against hand-seeded secrets.
- The repo's own precedents: statsig's back-off (user mock provider prevents real construction), unleash as the structural reference, and the Unleash-endpoint-gate change's finding that a self-skipping gate is no gate — whose lesson applies here only in its precise form (below, D4).

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract (variant selection, enablement, back-off, binding) is testable with zero Google credentials on the host.
- The credential path is an explicit, tested property, with ADC as fallback rather than as the only route.
- The package-private client seam is respected, not pried.

**Non-Goals:**
- Reaching provider-vs-backend correctness: no Secret Manager emulator tier here (upstream itself does not use the GAPIC in-process emulators; the gRPC-correctness contract is upstream's).
- Multi-secret or custom secret-version selection semantics beyond the options the builder carries.

## Decisions

**D1 — One module, both variants, selected by `spring.openfeature.gcp.type` (default `secretmanager`); an unknown value fails startup naming it.**
The variants share `GcpProviderOptions`, the aspect-free shape, the credential route, and the whole test harness; two modules would duplicate all of that to express one enum. *Alternatives rejected:* two modules (`spring-boot-starter-openfeature-gcp-sm` / `-pm`) — full duplication for a discriminator; both providers registered at once — the SDK admits one provider per client and the second bean would be dead weight.

**D2 — Credentials are property-sourced: `service-account-json` path → `options.credentials`; unset ⇒ ADC as today.**
Making the credential a property is what makes D3 possible at all, and it removes "works only where ADC happens to exist" from the starter's contract. *Alternative rejected:* ADC-only (untestable on CI, hostile to non-GKE hosts); full injectable-`Credentials` bean (serializing Google credential objects via Spring config is not a thing — the path is the property-shaped route).

**D3 — Test posture: back-off for the contract tier; real construction under substitute credentials for the happy path; the package-private overload is never entered by reflection.**
Contract tests (enablement, back-off, customizer, variant selection) never need a live-or-even-credentialled path: a user mock `FeatureProvider` keeps the real provider unconstructed, exactly the statsig move. The happy-path bean-presence test constructs the real provider with a test `Credentials` supplied via the customizer/D2 property (a fixed non-expiring `Credentials` substitute — e.g. the auth-library's test credentials or a `Credentials` subclass — is sufficient because the gRPC channel is lazy: what must be proven, and is task 2's spike, is that `createClient()` under such credentials performs no network/ADC lookup). Reflection into the package-private overload was rejected as brittle against `0.0.x` and as testing through a seam the artifact deliberately sealed; Mockito-on-the-client (upstream's unit trick) is unavailable to us *for the auto-config tests* because the injection point is that same sealed overload — upstream can use it from its own package, this module cannot.
*Consequence accepted:* starter-level tests assert wiring + option assembly + inert initialization; evaluation-semantics tests belong upstream, where the package is reachable.

**D4 — The real-backend integration tier self-skips without `GCP_PROJECT_ID`, and that skip is correct here.**
The Unleash-endpoint-gate change established that a skipping gate is no gate — but that change's subject was *the provider against a real endpoint*, promoted to a build gate on purpose. Here the specs' subject is the Spring wiring; provider-vs-backend conformance is upstream's contract, tested upstream the same self-skipping way. A `Skipped: 1` for the gated class is by-design; the *unit* tier is what must never skip. The distinction is written into the tasks' verification steps so nobody "fixes" the skip into a failure.

**D5 — BOM prices the provider coordinate and the two `com.google.cloud` client libraries.**
Follows the vendor-pair precedent (`unleash-client-java.version`, `flagsmith-java-client.version`, `optimizely-sdk.version` in the sibling change) rather than letting the GAX libraries float transitively: their compatibility window against the provider is precisely the lockstep surface, and `libraries-bom` from `com.google.cloud` is imported into the BOM to price them coherently. The lockstep check asserts the resolved `dev.openfeature.sdk` = `sdk.version` via `dependency:list`.

## Risks / Trade-offs

- [Happy-path construction trips on credential validation at client creation] → D3's spike (task 2) proves inertness *before* the test tier is written, with the named fallback: happy path joins the back-off posture and asserts the options bean only.
- [`0.0.x` provider churn + GAX client drift] → BOM-pinned trio + `dependency:list` assertion; module floor ratcheted from measured coverage so regressions in the new module are caught the same day.
- [The resolved provider poms declare their SDK dep as the **range `[1.16.0,1.99999)`** (verified in all three sibling artifacts' poms), so an unpinned transitive would resolve to the newest 1.x rather than the project's `sdk.version`] → the BOM's own `dev.openfeature:sdk` pin at `1.22.1` is what wins resolution; task 1.3's `dependency:list` check is the mechanism that proves the pin holds, not a formality.
- [Variant property typo silently selecting a default] → spec scenario pins fail-fast-with-value; task carries the test that proves it.
- [Integration tier bit-rotting while permanently skipped] → acceptable: it mirrors upstream's own gating, is not the change's contract, and the unit tier carries the specs.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class. No existing module changes behavior.

## Open Questions

- Whether the Parameter Manager variant honors the same `version`/`locationId` semantics as Secret Manager for naming — immaterial to the specs (which pin only variant selection and option binding), answerable from the provider source during implementation.
