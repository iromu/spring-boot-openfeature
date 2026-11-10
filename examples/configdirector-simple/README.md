# configdirector-simple

Minimal Spring Boot WebFlux app wired to the ConfigDirector starter, evaluating
flags through the OpenFeature `Client` and the declarative `@ToggleOnFlag`
annotation.

## Running without ConfigDirector (default demo mode)

Boots with an in-memory provider and no ConfigDirector credentials:

```bash
java -jar target/configdirector-simple-4.1.4-SNAPSHOT.jar --example.fake-provider=true
```

`FakeConfigDirectorProviderConfig` registers a `@Primary` `FeatureProvider`, so
the ConfigDirector auto-configuration backs off entirely and no credentials are
required. `random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real ConfigDirector

Drop the switch and provide a server SDK key for your environment:

```bash
export CONFIGDIRECTOR_API_KEY=<your-server-sdk-key>
# optionally point at a non-default endpoint:
export CONFIGDIRECTOR_URL=https://api.configdirector.com
java -jar target/configdirector-simple-4.1.4-SNAPSHOT.jar
```

The starter reads `spring.openfeature.configdirector.*` from `application.yaml`:
`api-key` (required) and `url` (optional endpoint).

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
