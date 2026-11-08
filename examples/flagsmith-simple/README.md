# flagsmith-simple

Minimal Spring Boot WebFlux app wired to the Flagsmith starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without Flagsmith (default demo mode)

Boots with an in-memory provider and no Flagsmith API key:

```bash
java -jar target/flagsmith-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeFlagsmithProviderConfig` registers a `@Primary` `FeatureProvider`, so the Flagsmith
auto-configuration backs off entirely and the API token is never read.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real Flagsmith

Drop the switch and supply your API key:

```bash
export FLAGSMITH_API_KEY=<your-server-side-sdk-token>
java -jar target/flagsmith-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.flagsmith.api-key` from `application.yaml`. The
API key is either an Environment API key or a Server Side SDK Token depending on your
evaluation mode.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
