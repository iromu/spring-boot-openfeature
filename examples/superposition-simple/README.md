# superposition-simple

Minimal Spring Boot WebFlux app wired to the Superposition starter, evaluating flags through the
OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without Superposition (default demo mode)

Boots with an in-memory provider and no Superposition credentials:

```bash
java -jar target/superposition-simple-4.1.4-SNAPSHOT.jar --example.fake-provider=true
```

`FakeSuperpositionProviderConfig` registers a `@Primary` `FeatureProvider`, so the Superposition
auto-configuration backs off entirely and the credentials are never required. `random-boolean-flag`
starts enabled; `toggle` flips any key.

## Running against real Superposition

Drop the switch and supply the API token plus organization and workspace IDs:

```bash
export SUPERPOSITION_API_KEY=your-superposition-api-token
export SUPERPOSITION_ORG_ID=your-superposition-org-id
export SUPERPOSITION_WORKSPACE_ID=your-superposition-workspace-id
java -jar target/superposition-simple-4.1.4-SNAPSHOT.jar
```

The starter reads `spring.openfeature.superposition.*` from `application.yaml`: `api-key`,
`org-id` and `workspace-id` (all required), plus optional `endpoint`, `refresh-timeout-millis`
and `refresh-interval-millis`. Without all three required properties startup fails with a clear
message naming the missing properties — the starter validates eagerly, which is intentional.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
