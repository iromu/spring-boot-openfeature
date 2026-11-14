---
title: "Fragile Behavior"
type: "risk"
status: "active"
language: "default"
source_paths:
  - "spring-boot-openfeature-autoconfigure/src/main/java/org/iromu/openfeature/boot/autoconfigure/ClientAutoConfiguration.java"
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/aop/ToggleOnFlagAspect.java"
  - "spring-boot-starter-openfeature-multiprovider/src/main/java/org/iromu/openfeature/boot/autoconfigure/multiprovider/MultiProviderAutoConfiguration.java"
  - "Makefile"
  - ".pre-commit-config.yaml"
updated_at: "2026-11-14"
---

# Fragile Behavior — What Not to Break

## Global SDK singleton

`OpenFeatureAPI.getInstance()` is process-wide state shared by all OpenFeature code in the JVM. This project funnels all provider registration through auto-config (`setProviderAndWait`). Registering a provider outside that path (in user code, tests, or a new module) can silently override what the `Client` bean serves.

## Auto-configuration ordering

Provider auto-configs are `@AutoConfigureBefore({ClientAutoConfiguration, MultiProviderAutoConfiguration})`, and `ClientAutoConfiguration` is guarded by `@ConditionalOnBean(FeatureProvider.class)` + `@ConditionalOnMissingBean(name = "multiProvider")`. If a new provider forgets the `@AutoConfigureBefore`, the client may be built before the provider bean exists — and since `@ConditionalOnBean` is order-sensitive, the failure mode is a missing `Client` bean, not an error message.

## The `multiProvider` bean name

`ClientAutoConfiguration`'s `@ConditionalOnMissingBean(name = "multiProvider")` couples the generic and [[features/multi-provider]] modules by bean name. Renaming the `MultiProvider` bean method breaks that hand-off silently.

## `ToggleOnFlagAspect` details

- A `System.out.println` debug line runs for every SpEL attribute resolution (`ToggleOnFlagAspect.resolveAttributesSpEL`) — noisy and non-standard; treat as a known wart.
- The `orElse` fallback is invoked reflectively (`Method.invoke` with the original parameter types); a fallback with a different signature fails at runtime, not compile time.
- SpEL expressions are evaluated with `StandardEvaluationContext` (full SpEL, not the restricted variant) — an `attributes` expression can reference anything in scope.

## Failure semantics

Flag evaluation defaults to `false` everywhere. A down provider means *all* flag-gated code takes the off/fallback path — by design, but worth knowing when debugging "why did this feature just turn off?".

## Design documents drift

`docs/` replaced the OpenSpec trees on 2026-11-14. A design change that lands only in code silently re-creates the problem that killed the specs: the retired trees claimed mandatory properties that do not exist (Octopus Deploy, FeatBit) and stayed wrong because nothing enforced them. Update the matching `docs/` page together with the decision — see [[overview/design-docs]].

## `make lint` must name the checkstyle execution

`make lint` and `make checkstyle` have to invoke `checkstyle:check@checkstyle-validation`.
The plugin's `configLocation` (the Spring ruleset), `includeTestSourceDirectory=false` and
`violationIgnore=SpringNullability` live on that one execution (`pom.xml:276-288`), so a bare
`checkstyle:check` inherits none of them and silently falls back to Checkstyle's default
`sun_checks.xml` — which reports 68 violations the project never enforced, including
80-character lines and `FileTabCharacter` (the repo indents Java with tabs on purpose).

Two ways this goes wrong quietly:

- Drop the `@checkstyle-validation` suffix and lint checks the wrong ruleset: red on clean
  code, and blind to the Spring rules that actually matter.
- Give the target no recipe and the local `make-lint` pre-commit hook reports "Passed"
  having run nothing — which is exactly how it shipped until 2026-11-14, alongside a
  duplicated `checkstyle:` target that made `make` warn on every call.

Verify either change behaviorally rather than by exit code: add a file with a `java.util.*`
star import and confirm the goal fails with `AvoidStarImport`.
