# optimizely-simple

Minimal Spring Boot WebFlux app wired to the Optimizely starter, evaluating flags through the
OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without Optimizely (default demo mode)

Boots with an in-memory provider and no Optimizely credentials:

```bash
java -jar target/optimizely-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeOptimizelyProviderConfig` registers a `@Primary` `FeatureProvider`, so the Optimizely
auto-configuration backs off entirely and the project-config path is never used.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real Optimizely

Drop the switch and supply a project SDK key:

```bash
export OPTIMIZELY_SDK_KEY=<your-sdk-key>
# optional:
# export OPTIMIZELY_CONFIG_URL=https://config.optimizely.com/api/v4/optimizely/datafile.json
# export OPTIMIZELY_DATAFILE_AUTH_TOKEN=<token>
java -jar target/optimizely-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.optimizely.*` from `application.yaml`: either
`datafile` (a local datafile, fully offline) or `sdk-key` + `config-url` (remote project
config, with optional `datafile-auth-token` and `poll-interval`). Without `OPTIMIZELY_SDK_KEY`
(and no `datafile`) startup fails, which is intentional.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
