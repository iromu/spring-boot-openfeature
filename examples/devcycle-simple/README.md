# devcycle-simple

Minimal Spring Boot WebFlux app wired to the DevCycle starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without DevCycle (default demo mode)

Boots with an in-memory provider and no DevCycle service key:

```bash
java -jar target/devcycle-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeDevCycleProviderConfig` registers a `@Primary` `FeatureProvider`, so the
DevCycle auto-configuration backs off entirely and the service key is never required.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real DevCycle

Drop the switch and supply a server-side SDK key:

```bash
export DEV_CYCLE_SERVER_SIDE_KEY=dvc_server_your_project_key
java -jar target/devcycle-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.devcycle.*` from `application.yaml`:
`server-side-key` (required) and `base-url-override` (optional). Without
`DEV_CYCLE_SERVER_SIDE_KEY` startup fails with a clear `server-side-key` message — the
starter validates eagerly, which is intentional.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
