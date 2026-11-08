# flagd-simple

Minimal Spring Boot WebFlux app wired to the Flagd starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without flagd (default demo mode)

Boots with an in-memory provider and no flagd endpoint:

```bash
java -jar target/flagd-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeFlagdProviderConfig` registers a `@Primary` `FeatureProvider`, so the flagd
auto-configuration backs off entirely and the gRPC endpoint is never contacted.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against a real flagd instance

Drop the switch and point at your flagd server:

```bash
export FLAGD_HOST=localhost
export FLAGD_PORT=8015
java -jar target/flagd-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.flagd.*` from `application.yaml`: `resolver-type`
(`RPC` or `IN_PROCESS`), `deadline` (ms), and optionally `offline-flag-source-path` for
offline mode.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
