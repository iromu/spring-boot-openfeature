# flipt-simple

Minimal Spring Boot WebFlux app wired to the Flipt starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without Flipt (default demo mode)

Boots with an in-memory provider and no Flipt endpoint:

```bash
java -jar target/flipt-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeFliptProviderConfig` registers a `@Primary` `FeatureProvider`, so the Flipt
auto-configuration backs off entirely and the Flipt server is never contacted.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real Flipt

Drop the switch and point at your Flipt server:

```bash
export FLIPT_URL=http://localhost:8080
java -jar target/flipt-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.flipt.*` from `application.yaml`: `base-url`,
`namespace`, `timeout`, and optionally `headers` for authentication.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
