---
title: "Provider Starters"
type: "feature"
status: "active"
language: "default"
source_paths:
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/autoconfigure/unleash/UnleashAutoConfiguration.java"
  - "spring-boot-starter-openfeature-unleash/src/main/java/org/iromu/openfeature/boot/unleash/UnleashProperties.java"
  - "examples/pom.xml"
updated_at: "2026-11-09"
---

# Provider Starters

One Maven module per flag backend: `unleash`, `configcat`, `datadog`, `envvar`, `flagd`, `flagsmith`, `flipt`, `gofeatureflag`, `jsonlogic`, `statsig`, `growthbook`, `gcp`, `ofrep`, `optimizely`, `launchdarkly`, `split`, `devcycle`, `featbit`, `featureflip`, `mixpanel`, plus `multiprovider` ([[features/multi-provider]]).

## The shared pattern

`UnleashAutoConfiguration` is the reference implementation. Each provider auto-config:

1. `@AutoConfigureBefore({ClientAutoConfiguration.class, MultiProviderAutoConfiguration})` — the provider bean must exist before the client is built ([[flows/startup-flow]]).
2. `@ConditionalOnClass(<Provider>.class)` — activates only when the provider library is on the classpath.
3. `@ConditionalOnProperty(prefix = ..., name = "enabled", matchIfMissing = true)` — opt-out via `spring.openfeature.<provider>.enabled=false`.
4. `@EnableConfigurationProperties(<Provider>Properties.class)` — binds `spring.openfeature.<provider>.*`.
5. Declares `@Bean @ConditionalOnMissingBean` for the provider's config object and the `FeatureProvider`.

## Provider-specific notes

- **Unleash:** `UnleashProperties` (`spring.openfeature.unleash.*`) maps to `UnleashConfig` — API URL, token (sent as `Authorization` header), app name, environment, optional `backupFile` local cache.
- **EnvVar:** reads flags from environment variables via `System.getenv`; exposes `EnvironmentGateway` and `EnvironmentKeyTransformer` beans so consumers can swap them. No customizer interface.
- **GCP:** `GcpProperties` (`spring.openfeature.gcp.*`) points at a Google Secret Manager secret holding a flag config; a fake provider backs the runnable `gcp-simple` example.
- **OFREP:** binds to an OpenFeature Remote Evaluation Protocol endpoint (`spring.openfeature.ofrep.*`). The provider has no public constructor — the starter builds it via the `constructProvider` factory, which validates the base URL.
- **Optimizely:** `OptimizelyProperties` (`spring.openfeature.optimizely.*`) assembles the vendor SDK's `Configuration` (datafile or sdk-key). The SDK's config parser imports `org.json` without declaring it, so the BOM manages `org.json:json` explicitly.
- **LaunchDarkly:** `LaunchDarklyProperties` (`spring.openfeature.launchdarkly.*`) binds `service-key` (the environment is encoded in the key) and `offline`. The LaunchDarkly OpenFeature provider is vendor-published (`com.launchdarkly:launchdarkly-openfeature-serverprovider`), and its POM ships the LaunchDarkly server SDK in `runtime` scope, so the module declares `launchdarkly-java-server-sdk` explicitly to compile against `LDConfig`. Data-source selection (streaming/polling/file) goes through the `LaunchDarklyCustomizer`; the contract tests run credential-free via a file data source + `events(Components.noEvents())`.
- **Datadog:** `DatadogProperties` (`spring.openfeature.datadog.*`) binds api-key/app-key/site (required, fail-fast) plus `init-timeout` and `evaluation-telemetry-opt-out` for the provider's `Options`. The provider has no programmatic credential API, so the starter publishes the bound values as `DD_API_KEY`/`DD_SITE`/`DD_ENV` system properties only when not already set — the dd-trace settings then pick them up. The provider constructor is lazy; real initialization requires the `dd-java-agent`.
- **Split:** `SplitProperties` (`spring.openfeature.split.*`) supplies the `authorization-key` and an optional `domain` endpoint override. The Split SDK's factory is built eagerly, so the starter assembles a `SplitProviderOptions` (key + `SplitClientConfig`) and fails fast when the key is unset; finer `SplitClientConfig` tuning goes through the `SplitCustomizer` bean. A fake provider backs the runnable `split-simple` example.
- **DevCycle:** `DevCycleProperties` (`spring.openfeature.devcycle.*`) binds `server-side-key` (required; must start with `dvc_server`) and `base-url-override`. The OpenFeature provider (`com.devcycle.sdk.server.openfeature.DevCycleProvider`) ships inside the vendor-published `com.devcycle:java-server-sdk`; the starter wraps a `DevCycleCloudClient` around it. The client constructor is network-lazy (no connection until the first evaluation) and runs no background threads, so the contract tests run credential-free by pointing `base-url-override` at a `MockWebServer`. The module pins `dev.openfeature:sdk` to the repo's `sdk.version` because the DevCycle SDK transitively pulls an older SDK at compile scope.
- **FeatBit:** `FeatBitProperties` (`spring.openfeature.featbit.*`) binds `token`, `streaming-url` and `event-url` (all required, fail-fast) plus `offline` and `disable-events`. The vendor-published provider (`co.featbit:featbit-openfeature-provider-java-server`) has an eager constructor — it always builds an `FBClient` — so the contract tests run credential-free in offline mode (`offline=true`), which wires a null data-synchronizer and a null insight processor and opens no connection. The module declares `featbit-java-sdk` explicitly and pins `dev.openfeature:sdk` lockstep.
- **Featureflip:** `FeatureflipProperties` (`spring.openfeature.featureflip.*`) binds `api-key` (required, fail-fast) plus an optional `base-url` endpoint override; the target environment is encoded in the SDK key itself, so there is no separate environment property. The provider (`io.featureflip:featureflip-openfeature`) is vendor-published by Canopy Labs, its constructor is lazy (bytecode-verified — no I/O until `initialize()`), and the contract tests run credential-free by stubbing `OpenFeatureAPI` so the real provider is constructed but never initialized.
- **Mixpanel:** `MixpanelProperties` (`spring.openfeature.mixpanel.*`) binds `project-token` (required, fail-fast) plus optional `api-host` and `request-timeout-seconds`. The vendor-published provider (`com.mixpanel:mixpanel-java-openfeature`) is wired in remote-evaluation mode — its constructor is lazy (no network I/O until the first flag evaluation), so the contract tests construct the real provider with a fake token and never open a connection. The module pins `dev.openfeature:sdk` to the repo's `sdk.version` because the provider transitively pulls an older SDK.
- Most others (configcat, flagd, flagsmith, flipt, gofeatureflag, jsonlogic, statsig, growthbook) follow the same shape with their own `Properties` + `Customizer`.

## Adding a new provider

Copy the Unleash module: properties, customizer, auto-configuration, `pom.xml`, the `AutoConfiguration.imports` registration, and `ApplicationContextRunner`-style tests. Then add the module + version property to the root `pom.xml` and the BOM.

Extension points: [[concepts/customizers]]. Config reference: [[entities/configuration-properties]].

## Per-provider documentation

Each provider starter module ships a `README.md` with its full `spring.openfeature.<provider>.*` property table, a configuration example, and a short usage snippet. The root `README.md` links to all of them from the provider table.

## Example apps

Every provider starter pairs with a runnable `<provider>-simple` example in the `examples/` reactor (21 modules total including `unleash-advanced`). Each boots without vendor credentials via a `@Primary` fake `FeatureProvider` gated on `--example.fake-provider=true`; the real provider auto-config backs off entirely. See [[reference/build-and-modules]] for the full list.
