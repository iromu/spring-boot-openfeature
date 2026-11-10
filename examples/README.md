# examples

Runnable sample applications that exercise the starters end-to-end. Each `<provider>-simple` app is a
minimal Spring Boot web app that evaluates flags through the auto-configured `Client`; `unleash-advanced`
is a richer Unleash example.

There are 30 modules, one per supported provider plus the advanced Unleash app:
`unleash-simple`, `unleash-advanced`, `configcat-simple`, `envvar-simple`, `flagd-simple`,
`flagsmith-simple`, `flipt-simple`, `gcp-simple`, `gofeatureflag-simple`, `growthbook-simple`,
`jsonlogic-simple`, `multiprovider-simple`, `ofrep-simple`, `optimizely-simple`, `statsig-simple`,
`launchdarkly-simple`, `datadog-simple`, `split-simple`, `devcycle-simple`, `featbit-simple`,
`featureflip-simple`, `mixpanel-simple`, `kameleoon-simple`, `confidence-simple`, `octopusdeploy-simple`,
`vwo-simple`, `superposition-simple`, `ageris-simple`, `cloudbees-simple`, `configdirector-simple`.

## Building

The examples form their own Maven reactor (their parent is `spring-boot-starter-parent`, not the root
project POM), and are built with the `examples` profile:

```bash
# from the repository root
./mvnw -P examples clean package

# or build the reactor directly
cd examples && mvn clean package
```

## Running without a real backend

Most examples ship an in-memory fake provider so you can boot them without the corresponding
feature-flag service. Enable it with the `example.fake-provider` flag:

```bash
./mvnw -pl :configcat-simple spring-boot:run -Dspring-boot.run.arguments=--example.fake-provider=true
```

Otherwise each app reads its provider settings from `application.yaml` (typically via environment
variables, e.g. `CONFIGDIRECTOR_API_KEY`, `GCP_PROJECT_ID`).

## Native image

`unleash-advanced` is compiled to a GraalVM native image by the helper scripts in this directory
(require a GraalVM JDK):

- `native.sh` — Linux/macOS
- `native.ps1` — Windows
