# ageris-simple

Minimal Spring Boot WebFlux app wired to the Ageris starter, evaluating flags
through the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without Ageris (default demo mode)

Boots with an in-memory provider and no Ageris configuration files:

```bash
java -jar target/ageris-simple-4.1.4-SNAPSHOT.jar --example.fake-provider=true
```

`FakeAgerisProviderConfig` registers a `@Primary` `FeatureProvider`, so the Ageris
auto-configuration backs off entirely and no configuration files are required.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against real Ageris

Drop the switch and point the starter at a base directory; Ageris reads the
files from `<path>/<application>/`:

```bash
export AGERIS_CONFIG_PATH=/etc/my-app
# featureflags.json must live at /etc/my-app/AgerisApplication/featureflags.json
java -jar target/ageris-simple-4.1.4-SNAPSHOT.jar
```

The starter reads `spring.openfeature.ageris.*` from `application.yaml`:
`application`, `path` and `feature-flag-files` (plus optionally
`json-config-files`, `targeting-key-type`, `refresh-interval` and
`first-load-timeout`). Ageris refreshes the files on the configured interval;
boolean lookups are answered from the feature-flag files.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
