---
title: "Health Indicator"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/health/OpenFeatureHealthIndicator.java"
  - "spring-openfeature/src/main/java/org/iromu/openfeature/boot/autoconfigure/HealthAutoConfiguration.java"
updated_at: "2026-09-05"
---

# Health Indicator

`OpenFeatureHealthIndicator` exposes the flag provider's state through the Actuator health endpoint. It is `@ConditionalOnBean(OpenFeatureAPI.class)` and imported by `HealthAutoConfiguration`.

## State mapping

| SDK provider state | Actuator status |
| --- | --- |
| `READY` | UP (detail: provider class name) |
| `NOT_READY`, `STALE` | OUT_OF_SERVICE |
| `ERROR`, `FATAL` | DOWN |
| `NoOpProvider` (no provider set) | DOWN, detail `No active OpenFeature provider` |
| any exception | DOWN with the exception message |

This makes flag-backend outages visible in standard health checks — e.g. an Unleash instance that is unreachable shows the app as degraded rather than silently serving default flag values.
