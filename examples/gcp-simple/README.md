# gcp-simple

Minimal Spring Boot WebFlux app wired to the GCP starter, evaluating flags through the
OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without GCP (default demo mode)

Boots with an in-memory provider and no Google credentials:

```bash
java -jar target/gcp-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeGcpProviderConfig` registers a `@Primary` `FeatureProvider`, so the GCP
auto-configuration backs off entirely and the service-account path is never read.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real GCP

Drop the switch and supply a project:

```bash
export GCP_PROJECT_ID=<your-project>
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json   # optional; unset falls back to ADC
java -jar target/gcp-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.gcp.*` from `application.yaml`: `type`
(`secretmanager` or `parametermanager`) and `project-id`, with `service-account-json`
available commented out. Flags resolve from Secret Manager secret names, prefixed by
`name-prefix` when set, so a `true` answer needs a matching secret holding `true`.

Without `GCP_PROJECT_ID` startup fails with `GcpProviderOptions: projectId must not be
blank` — the provider validates eagerly at `initialize()`, which is intentional.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
