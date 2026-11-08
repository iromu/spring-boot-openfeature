# statsig-simple

Minimal Spring Boot WebFlux app wired to the Statsig starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without Statsig (default demo mode)

Boots with an in-memory provider and no Statsig SDK key:

```bash
java -jar target/statsig-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeStatsigProviderConfig` registers a `@Primary` `FeatureProvider`, so the Statsig
auto-configuration backs off entirely and the SDK key is never read.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real Statsig

Drop the switch and supply your SDK key:

```bash
export STATSIG_SDK_KEY=<your-sdk-key>
java -jar target/statsig-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.statsig.*` from `application.yaml`: `sdk-key`
and `local-mode` (set to `true` to use Statsig's local evaluation mode).

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
