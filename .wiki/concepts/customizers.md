---
title: "Customizer Extension Points"
type: "concept"
status: "active"
language: "default"
source_paths:
  - "spring-boot-openfeature-autoconfigure/src/main/java/org/iromu/openfeature/boot/autoconfigure/ClientCustomizer.java"
  - "spring-boot-openfeature-autoconfigure/src/main/java/org/iromu/openfeature/boot/autoconfigure/OpenFeatureAPICustomizer.java"
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/unleash/UnleashCustomizer.java"
updated_at: "2026-09-05"
---

# Customizer Extension Points

The project follows Spring Boot's functional-bean idiom: auto-config consumes `ObjectProvider<XCustomizer>` and applies all customizers in order. Consumers define a `@Bean` implementing the interface — no auto-config editing needed.

Three levels exist:

1. **`ClientCustomizer`** — applied to the `Client` bean after it is created (`ClientAutoConfiguration` / `MultiProviderAutoConfiguration`). Use for hooks or client-level tweaks.
2. **`OpenFeatureAPICustomizer`** — applied to the global `OpenFeatureAPI` singleton (`OpenFeatureAPIAutoConfiguration`). The built-in logging customizer and the [[features/security-integration]] hook both use this. Consumers can add hooks, event handlers, etc.
3. **Provider customizers** (e.g. `UnleashCustomizer`) — applied to the provider's config builder inside the provider auto-config. Use for settings not covered by `spring.openfeature.<provider>.*` properties ([[entities/configuration-properties]]).

All customizers are `Ordered`-aware (`orderedStream()`), so consumers can control application order.
