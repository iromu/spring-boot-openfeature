# kameleoon-simple

Minimal Spring Boot WebFlux app wired to the Kameleoon starter, evaluating flags through the
OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without Kameleoon (default demo mode)

Boots with an in-memory provider and no Kameleoon credentials:

```bash
java -jar target/kameleoon-simple-4.1.4-SNAPSHOT.jar --example.fake-provider=true
```

`FakeKameleoonProviderConfig` registers a `@Primary` `FeatureProvider`, so the Kameleoon
auto-configuration backs off entirely and the site code is never required. `random-boolean-flag`
starts enabled; `toggle` flips any key.

## Running against real Kameleoon

Drop the switch and supply a site code plus client credentials:

```bash
export KAMELEOON_SITE_CODE=your-kameleoon-site-code
export KAMELEOON_CLIENT_ID=your-kameleoon-client-id
export KAMELEOON_CLIENT_SECRET=your-kameleoon-client-secret
java -jar target/kameleoon-simple-4.1.4-SNAPSHOT.jar
```

The starter reads `spring.openfeature.kameleoon.*` from `application.yaml`: `site-code`,
`client-id` and `client-secret` (all required), plus optional `environment`,
`top-level-domain`, `refresh-interval`, `session-duration`, `default-timeout` and
`tracking-interval`. Without all three required properties startup fails with a clear message
naming the missing properties — the starter validates eagerly, which is intentional.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
