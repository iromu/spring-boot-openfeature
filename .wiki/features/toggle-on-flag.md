---
title: "ToggleOnFlag Annotation"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/aop/ToggleOnFlag.java"
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/aop/ToggleOnFlagAspect.java"
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/autoconfigure/AspectAutoConfiguration.java"
updated_at: "2026-09-05"
---

# `@ToggleOnFlag` — Declarative Flag Gating

An annotation (`org.iromu.openfeature.boot.aop.ToggleOnFlag`) that gates a method — or every method in a class — on a boolean feature flag.

## Attributes

- `key` — the flag key to evaluate.
- `attributes` — a SpEL map for the evaluation context, e.g. `"{'userId': #id}"`. Method parameters are exposed as SpEL variables by name.
- `orElse` — name of a fallback method (same parameter types) invoked when the flag is false.

## Behavior

`ToggleOnFlagAspect` (an `@Aspect`, registered by `AspectAutoConfiguration`, and `@ConditionalOnBean(Client.class)`) wraps matching methods with around advice:

1. The method-level annotation wins over the class-level one.
2. `attributes` (when not `"{}"`) is evaluated with SpEL into a `Map<String, Value>`; unknown keys throw.
3. The flag is evaluated with `client.getBooleanValue(key, false[, context])` — **default is false**, so a missing flag disables the method.
4. Flag true → original method runs. Flag false → the `orElse` fallback is invoked reflectively. No annotation → normal execution.

## Example

```java
@GetMapping("annotated/user/{id}")
@ToggleOnFlag(key = "users-flag", attributes = "{'userId': #id}", orElse = "featureOnUserIdDisabled")
public String featureOnUserIdAnnotated(@PathVariable("id") final String id) { ... }
```

Evaluation details: [[flows/flag-evaluation]]. Known rough edges (debug `System.out.println`, reflective fallback): [[risks/fragile-behavior]].
