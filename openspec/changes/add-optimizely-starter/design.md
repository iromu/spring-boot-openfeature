## Context

See `proposal.md` — Why. Design-relevant current state, read from the resolved artifact:

- `javap` on `optimizely-1.0.0.jar`: `OptimizelyProvider extends EventProvider`, public constructor `OptimizelyProvider(OptimizelyProviderConfig)`, an `initialize(EvaluationContext)` override, and a `getOptimizely()` accessor — the provider owns/bridges a vendor client.
- `OptimizelyProviderConfig` is builder-shaped over vendor collaborators: `ProjectConfigManager`, `ProjectConfig`, `EventHandler`, `EventProcessor`, `UserProfileService`, `ErrorHandler`, `ODPManager`, plus `datafile` and `defaultDecideOptions`.
- The provider's pom prices `com.optimizely.ab:core-api` and `core-httpclient-impl` — a real vendor SDK pair, which is why this is the statsig/unleash/flagsmith family (vendor-client-backed), not the flagd family. **Both are priced `4.2.2` in the resolved `optimizely-1.0.0.pom`** — the value `optimizely-sdk.version` takes.
- `javap -c` on `initialize()` shows a **pure pass-through**: `Optimizely.builder().withConfigManager(config.getProjectConfigManager()).withDatafile(config.getDatafile()).withEventProcessor(...).withErrorHandler(...).withUserProfileService(...).withDefaultDecideOptions(...).withODPManager(...).build()`, then `ContextTransformer.builder().optimizely(...)`. The provider never constructs a vendor manager itself — the starter must supply either a manager (online) or a datafile (offline).
- `core-httpclient-impl:4.2.2` carries `HttpProjectConfigManager.builder()` (with `CONFIG_SDK_KEY` / `CONFIG_DATAFILE_AUTH_TOKEN` / poll-interval constants) and `OptimizelyFactory.newDefaultInstance(sdkKey, datafileAccessToken, configUrl, ...)` overloads; `PollingProjectConfigManager`'s own constructor takes no key (`setSdkKey` is protected).
- The statsig test's load-bearing comment is the caution for this shape: *"The user-supplied provider makes the auto-config provider back off, so the real StatsigProvider (which runs postInit) is never constructed"* — constructing a vendor-backed provider can start vendor machinery.

## Goals / Non-Goals

**Goals:**
- Property surface stays small and honest: what properties cannot express, the customizer can; no property is invented that the config builder cannot actually carry.
- Tests never start vendor background machinery (polling/ODP/event threads) unless the test is explicitly about it.
- The vendor SDK pair is BOM-priced so the lockstep convention covers it.

**Non-Goals:**
- Surfacing the full vendor collaborator set (ODP, eventing, bucketing) as properties — the customizer is the escape hatch for those.
- Eventing semantics beyond what `EventProvider` inheritance gives for free.

## Decisions

**D1 — Auto-configuration assembles `OptimizelyProviderConfig` via its builder; properties carry `enabled`, `datafile`, and the scalar manager settings (`sdk-key`, `datafile-auth-token`, `config-url`, `poll-interval`); the customizer receives the config builder.**
Since `initialize()` is a pure pass-through (Context), the starter must place a `ProjectConfigManager` or a `datafile` into the config — and the manager settings are all scalars, so they are property-expressible rather than customizer-only. The remaining builder fields are vendor collaborator objects — property-binding them is impossible or meaningless, and the customizer exists precisely to reach them. *Alternative rejected:* mirroring every builder field with a property (unexpressible for collaborator types) and consumers hand-building the whole config (defeats auto-configuration).

**D2 — The happy-path test runs in datafile mode, contingent on an offline-boot spike.**
A datafile-supplied vendor client is the vendor's own offline configuration route, which is what makes a bean-presence test constructible without network. The pass-through in Context sharpens the spike to its exact question: with a datafile-only config, `initialize()` hands `config.getProjectConfigManager()` — **null** — to `withConfigManager(...)`; the spike must prove `Optimizely.Builder.build()` tolerates the null manager when a datafile is present and that no polling/ODP thread starts that a credential-less CI host would trip over. If it does not, the happy path adopts statsig's back-off posture and the datafile test moves behind a `@Tag` gate. *Alternative rejected:* happy-path testing only a mocked user provider — it would leave the auto-config's own config-assembly code unexercised.

**D3 — The sdk-key route is starter-built `HttpProjectConfigManager` from scalar properties (spike resolved during exploration 2026-09-17; task 1.1 re-verifies against the resolved jar).**
The pre-resolution framing was open: `javap` showed no sdkKey on the config builder, and the route could have been a starter-built config, customizer-only collaborators, or a factory. The vendor jars answer it: `HttpProjectConfigManager.builder()` keys on `CONFIG_SDK_KEY` / `CONFIG_DATAFILE_AUTH_TOKEN` / poll-interval constants, `OptimizelyFactory.newDefaultInstance(sdkKey, datafileAccessToken, configUrl, ...)` offers the string-key overloads, and `initialize()` (Context) only passes the config's manager/datafile through — so the starter **must** place one or the other. Outcome: `sdk-key` (+ optional `datafile-auth-token`, `config-url`, `poll-interval`) properties build the manager as the online route; `datafile` is the offline route; exotic manager variants stay customizer territory. The specs pin only the datafile scenario, so this resolution does not disturb them.

**D4 — BOM prices the provider *and* the vendor pair (`optimizely.version`, `optimizely-sdk.version`).**
This mirrors `unleash-client-java.version` / `flagsmith-java-client.version` sitting beside their provider properties: the provider↔vendor-SDK compatibility window is exactly the thing the lockstep convention exists to protect, and letting it float transitively puts it outside BOM control. The lockstep check is `dependency:list` asserting both the resolved `dev.openfeature.sdk` = `sdk.version` and the vendor pair = the BOM's prices.

## Risks / Trade-offs

- [Vendor machinery starts at construction/`initialize`, making CI tests network-dependent] → D2 gates the whole test posture on an offline-boot spike with a named fallback (back-off + tagged integration tier).
- [`sdkKey` route turns out to require a second property surface or customizer-only usage] → D3's spike is sequenced before auto-config authoring; both outcomes were pre-enumerated and spec-compatible.
- [Vendor `1.0.0` API drift on future bumps] → BOM-pinned pair + lockstep `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry and the three BOM rows. No consumer-visible change to existing modules.

## Open Questions

(none deferrable past the spike — D3 is resolved by task 2's spike before implementation commits to a property surface.)
