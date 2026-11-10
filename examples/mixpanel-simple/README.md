# mixpanel-simple

Minimal Spring Boot WebFlux app wired to the Mixpanel starter, evaluating flags through the
OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without Mixpanel (default demo mode)

Boots with an in-memory provider and no Mixpanel project token:

```bash
java -jar target/mixpanel-simple-4.1.4-SNAPSHOT.jar --example.fake-provider=true
```

`FakeMixpanelProviderConfig` registers a `@Primary` `FeatureProvider`, so the Mixpanel
auto-configuration backs off entirely and the token is never required. `random-boolean-flag`
starts enabled; `toggle` flips any key.

## Running against real Mixpanel

Drop the switch and supply your project token:

```bash
export MIXPANEL_PROJECT_TOKEN=your-mixpanel-project-token
java -jar target/mixpanel-simple-4.1.4-SNAPSHOT.jar
```

The starter reads `spring.openfeature.mixpanel.*` from `application.yaml`: `project-token`
(required — without it startup fails with a clear message naming the missing property),
plus the optional `api-host` (your region's Mixpanel API host, e.g. `api-eu.mixpanel.com`)
and `request-timeout-seconds`.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
