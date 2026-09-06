## Why

The test suite only exercises happy paths. Overall branch coverage is 71% (line 89%, instruction 85%), and the weakest classes are the auto-configurations that carry the project's real behavior: `OpenFeatureAPIAutoConfiguration` (54%), `ConfigCatAutoConfiguration` (61%), `ToggleOnFlagAspect` (92% with 5 untested branches), and the 0%-covered `AspectAutoConfiguration` / `HealthAutoConfiguration`. JaCoCo reports coverage but has no `check` gate, so nothing prevents it from silently regressing.

## What Changes

- Add **conditional and functional auto-configuration tests** across the core and all provider starters: assert beans are absent when `enabled=false`, that a user-supplied `FeatureProvider` triggers `@ConditionalOnMissingBean` back-off, and that a registered `*Customizer` actually mutates the built configuration.
- Add **functional tests for `OpenFeatureAPIAutoConfiguration`**: invoke the customizer against a mocked `OpenFeatureAPI` and fire all four provider events (ready / error / stale / configuration-changed).
- Add a **dedicated `ClientAutoConfigurationTest`** for the central `Client` bean (currently untested).
- Add **`ToggleOnFlagAspect` edge-case tests**: condition-false → proceed, condition-false → `orElse` fallback, SpEL duplicate-key, and SpEL non-Map result.
- Add context tests for the 0%-covered `AspectAutoConfiguration` and `HealthAutoConfiguration`.
- **Remove a `System.out.println` debug statement** left in `ToggleOnFlagAspect.resolveAttributesSpEL`.
- Add a **per-module JaCoCo `check` gate** (BUNDLE-level, `INSTRUCTION` counter) with a minimum set to the measured post-test coverage, so the new floor cannot regress in CI.

## Capabilities

### New Capabilities
- `test-coverage`: The project's test and coverage contract — the build enforces a per-module instruction-coverage floor, auto-configuration tests assert positive/conditional/customizer behavior, and the `@ToggleOnFlag` AOP is tested across its decision paths.

### Modified Capabilities
<!-- No existing capabilities; the project has no prior specs. -->

## Impact

- **Build:** `pom.xml` (root) gains a JaCoCo `check` execution; each module's `verify` now enforces its coverage floor.
- **Tests:** New/expanded test classes in `spring-openfeature`, `spring-boot-openfeature-autoconfigure`, and all 11 provider starters. No production behavior changes except the `System.out.println` removal.
- **No API, dependency, or provider-surface changes.**
