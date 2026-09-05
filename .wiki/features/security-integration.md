---
title: "Security Integration"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "spring-boot-openfeature-autoconfigure/src/main/java/org/iromu/openfeature/boot/autoconfigure/security/SecurityAutoConfiguration.java"
updated_at: "2026-09-05"
---

# Security Integration

When Spring Security is on the classpath, `SecurityAutoConfiguration` (guarded by `@ConditionalOnClass(SecurityContextHolder.class)`) registers an `OpenFeatureAPICustomizer` that adds an OpenFeature hook.

The hook runs **before every flag evaluation**: it reads the current `Authentication` from `SecurityContextHolder` and, if present, merges an evaluation context with:

- `userId` — the principal name
- `authorities` — the list of granted authorities

This means per-user flag strategies (e.g. "roll out to user 111") work automatically for any `client.getBooleanValue(...)` call, with no manual context passing. It composes with explicit contexts — see [[flows/flag-evaluation]] — and with [[features/toggle-on-flag]] attributes.

No configuration is required; the integration is purely classpath-driven.
