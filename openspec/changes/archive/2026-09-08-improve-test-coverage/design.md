## Context

See proposal.md for motivation. Current state: 85% instruction / 71% branch / 89% line coverage. JaCoCo is configured with `prepare-agent` + `report` only (no `check`), so coverage is reported but not enforced. The existing test convention is `ApplicationContextRunner` + `AutoConfigurations.of(...)` + `withPropertyValues(...)` + AssertJ — but each test asserts only the happy path (bean presence). The weak axis is branch coverage: conditionals (`enabled=false`, `@ConditionalOnMissingBean` back-off, customizer application, AOP decision paths) are untested.

## Goals / Non-Goals

**Goals:**
- Lift branch coverage by adding conditional, back-off, customizer, and AOP edge-case tests across the core and all 11 provider starters.
- Close the specific thin spots: `OpenFeatureAPIAutoConfiguration` (54%), `ConfigCatAutoConfiguration` (61%), `ToggleOnFlagAspect` branches, the 0%-covered `AspectAutoConfiguration`/`HealthAutoConfiguration`, and the untested `ClientAutoConfiguration`.
- Add a per-module JaCoCo `check` gate so the improved floor cannot regress.
- Remove the leftover `System.out.println` in `ToggleOnFlagAspect`.

**Non-Goals:**
- No new providers, no new dependencies, no public API changes.
- No refactoring beyond the `System.out.println` removal.
- No change to provider runtime behavior.

## Decisions

### 1. Gate on INSTRUCTION coverage, not BRANCH or LINE
- **Choice:** The `check` rule uses the `INSTRUCTION` counter with a `COVEREDRATIO` minimum.
- **Why:** Instruction ratio is the most stable and discriminating metric. Branch coverage is fragile on low-branch modules (e.g. `envvar` has near-zero branches in covered paths, so a branch floor is noise). Line coverage is already at 89% and is a weaker signal.
- **Alternatives considered:** Branch floor (rejected — brittle on low-branch modules); line floor (rejected — less discriminating); a secondary lower branch ratchet (optional, added only if a module's branch floor is meaningfully stable).

### 2. Per-module BUNDLE-level check, not global or per-class
- **Choice:** One BUNDLE-level `check` per module (the JaCoCo default element), configured in the root pom so it runs for every module.
- **Why:** A single global floor would let a strong module (envvar, 100%) mask a weak one (configcat, 61%). Per-class rules are over-fit to class names and brittle. BUNDLE-level (module aggregate) smooths tiny classes (the two 0% auto-configs are 3 instructions each) against the module's other code.
- **Alternatives considered:** Global floor (rejected — coarse); per-class rules (rejected — brittle, high maintenance).

### 3. Threshold = measured post-test coverage (ratchet), captured in the final wave
- **Choice:** Each module's `INSTRUCTION` minimum is set to its *measured* coverage after the new tests land, captured during the gate wave.
- **Why:** A pre-chosen fixed target is a chicken-and-egg: if any module can't reach it, `./mvnw verify` fails. A ratchet at the measured value always passes today and prevents any future drop — exactly the "can't silently regress" semantic requested. Floors can be ratcheted upward over time.
- **Alternatives considered:** Fixed per-module targets (rejected — must be hit exactly; fragile); a single global 0.90 (rejected — same masking problem as #2).

### 4. Test pattern: the 3-assertion `ApplicationContextRunner` convention
- **Choice:** Each backend provider test keeps the positive assertion and adds: (a) `enabled=false` → no `FeatureProvider` bean; (b) user-supplied `FeatureProvider` → auto-config backs off (`@ConditionalOnMissingBean`); (c) where a customizer exists, a registered `*Customizer` actually mutates the built config. `multiprovider` is exempt from (a) — see the `multiprovider` exemption under the enabled-gate requirement in `specs/test-coverage/spec.md`.
- **Why:** Matches the repo's existing convention. Assertions (a) and (b) are cheap — bean presence/absence, no network needed. This is what lifts branch coverage most efficiently.
- **Customizer coverage:** 9 providers have a `*Customizer` (configcat, flagd, flagsmith, flipt, gofeatureflag, growthbook, statsig, unleash, + core). 3 do not (envvar, jsonlogic, multiprovider): `envvar` and `jsonlogic` get (a) and (b), while `multiprovider` is exempt from (a) and so gets only (b).

### 5. Functional tests use a mocked `OpenFeatureAPI` / `Client`
- **Choice:** For `OpenFeatureAPIAutoConfiguration`, pull the `OpenFeatureAPICustomizer` bean, call `.customize(mockApi)` on a mocked `OpenFeatureAPI`, then fire each event and verify the handler was wired. For `ToggleOnFlagAspect`, inject a mocked `Client` returning controlled booleans.
- **Why:** These verify behavior, not just bean presence. Mocking avoids network and provider startup. (Existing HTTP-based provider tests use `mockwebserver`; the conditional tests don't need it.)
- **Alternatives considered:** Full provider integration with a real backend (rejected — slow, flaky, out of scope).

### 6. Remove the `System.out.println` debug statement
- **Choice:** Delete the `System.out.println("Adding to SpEL context: ...")` line in `ToggleOnFlagAspect.resolveAttributesSpEL`.
- **Why:** It runs on every SpEL-flagged call in production; it's a debug leftover. No test relies on its output.

## Risks / Trade-offs

- **[A module can't reach a clean floor due to inherently-unreachable defensive code (exception catches, back-off paths)]** → The ratchet is set to *measured* coverage, not an arbitrary target, so it always passes; raise it incrementally over time.
- **[JaCoCo config currently lives in `pluginManagement`; a `check` there may not run per module]** → Add the `check` execution under the active `build/plugins` so it binds to each module's `verify`.
- **[Two 0% auto-config classes are tiny; a per-class floor would be noisy]** → Use BUNDLE-level (module aggregate), which smooths them out.
- **[Removing `System.out.println` changes stdout in tests]** → It's a debug artifact; no assertion depends on it.
- **[Some provider customizers may be hard to observe]** → Assert on the constructed config object; only skip the customizer assertion for the 3 providers with no customizer.

## Open Questions

- The exact per-module floor values are captured at implementation time (gate wave) from the measured post-test coverage. This is deferrable and does not change the specs, the approach, or the task breakdown.
