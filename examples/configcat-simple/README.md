# configcat-simple

Minimal Spring Boot WebFlux app wired to the ConfigCat starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without ConfigCat (default demo mode)

Boots with an in-memory provider and no ConfigCat SDK key:

```bash
java -jar target/configcat-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeConfigCatProviderConfig` registers a `@Primary` `FeatureProvider`, so the ConfigCat
auto-configuration backs off entirely and the SDK key is never read.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real ConfigCat

Drop the switch and supply your SDK key:

```bash
export CONFIGCAT_SDK_KEY=<your-sdk-key>
java -jar target/configcat-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.config-cat.*` from `application.yaml`: `sdk-key`
and optionally `filename` for local flag overrides.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
