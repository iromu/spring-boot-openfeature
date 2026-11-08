# growthbook-simple

Minimal Spring Boot WebFlux app wired to the GrowthBook starter, evaluating flags
through the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without GrowthBook (default demo mode)

Boots with an in-memory provider and no GrowthBook client key:

```bash
java -jar target/growthbook-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeGrowthBookProviderConfig` registers a `@Primary` `FeatureProvider`, so the
GrowthBook auto-configuration backs off entirely and the client key is never read.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real GrowthBook

Drop the switch and supply your client key:

```bash
export GROWTHBOOK_CLIENT_KEY=<your-client-key>
java -jar target/growthbook-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.growthbook.*` from `application.yaml`:
`api-host`, `client-key`, `cache-mode`, `is-qa-mode`, and optionally `decryption-key`,
`cache-directory`, `refresh-strategy`, and `global-forced-variations-map`.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
