## Context

CloudBees publishes an OpenFeature Java provider requiring an API key to connect to the CloudBees service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero CloudBees credentials on the host.
- The API key is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — API key is property-sourced: `spring.openfeature.cloudbees.api-key`.**
The auto-config SHALL fail-fast if the key is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The back-off pattern is sufficient; the provider likely opens a network connection on construction.

**D4 — BOM prices the provider coordinate.**
Pinned at `cloudbees-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Feasibility findings (resolved 2026-11-10)

**Coordinate (task 1.1).** `com.cloudbees.openfeature:cloudbees-openfeature-provider`,
latest stable `1.0.1`. Verified from raw Maven Central bytes:
`https://repo1.maven.org/maven2/com/cloudbees/openfeature/cloudbees-openfeature-provider/maven-metadata.xml`
(HTTP 200; versions 0.1.1, 0.3.0, 1.0.0, 1.0.1) and the
`cloudbees-openfeature-provider-1.0.1.pom` (HTTP 200). The provider class is
`com.cloudbees.openfeature.provider.CloudbeesProvider` (note the lowercase `b`);
its constructors are `CloudbeesProvider(String appKey)` and
`CloudbeesProvider(String appKey, RoxOptions options)`. The provider transitively
declares `dev.openfeature:sdk:1.0.0`, so the starter pins
`dev.openfeature:sdk:${sdk.version}` explicitly (lockstep, mixpanel/vwo precedent).
The `RoxOptions.Builder` surface used by the starter ships at compile scope in
`io.rollout.rox:rox-java-server:6.0.0` (transitive), so no extra explicit
dependency is needed.

**Constructor posture (task 1.2).** The constructor is eager: it calls
`Rox.setup(appKey, options).get()` (blocking) before returning, which opens a
connection to the CloudBees (Rox) service. Tests therefore construct the real
provider only inside a Mockito `mockStatic(Rox.class)` of `Rox.setup`
(VWO/Split precedent); the disabled/back-off/missing-credential tests never
construct the provider at all.
