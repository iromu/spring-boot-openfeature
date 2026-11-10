# cloudbees-simple

Minimal Spring Boot WebFlux app wired to the CloudBees starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without CloudBees (default demo mode)

Boots with an in-memory provider and no CloudBees credentials:

```bash
java -jar target/cloudbees-simple-4.1.4-SNAPSHOT.jar --example.fake-provider=true
```

`FakeCloudbeesProviderConfig` registers a `@Primary` `FeatureProvider`, so the
CloudBees auto-configuration backs off entirely and no credentials are required.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real CloudBees

Drop the switch and supply your app key:

```bash
export CLOUDBEES_API_KEY=<your-app-key>
java -jar target/cloudbees-simple-4.1.4-SNAPSHOT.jar
```

The starter reads `spring.openfeature.cloudbees.*` from `application.yaml`:
`api-key` (required) and the optional `endpoint`.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
