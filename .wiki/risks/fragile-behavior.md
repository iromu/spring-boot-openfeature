---
title: "Fragile Behavior"
type: "risk"
status: "active"
language: "default"
source_paths:
  - "spring-boot-openfeature-autoconfigure/src/main/java/org/iromu/openfeature/boot/autoconfigure/ClientAutoConfiguration.java"
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/aop/ToggleOnFlagAspect.java"
  - "spring-boot-starter-openfeature-multiprovider/src/main/java/org/iromu/openfeature/boot/autoconfigure/multiprovider/MultiProviderAutoConfiguration.java"
updated_at: "2026-09-05"
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
