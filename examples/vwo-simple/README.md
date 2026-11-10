# vwo-simple

Minimal Spring Boot WebFlux app wired to the VWO starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without VWO (default demo mode)

Boots with an in-memory provider and no VWO credentials:

```bash
java -jar target/vwo-simple-4.1.4-SNAPSHOT.jar --example.fake-provider=true
```

`FakeVwoProviderConfig` registers a `@Primary` `FeatureProvider`, so the VWO
auto-configuration backs off entirely and no credentials are required.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real VWO

Drop the switch and supply your account ID and API key:

```bash
export VWO_ACCOUNT_ID=<your-account-id>
export VWO_API_KEY=<your-api-key>
java -jar target/vwo-simple-4.1.4-SNAPSHOT.jar
```

The starter reads `spring.openfeature.vwo.*` from `application.yaml`:
`account-id` and `api-key` (both required).

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
