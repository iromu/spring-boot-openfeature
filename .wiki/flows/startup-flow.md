---
title: "Startup Flow"
type: "flow"
status: "active"
language: "default"
source_paths:
  - "spring-boot-openfeature-autoconfigure/src/main/java/org/iromu/openfeature/boot/autoconfigure/ClientAutoConfiguration.java"
  - "spring-boot-openfeature-autoconfigure/src/main/java/org/iromu/openfeature/boot/autoconfigure/OpenFeatureAPIAutoConfiguration.java"
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/autoconfigure/unleash/UnleashAutoConfiguration.java"
  - "spring-openfeature/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
  - "spring-boot-openfeature-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
updated_at: "2026-09-05"
---

# Startup Flow

What happens when a Spring Boot app using this starter starts up:

1. **Auto-config registration.** Each module lists its `@AutoConfiguration` classes in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. The core module registers `AspectAutoConfiguration` + `HealthAutoConfiguration`; the autoconfigure module registers `ClientAutoConfiguration`, `OpenFeatureAPIAutoConfiguration`, and `SecurityAutoConfiguration`.
2. **Provider bean.** A provider starter (e.g. `UnleashAutoConfiguration`) reads `spring.openfeature.<provider>.*` properties, applies any consumer-supplied provider customizer, and exposes a `dev.openfeature.sdk.FeatureProvider` bean. It is annotated `@AutoConfigureBefore(ClientAutoConfiguration.class)` so this happens first.
3. **API singleton.** `OpenFeatureAPIAutoConfiguration` exposes `OpenFeatureAPI.getInstance()` and installs a default customizer that logs provider `READY`/`ERROR`/`STALE`/configuration-change events.
4. **Security hook.** If `SecurityContextHolder` is on the classpath, `SecurityAutoConfiguration` adds a hook injecting `userId` and `authorities` from the current authentication — see [[features/security-integration]].
5. **Client bean.** `ClientAutoConfiguration` (guarded by `@ConditionalOnBean(FeatureProvider.class)` and `@ConditionalOnMissingBean(name = "multiProvider")`) calls `setProviderAndWait(provider)` — blocking until the provider is ready — then exposes the `Client` bean after running all `ClientCustomizer`s.

In multi-provider mode the flow diverges at step 5: `MultiProviderAutoConfiguration` wins and builds the client from all `FeatureProvider` beans — see [[features/multi-provider]].

Downstream, flags are evaluated via the `Client` bean or via `@ToggleOnFlag` — see [[flows/flag-evaluation]].
