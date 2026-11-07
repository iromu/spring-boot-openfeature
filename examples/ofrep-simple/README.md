# ofrep-simple

Minimal Spring Boot WebFlux app wired to the OFREP starter, evaluating flags through the
OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without an OFREP endpoint (default demo mode)

Boots with an in-memory provider and no remote-evaluation server:

```bash
java -jar target/ofrep-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeOfrepProviderConfig` registers a `@Primary` `FeatureProvider`, so the OFREP
auto-configuration backs off entirely and the endpoint is never contacted.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against a real OFREP endpoint

Drop the switch and supply the endpoint:

```bash
export OFREP_BASE_URL=https://flags.example.com/ofrep
java -jar target/ofrep-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.ofrep.*` from `application.yaml`: `base-url`
(required), `connect-timeout`, `request-timeout`, and optional `headers`. The provider
is built via the OFREP `constructProvider` factory, which validates the base URL — so
without `OFREP_BASE_URL` startup fails with an `IllegalArgumentException`, which is
intentional.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
