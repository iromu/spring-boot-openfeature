# multiprovider-simple

Minimal Spring Boot WebFlux app wired to the MultiProvider starter, combining multiple
feature providers behind a single `Client` using the OpenFeature `Client` and the
declarative `@ToggleOnFlag` annotation.

## Running without real providers (default demo mode)

Boots with an in-memory provider alongside the env-var provider:

```bash
java -jar target/multiprovider-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeMultiProviderConfig` registers a `@Primary` `FeatureProvider`, which the
`MultiProvider` wraps alongside the `EnvVarProvider` (also on the classpath). The
`FirstMatchStrategy` delegates to the first provider that resolves a flag.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running with real providers

Drop the switch and configure your providers via their respective starters. The
`MultiProvider` picks up all `FeatureProvider` beans and delegates evaluation
according to the configured `Strategy` (default: `FirstMatchStrategy`).

Add additional provider starters to `pom.xml` to extend the set of providers:

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-unleash</artifactId>
    <version>${spring-boot-openfeature.version}</version>
</dependency>
```

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
