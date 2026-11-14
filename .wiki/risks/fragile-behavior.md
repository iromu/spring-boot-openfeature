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

## `make lint` checks nothing

`Makefile:53` declares `.PHONY: lint` but no `lint:` rule follows it — the recipe below belongs to a `checkstyle:` target that is then defined a second time at `Makefile:58`, so `make` warns "overriding recipe for target 'checkstyle'" on every invocation. Consequences:

- `make lint` prints `Nothing to be done for 'lint'` and exits 0, so the local `make-lint` pre-commit hook passes having run nothing.
- With the standalone checkstyle hook removed from `.pre-commit-config.yaml`, nothing at commit time enforces style; only the Maven `validate` phase does, on every actual build.

Treat a green `make lint` as no evidence at all until the target gets a recipe.
