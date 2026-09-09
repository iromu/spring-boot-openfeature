---
title: "Multi-Provider Mode"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "spring-boot-starter-openfeature-multiprovider/src/main/java/org/iromu/openfeature/boot/autoconfigure/multiprovider/MultiProviderAutoConfiguration.java"
updated_at: "2026-09-05"
---

# Multi-Provider Mode

The `multiprovider` starter lets one app combine several flag backends behind a single `Client`.

`MultiProviderAutoConfiguration` (activates when `dev.openfeature.contrib.providers.multiprovider.MultiProvider` is on the classpath):

1. Exposes a `Strategy` bean — default `FirstMatchStrategy`, replaceable by the consumer.
2. Collects **all** `FeatureProvider` beans (from however many provider starters are present) into a `MultiProvider` bean.
3. Exposes the `Client` itself via `multiClient(...)`, after running all `ClientCustomizer`s.

Interaction with the default flow: `ClientAutoConfiguration` is guarded by `@ConditionalOnMissingBean(name = "multiProvider")`, so in multi-provider mode the generic client bean steps aside and this module owns it. All provider auto-configs are also ordered `@AutoConfigureBefore` this one. See [[flows/startup-flow]] and [[risks/fragile-behavior]] for why this bean name matters.
