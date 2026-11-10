# featureflip-simple

Minimal Spring Boot WebFlux app wired to the Featureflip starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without Featureflip (default demo mode)

Boots with an in-memory provider and no Featureflip credentials:

```bash
java -jar target/featureflip-simple-4.1.3-SNAPSHOT.jar --example.fake-provider=true
```

`FakeFeatureflipProviderConfig` registers a `@Primary` `FeatureProvider`, so the Featureflip
auto-configuration backs off entirely and no credentials are required.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real Featureflip

Drop the switch and supply your SDK key:

```bash
export FEATUREFLIP_API_KEY=<your-sdk-key>
java -jar target/featureflip-simple-4.1.3-SNAPSHOT.jar
```

The starter reads `spring.openfeature.featureflip.*` from `application.yaml`: `api-key` and
optionally `base-url` (for self-hosted deployments).

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
