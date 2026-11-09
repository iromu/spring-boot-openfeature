# featbit-simple

Minimal Spring Boot WebFlux app wired to the FeatBit starter, evaluating flags through the
OpenFeature `Client` and the declarative `@ToggleOnFlag` annotation.

## Running without FeatBit (default demo mode)

Boots with an in-memory provider and no FeatBit token:

```bash
java -jar target/featbit-simple-4.1.3-SNAPSHOT.jar --example.fake-provider=true
```

`FakeFeatBitProviderConfig` registers a `@Primary` `FeatureProvider`, so the FeatBit
auto-configuration backs off entirely and the token is never required. `random-boolean-flag`
starts enabled; `toggle` flips any key.

## Running against real FeatBit

Drop the switch and supply an app token plus your environment's endpoints:

```bash
export FEATBIT_TOKEN=your-featbit-app-token
export FEATBIT_STREAMING_URL=wss://your-environment.streaming.featbit.co
export FEATBIT_EVENT_URL=https://your-environment.api.featbit.co
java -jar target/featbit-simple-4.1.3-SNAPSHOT.jar
```

The starter reads `spring.openfeature.featbit.*` from `application.yaml`: `token`,
`streaming-url` and `event-url` (all required — each FeatBit environment has its own endpoints,
obtained from the FeatBit dashboard), plus `offline` and `disable-events` (both default to
`false`). Without all three startup fails with a clear message naming the missing properties —
the starter validates eagerly, which is intentional.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/feature/{name}` | Boolean value for the flag, defaulting to `false` |
| GET | `/feature/toggle/{name}` | Flips the flag; only available in fake-provider mode |
| GET | `/feature/greet/{name}` | `Hello name` when `random-boolean-flag` is on, `HELLO name` via the `orElse` fallback when off |

Swagger UI is served at `/` on port `9999`.
