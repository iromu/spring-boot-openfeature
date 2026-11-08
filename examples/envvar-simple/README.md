# envvar-simple

Minimal Spring Boot WebFlux app wired to the EnvVar starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without env vars (default demo mode)

Boots with an in-memory provider and no environment variables:

```bash
java -jar target/envvar-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeEnvVarProviderConfig` registers a `@Primary` `FeatureProvider`, so the EnvVar
auto-configuration backs off entirely.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real environment variables

Drop the switch and set env vars matching your flag keys:

```bash
export random-boolean-flag=true
export my-other-flag=false
java -jar target/envvar-simple-4.1.2-SNAPSHOT.jar
```

The `EnvVarProvider` reads flag values directly from the process environment. The
key transformer (default: identity) maps flag keys to environment variable names.
Boolean values are parsed from `true`/`false` strings.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
