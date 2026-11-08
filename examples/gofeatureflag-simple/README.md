# gofeatureflag-simple

Minimal Spring Boot WebFlux app wired to the GO Feature Flag starter, evaluating flags
through the OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without GO Feature Flag (default demo mode)

Boots with an in-memory provider and no relay proxy:

```bash
java -jar target/gofeatureflag-simple-4.1.2-SNAPSHOT.jar --example.fake-provider=true
```

`FakeGoFeatureFlagProviderConfig` registers a `@Primary` `FeatureProvider`, so the
GO Feature Flag auto-configuration backs off entirely and the relay proxy is never
contacted.
`random-boolean-flag` starts enabled; `toggle` flips any key.

## Running against a real GO Feature Flag relay proxy

Drop the switch and point at your relay proxy:

```bash
export GOFEATUREFLAG_ENDPOINT=https://mydomain.com/gofeatureflagproxy/
java -jar target/gofeatureflag-simple-4.1.2-SNAPSHOT.jar
```

The starter reads `spring.openfeature.gofeatureflag.*` from `application.yaml`:
`endpoint` (mandatory) and optionally `api-key` for authenticated relay proxies
(v1.7.0+).

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
