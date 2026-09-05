---
title: "Flag Evaluation Flow"
type: "flow"
status: "active"
language: "default"
source_paths:
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/aop/ToggleOnFlagAspect.java"
  - "spring-boot-openfeature-autoconfigure/src/main/java/org/iromu/openfeature/boot/autoconfigure/security/SecurityAutoConfiguration.java"
updated_at: "2026-09-05"
---

# Flag Evaluation Flow

Two entry points feed the same OpenFeature evaluation pipeline:

## Direct API

```java
boolean on = client.getBooleanValue("my-feature", false);
// or with an explicit context:
client.getBooleanValue("users-flag", false, new ImmutableContext(Map.of("userId", new Value(id))));
```

## Declarative

`@ToggleOnFlag(key, attributes, orElse)` on a method or class — the aspect resolves the SpEL `attributes` map, evaluates the flag (default `false`), and proceeds or invokes the `orElse` fallback. See [[features/toggle-on-flag]].

## What happens inside the SDK

1. **Before-hooks run first.** The [[features/security-integration]] hook (when Spring Security is present) merges `userId` + `authorities` from `SecurityContextHolder` into the evaluation context. An explicit context passed by the caller takes precedence per the SDK's merge rules.
2. The provider (Unleash, env var, etc.) evaluates the flag for that context — strategies like "specific user" use the context attributes.
3. On failure the SDK returns the caller-supplied default (always `false` in this project's code paths), so a broken provider degrades to "feature off" rather than an exception.

Provider state is observable via the Actuator endpoint — see [[features/health-indicator]].
