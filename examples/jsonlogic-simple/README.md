# jsonlogic-simple

Minimal Spring Boot WebFlux app wired to the JSON Logic starter, evaluating flags
through the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without a rules file (default demo mode)

Boots with an in-memory provider and no JSON Logic rules:

```bash
java -jar target/jsonlogic-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeJsonlogicProviderConfig` registers a `@Primary` `FeatureProvider`, so the JSON
Logic auto-configuration backs off entirely and no rules file is read.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running with JSON Logic rules

Drop the switch and point at a rules file:

```bash
java -jar target/jsonlogic-simple-4.1.2-SNAPSHOT.jar --spring.openfeature.json-logic.filename=classpath:rules.json
```

The starter reads `spring.openfeature.json-logic.filename` from `application.yaml`.
The rules file maps flag keys to JSON Logic expressions evaluated against the
`EvaluationContext`. A sample `rules.json` is included in `src/main/resources`.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
