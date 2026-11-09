# datadog-simple

Minimal Spring Boot WebFlux app wired to the Datadog starter, evaluating flags through
the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without Datadog (default demo mode)

Boots with an in-memory provider and no Datadog credentials:

```bash
java -jar target/datadog-simple-4.1.3-SNAPSHOT.jar --example.fake-provider=true
```

`FakeDatadogProviderConfig` registers a `@Primary` `FeatureProvider`, so the Datadog
auto-configuration backs off entirely and no credentials are required.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real Datadog

Drop the switch and supply your credentials:

```bash
export DD_API_KEY=<your-api-key>
export DD_APP_KEY=<your-app-key>
export DD_SITE=datadoghq.com
java -agentpath:/path/to/dd-java-agent.jar -jar target/datadog-simple-4.1.3-SNAPSHOT.jar
```

The starter reads `spring.openfeature.datadog.*` from `application.yaml`: `api-key`,
`app-key`, `site` and optionally `environment` and `init-timeout`. The Datadog java
agent (1.66.0 or later) must be loaded in the JVM for agentless flag delivery.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
