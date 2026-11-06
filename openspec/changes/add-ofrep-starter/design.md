## Context

See `proposal.md` — Why. Design-relevant current state, all of it read from the resolved artifact rather than assumed:

- `javap` on `ofrep-0.0.2.jar` shows `OfrepProvider` is `public final` with **no public constructor**: construction is `OfrepProvider.constructProvider()` / `constructProvider(OfrepProviderOptions)` static factories.
- `OfrepProviderOptions` is builder-shaped over: `baseUrl`, `proxySelector`, `executor`, `requestTimeout`, `connectTimeout`, `headers` (map of string → list of string).
- The provider's pom prices jackson, guava, and the OpenFeature SDK; no vendor client library beyond those — unlike unleash/statsig there is no separate vendor SDK to version.
- **flagd** is the structural template in this repo: a `FeatureProvider` bean `@AutoConfigureBefore(ClientAutoConfiguration.class)`, `@ConditionalOnClass`, an `enabled` gate, `@ConditionalOnMissingBean` on both the options and the provider beans, and a customizer `ObjectProvider` stream applied to the builder (`FlagdAutoConfiguration`).
- The statsig test proves the house back-off pattern for contract tests: a user-registered mock `FeatureProvider` keeps the real provider from being constructed at all.

## Goals / Non-Goals

**Goals:**
- The starter is a faithful instance of the provider-starter pattern; a reviewer who knows flagd needs no new concepts.
- Construction and tests never require a live OFREP endpoint.
- The pinned `sdk.version` stays authoritative over the contrib parent's version range.

**Non-Goals:**
- No endpoint gate / testcontainers provisioning for an OFREP server — upstream ships none, and provider-vs-protocol conformance is upstream's contract, not the starter's.
- No streaming/context-matching surface beyond what `constructProvider` already wires.

## Decisions

**D1 — Build the provider via `constructProvider(OfrepProviderOptions)`; never reflect into the non-public constructor.**
The factory is the artifact's supported entry point; the constructor's non-publicity is a deliberate encapsulation signal. *Alternative rejected:* reflective construction — brittle across `0.0.x` and silently bypasses factory-side validation.

**D2 — Property surface = `enabled`, `base-url`, `connect-timeout`, `request-timeout`, `headers`; `proxySelector` and `executor` stay customizer-only.**
Those two are collaborator-shaped (a `ProxySelector`, an `Executor`) — property-binding them invites awkward converters and most consumers never touch them; the customizer already exposes the builder for exactly this. *Alternative considered:* full 1:1 property mapping, rejected as binding ergonomics over usefulness; consistent with flagd exposing only its commonly-set fields.

**D3 — Test posture: back-off pattern for the contract tier; happy-path bean-presence tests construct the real provider, contingent on an offline-safety spike.**
Contract tests (disabled, back-off, customizer-applied) follow statsig verbatim, where a user mock provider prevents real construction. The happy-path test does construct, so a first task must *prove* `constructProvider` is offline-safe (no socket at construction or at `Client` registration) — flagd's precedent says provider construction is inert until evaluation; if the spike finds eager connection, the happy path adopts the back-off shape too and asserts options-bean wiring instead. *Alternative rejected:* asserting bean presence via a mocked options bean only — it would leave the actual provider-construction path untested.

**D4 — BOM prices only `dev.openfeature.contrib.providers:ofrep`; transitives flow.**
There is no vendor client to pin (unlike the `unleash-client-java` / `flagsmith-java-client` pattern) — the provider's own pom already prices its jackson/guava needs; re-pinning them here would duplicate the BOM's stated job. The lockstep rule is enforced by check, not by pinning: a task runs `dependency:list` on the module and requires the resolved `dev.openfeature.sdk` to equal the root `sdk.version`.

## Risks / Trade-offs

- [Provider is `0.0.x`: API churn on bump] → version is BOM-pinned; the lockstep `dependency:list` check catches SDK drift at build time rather than at runtime.
- [`constructProvider` eagerly connects, breaking the credential-free happy path] → D3's spike is sequenced before test authoring and names the fallback posture, so the outcome changes a test shape, not the specs.
- [Contrib parent resolves the SDK via a `[1.0,2.0)`-style range] → the resolved version is asserted against `sdk.version`, not trusted.

## Migration Plan

Additive new module; no consumer of existing modules is touched. Rollback is removing the `<module>` entry and the BOM row — no state, no migration.

## Open Questions

- Whether to add an evaluation round-trip test over `mockwebserver` (the OFREP HTTP API against a local stub) in this change or leave it to a follow-up; either way the specs' scenarios are satisfied by the contract tier, so this does not block tasks. Precedent exists in-repo and is cheap to follow: `GrowthBookAutoConfigurationTest` already runs a live `MockWebServer` with enqueued JSON bodies (and the same user-supplied-provider back-off idiom as statsig), so the decision is scope, not feasibility.
