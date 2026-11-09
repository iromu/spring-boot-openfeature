# confidence-simple

Minimal Spring Boot WebFlux app wired to the Confidence starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without Confidence (default demo mode)

Boots with an in-memory provider and no Confidence credentials:

```bash
java -jar target/confidence-simple-4.1.4-SNAPSHOT.jar --example.fake-provider=true
```

`FakeConfidenceProviderConfig` registers a `@Primary` `FeatureProvider`, so the Confidence
auto-configuration backs off entirely and no credentials are required.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real Confidence

Drop the switch and supply your API token:

```bash
export CONFIDENCE_API_TOKEN=<your-api-token>
java -jar target/confidence-simple-4.1.4-SNAPSHOT.jar
```

The starter reads `spring.openfeature.confidence.*` from `application.yaml`: `api-token` and
optionally `host` / `port` (to point at a non-default flag-resolver endpoint).

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
