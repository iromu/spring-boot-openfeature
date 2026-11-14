# System Architecture

**Spring Boot OpenFeature** (`org.iromu.openfeature:spring-boot-openfeature`, a `pom` aggregator) is a
Spring Boot starter library that wires the [OpenFeature](https://openfeature.dev/) Java SDK into a Spring
Boot application. Java 17, Spring Boot 4.1.2 (`spring-boot.version`), OpenFeature SDK 1.23.0
(`sdk.version`), project version `4.1.5-SNAPSHOT` (`revision`). It is a library, not an application — the
runnable apps live in the separate [examples](../examples) reactor.

## Consumer contract

Three steps, no code required:

1. Import the BOM `org.iromu.openfeature:spring-boot-openfeature-dependencies` (or pin versions yourself).
2. Add **one** provider starter, e.g. `spring-boot-starter-openfeature-unleash`.
3. Set `spring.openfeature.<provider>.*` in `application.yml`.

The result is an auto-configured `dev.openfeature.sdk.Client` bean (plus an Actuator health indicator and
the `@ToggleOnFlag` annotation). Inject `Client` and call `getBooleanValue`/`getStringValue`/…, or annotate
a method. Each provider starter pulls `spring-boot-openfeature-autoconfigure`, which pulls
`spring-openfeature` — so a single dependency is enough; nothing in this repo depends on the
`spring-boot-starter-openfeature` artifact except consumers.

The `enabled` flag that actually disables something is always the **per-provider** one
(`spring.openfeature.<provider>.enabled`). `OpenFeatureProperties` (`spring.openfeature`, a single `enabled`
field) exists in `spring-openfeature` but no auto-configuration binds or reads it, so there is currently no
working global on/off switch.

## Module map

The root [pom.xml](../pom.xml) is a `pom`-packaging aggregator. Four core modules:

| Module | Owns |
| --- | --- |
| `spring-openfeature` | The lowest layer: `@ToggleOnFlag` + `ToggleOnFlagAspect` (`org.iromu.openfeature.boot.aop`), `OpenFeatureHealthIndicator`, `OpenFeatureProperties`, and the `AspectAutoConfiguration` / `HealthAutoConfiguration` auto-configs. Depends only on `spring-boot-starter-aspectj`, `spring-boot-starter-actuator` and `dev.openfeature:sdk`. |
| `spring-boot-openfeature-autoconfigure` | Generic wiring: `ClientAutoConfiguration` (the `Client` bean), `OpenFeatureAPIAutoConfiguration` (SDK singleton + event logging), `security.SecurityAutoConfiguration`, and the `ClientCustomizer` / `OpenFeatureAPICustomizer` interfaces. The only module that depends on `spring-openfeature`. |
| `spring-boot-starter-openfeature` | Base starter (`pom` packaging): imports the BOM and depends on the autoconfigure module. The provider-agnostic artifact for consumers. |
| `spring-boot-openfeature-dependencies` | The BOM: pins `dev.openfeature:sdk` to `${sdk.version}` (keeping every provider in lockstep) plus every vendor provider and SDK library version. See [dependency-management.md](dependency-management.md). |

**29 provider starters** — `spring-boot-starter-openfeature-<provider>`, one per backend, each with its own
version column and README in the [root README](../README.md). Every one of them depends directly on
`spring-boot-openfeature-autoconfigure`.

| Extra module | Build profile |
| --- | --- |
| `aggregate-report` — JaCoCo `report-aggregate` for Sonar; a Groovy plugin injects every reactor module as a dependency so aggregation needs no manual list | `-P sonar` |
| `examples` — 30 runnable sample apps, its own reactor | `-P examples` |

The `publish` profile adds GPG signing and the Central Portal publishing plugin (no extra module). The
reactor also declares the `jitpack.io` repository, which the JitPack-only providers resolve from (e.g.
`com.github.growthbook:growthbook-openfeature-provider-java`, pinned in the BOM).

## Wiring flow

```
provider starter auto-config          spring-boot-openfeature-autoconfigure
spring.openfeature.<provider>.*  ──►  <Provider>Properties
        + <Provider>Customizer(s)  ──►  provider options / config bean
                                     ──►  FeatureProvider bean   ──┐
                                                                   ▼
                       OpenFeatureAPIAutoConfiguration: OpenFeatureAPI.getInstance()
                                     + OpenFeatureAPICustomizer(s) (logger, security hook)
                                                                   ▼
                       ClientAutoConfiguration: setProviderAndWait(provider)
                                     + ClientCustomizer(s)  ──►  Client bean
```

Ordering and back-off, as implemented:

- Every provider auto-config is `@AutoConfigureBefore({ClientAutoConfiguration.class}, name = "org.iromu.openfeature.boot.autoconfigure.multiprovider.MultiProviderAutoConfiguration")`, so the `FeatureProvider` bean exists before the client is considered.
- `ClientAutoConfiguration` is class-level `@ConditionalOnBean(FeatureProvider.class)` **and** `@ConditionalOnMissingBean(name = "multiProvider")`; its bean is `@Bean @ConditionalOnMissingBean`.
- Because `@ConditionalOnBean` is order-sensitive, a provider starter that forgot the ordering hint fails as *a missing `Client` bean*, not as an error.
- `spring-openfeature` cannot reference `ClientAutoConfiguration` as a class (it does not depend on that module), which is why `AspectAutoConfiguration` uses the `@AutoConfigureAfter(name = "...ClientAutoConfiguration")` string form.

**Multi-provider mode.** Adding `spring-boot-starter-openfeature-multiprovider` changes who owns the client.
`MultiProviderAutoConfiguration` (`@ConditionalOnClass(MultiProvider.class)`, no `enabled` gate) exposes a
`Strategy` bean (default `FirstMatchStrategy`, replaceable), collects **all** `FeatureProvider` beans into a
`multiProvider` bean, and builds the `Client` itself in a `multiClient` bean — same
`setProviderAndWait` + `ClientCustomizer` sequence. The `multiProvider` bean name is what makes
`ClientAutoConfiguration` step aside; renaming that bean method breaks the hand-off silently. The
autoconfigure module keeps the multiprovider library as an *optional* dependency precisely so this
by-name coupling does not become a hard one.

## Provider-starter pattern

Each starter carries `<Provider>Properties` (`@ConfigurationProperties`, 28 of 29 — `multiprovider` has
none), a `<Provider>Customizer` functional interface (26 of 29 — absent in `envvar`, `jsonlogic` and
`multiprovider`), and an `autoconfigure.<provider>.<Provider>AutoConfiguration` gated by
`@ConditionalOnClass(<vendor provider>)` plus `@ConditionalOnProperty(prefix, name = "enabled",
havingValue = "true", matchIfMissing = true)`, with `@Bean @ConditionalOnMissingBean` on both the vendor
options/config and the `FeatureProvider`. Auto-config classes are registered in
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (one class per starter
module; the core modules register their own two and three classes respectively).

The behavioral contract — required-property fail-fast, `@ConditionalOnMissingBean` back-off when a consumer
supplies its own `FeatureProvider`, customizer ordering — is specified in
[provider-starter-contract.md](provider-starter-contract.md) and not duplicated here.

## Extension points

| Interface | Applied by | Use |
| --- | --- | --- |
| `ClientCustomizer` (`customize(Client)`) | `ClientAutoConfiguration.client()` and `MultiProviderAutoConfiguration.multiClient()`, via `ObjectProvider.orderedStream()` | Add SDK hooks or client-level config |
| `OpenFeatureAPICustomizer` (`customize(OpenFeatureAPI)`) | `OpenFeatureAPIAutoConfiguration.openFeatureAPI()`, ordered stream | Register hooks / event handlers on the global API. Built-ins: `loggerOpenFeatureAPICustomizer` (`@Order(HIGHEST_PRECEDENCE)`) and the security hook |
| `<Provider>Customizer` | Inside the provider's own options/config `@Bean`, applied last before the vendor object is built | Anything the properties do not cover (builders rarely have flat knobs) |

`OpenFeatureAPI.getInstance()` is process-wide state. All registration funnels through these auto-configs;
registering a provider from user code or a test can silently change what the `Client` bean serves.

## `@ToggleOnFlag`

`@ToggleOnFlag` (`METHOD`, `TYPE`) is intercepted by `ToggleOnFlagAspect` on the pointcut
`@annotation(...) || @within(...)`; a method-level annotation wins over a class-level one.

| Attribute | Default | As implemented |
| --- | --- | --- |
| `key()` | required | Flag key. Evaluated with `client.getBooleanValue(key, false)`, so a missing flag or a down provider means **false** |
| `attributes()` | `"{}"` | A SpEL map, e.g. `{'userId': #id}`. Evaluated in a `StandardEvaluationContext` (full SpEL) with the invoked method's parameter names as variables, and each entry wrapped in `Value`. A non-Map result raises `IllegalArgumentException`; a duplicate key raises `IllegalStateException`. The map becomes an `ImmutableContext` passed to the evaluation |
| `orElse()` | `""` | Name of a fallback method on the same target bean, looked up reflectively with the *original* parameter types and invoked with the same arguments. A signature mismatch therefore fails at runtime, not compile time |

Decision path: flag true → `joinPoint.proceed()`. Flag false with a non-empty `orElse` → the fallback's
return value. **Flag false with no `orElse` → the original method still proceeds** — declaring the
annotation is not by itself a gate; a `false` flag only suppresses the method when a fallback exists.

The aspect is `@Aspect @Component @ConditionalOnBean(Client.class)`, imported by `AspectAutoConfiguration`.
SpEL parameter names resolve only if the *consumer's* build compiles with `-parameters` (Spring Boot's
parent POM sets it; only `spring-boot-openfeature-autoconfigure` sets it inside this repo).

## Health indicator and security integration

`OpenFeatureHealthIndicator` (`@ConditionalOnBean(OpenFeatureAPI.class)`, imported by
`HealthAutoConfiguration`) reports on the live provider:

| Provider state | Actuator status | Detail |
| --- | --- | --- |
| `NoOpProvider` (nothing registered) | `DOWN` | `error: No active OpenFeature provider` |
| `READY` | `UP` | `provider: <simple class name>` |
| `NOT_READY`, `STALE` | `OUT_OF_SERVICE` | `provider: <simple class name>` |
| `ERROR`, `FATAL` | `DOWN` | `provider: <simple class name>` |
| Any exception during the check | `DOWN` | `error: <message>` |

`SecurityAutoConfiguration` activates on `@ConditionalOnClass(SecurityContextHolder.class)` and contributes
an `OpenFeatureAPICustomizer` that registers an SDK `Hook`. Its `before()` reads
`SecurityContextHolder.getContext().getAuthentication()`; when there is an authentication with a non-null
principal it returns an `ImmutableContext` with `userId` = `authentication.getName()` and `authorities` =
the list of authority strings, which the SDK then uses as the evaluation context for that call. With no
authentication it returns `Optional.empty()` and leaves the context untouched.

## Examples reactor

`examples/` is a **separate Maven reactor**: its parent is `spring-boot-starter-parent` (4.1.1), not the
root aggregator POM, and it has its own `${revision}` (currently `4.1.4-SNAPSHOT`) feeding
`spring-boot-openfeature.version`. The root POM only mounts it under `-P examples`; CI does not build it.
Consequences:

- Examples resolve the starters **by version from a repository**, not from the reactor, so run
  `./mvnw install` (or publish snapshots) at the version the examples ask for first. They read
  `central-portal-snapshots` and `jitpack.io`.
- Build with `./mvnw -P examples clean package` or `cd examples && mvn clean package`.

The 30 modules are 29 `<provider>-simple` apps (one per starter) plus `unleash-advanced`. All are WebFlux
apps with springdoc OpenAPI, and all expose the same shapes under `/feature`: `GET /feature/{name}` (raw
`Client` lookup), `GET /feature/greet/{name}` (a `@ToggleOnFlag` method with an `orElse`), and a toggle
endpoint that flips a flag at runtime — `toggle/{name}` everywhere except the two Unleash apps, which use
`toggle?name=`. `unleash-advanced` adds a
`@ToggleOnFlag(attributes = "{'userId': #id}")` example, an `OpenFeatureAPICustomizer` that installs
logging hooks, and a `MockWebServer` standing in for the Unleash API; it is also the GraalVM native-image
target (`native.sh` / `native.ps1`).

Every `-simple` app ships an in-memory fake `FeatureProvider` gated on `example.fake-provider=true`
(`Fake<X>ProviderConfig`, `@Bean @Primary FeatureProvider`). Because the starters' provider beans are
`@ConditionalOnMissingBean` on the `FeatureProvider` type, the fake makes the provider auto-config back off
entirely, so the app boots with no credentials or vendor SDK traffic. Run it with, e.g.,
`--spring-boot.run.arguments=--example.fake-provider=true`.

## Related documents

- [provider-starter-contract.md](provider-starter-contract.md) — the behavioral contract every starter honors
- [quality-and-testing.md](quality-and-testing.md) — the per-module JaCoCo instruction ratchet and the test conventions
- [dependency-management.md](dependency-management.md) — the BOM and the SDK/contrib lockstep rule
- [dependency-versions.md](dependency-versions.md) — generated current-vs-pinned version audit
- [backlog.md](backlog.md) — deferred work, including the blocked provider candidates
