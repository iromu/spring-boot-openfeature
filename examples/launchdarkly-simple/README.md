# launchdarkly-simple

Minimal Spring Boot WebFlux app wired to the LaunchDarkly starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without LaunchDarkly (default demo mode)

Boots with an in-memory provider and no LaunchDarkly service key:

```bash
java -jar target/launchdarkly-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeLaunchDarklyProviderConfig` registers a `@Primary` `FeatureProvider`, so the
LaunchDarkly auto-configuration backs off entirely and the service key is never required.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real LaunchDarkly

Drop the switch and supply a server-side SDK key:

```bash
export LAUNCHDARKLY_SERVICE_KEY=sk-your-project-1-your-key
java -jar target/launchdarkly-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.launchdarkly.*` from `application.yaml`:
`service-key` (required) and `offline` (defaults to `false`). Without `LAUNCHDARKLY_SERVICE_KEY`
startup fails with a clear `service-key` message — the starter validates eagerly, which is
intentional.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
