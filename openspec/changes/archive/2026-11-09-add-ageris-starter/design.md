## Context

Ageris publishes an OpenFeature Java provider for a file-based feature-flag and
configuration platform (GitHub: `lichtsinn/ageris`, "Feature flags, runtime
configuration and policies for Java"). The provider reads feature flags and JSON
configs from configuration files (local filesystem by default) and exposes them
through the OpenFeature API. It has no network service and no API key.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Ageris credentials or
  files on the host.
- The required inputs (application name, config path, feature-flag files) are
  explicit, tested properties.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — Required inputs are property-sourced, and the auto-config fails fast.**
Ageris is a file-based platform: it has no API key (finding F2 below). The
required inputs are `spring.openfeature.ageris.application`,
`spring.openfeature.ageris.path` (the directory holding the configuration
files), and `spring.openfeature.ageris.feature-flag-files` (at least one file
name). The auto-config SHALL fail-fast with an `IllegalStateException` naming
the missing property path when the starter is enabled but any of them is
unset.

**D3 — Test posture: real temp files for the client tier, fake paths for the
assembly tier.**
The provider constructor is lazy (finding F3): it stores its managers and
loader and does no I/O. Only `initialize()` — invoked by OpenFeature on
`setProviderAndWait` — starts the loader, whose scheduled executor uses
non-daemon threads. A Mockito construction mock is NOT an option here:
`AgerisProvider` extends `EventProvider`, and the SDK's `attach()` touches a
final field that a mock leaves null (NPE). The client-tier test therefore
points the real provider at real temp configuration files, asserts a flag
lookup end to end, and shuts the provider down so the loader threads
terminate (the SDK also shuts down a replaced provider, so cross-test
interference on the `OpenFeatureAPI` singleton is safe). Assembly tests
construct the real provider with fake paths; nothing is read.

**D4 — BOM prices the provider coordinate.**
Pinned at `ageris-openfeature.version`. Lockstep check asserts
`dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Loader threads stay alive once the provider is initialized] → D3's
  client-tier test shuts the provider down after the assertions; assembly
  tests never register the provider with OpenFeature.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows,
and the property class.

## Findings (feasibility gate, tasks 1.1–1.2)

**F1 — Coordinate.** `io.github.lichtsinn:ageris-openfeature`, latest release
`0.7.0`, on Maven Central
(`https://repo1.maven.org/maven2/io/github/lichtsinn/ageris-openfeature/maven-metadata.xml`
returns 200 with `0.4.0/0.5.0/0.6.0/0.7.0`). Listed on the OpenFeature
ecosystem page as "Ageris Java Server Provider" (vendor supported).

**F2 — No API key.** The provider takes no credential. Its constructor is
`AgerisProvider(FeatureFlagManager, JsonConfigManager, ConfigurationLoader)`
(or `AgerisProvider.builder(ffm, jcm).loader(loader).targetingKeyType(type)
.firstLoadTimeout(d).build()`). The configuration comes from files read by a
`ConfigurationFetcher` (the starter builds a `FilesystemConfigurationFetcher`
for a configured directory). The original D2 "API key" premise is therefore
replaced by the required file-based inputs.

**F3 — Constructor is lazy; `initialize()` starts the loader.** Verified from
the 0.7.0 source: the constructor only stores fields. `initialize()` starts
the `ConfigurationLoader` and waits for the first load (default 30s);
`shutdown()` stops it (idempotent). `Ageris` creates its
`ScheduledExecutorService` with `Executors.newScheduledThreadPool(3)`
(non-daemon threads, created on demand), so a started loader keeps the JVM
alive — tests must not start the loader unless they shut the provider down.

**F4 — SDK lockstep is structurally safe.** The provider declares
`dev.openfeature:sdk` with `provided` scope (built against 1.23.0, the same as
the repo's `sdk.version`), so it never pulls a transitive SDK version; the
consumer's SDK arrives through `spring-openfeature` at the pinned version.

**F5 — File layout.** `FilesystemConfigurationFetcher` resolves every
registered file as `<basePath>/<application>/<fileName>`. The
`spring.openfeature.ageris.path` property is therefore the base directory,
and the client-tier test writes its temp files under
`<tempDir>/<application>/` (observed from the 0.7.0 loader's error message
naming the resolved paths).
