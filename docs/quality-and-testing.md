# Quality & Testing

How this project is tested and how the build enforces its quality floor. For module layout see [architecture.md](architecture.md); for what each provider starter must expose see [provider-starter-contract.md](provider-starter-contract.md); for version pinning see [dependency-management.md](dependency-management.md).

## Testing tiers and conventions

All tests run under `spring-boot-starter-test` (JUnit 5, AssertJ, Mockito). Source layout mirrors `src/main/java` under `src/test/java`.

| Tier | Where | Technique | Credentials needed |
| --- | --- | --- | --- |
| Auto-config contract tests | every provider + core module | `ApplicationContextRunner` with `AutoConfigurations.of(...)`, AssertJ context assertions (`hasSingleBean`, `doesNotHaveBean`) | None — HTTP providers stub the network with OkHttp `MockWebServer` (e.g. `UnleashAutoConfigurationTest`) |
| Unit tests | `spring-openfeature`, provider helpers | Plain JUnit + Mockito (e.g. aspect and `FakeUnleashProviderTest`) | None |
| Real-endpoint tests | `UnleashEndpointIntegrationTest` (unleash module) | Drives the provider against a live Unleash server (`src/test/resources/unleash/docker-compose.integration.yml`), authoring flags via the admin API and reading them back through the SDK | Local Docker; **self-skips** via JUnit `Assumptions` when no endpoint answers `/health` |
| Example smoke tests | `examples/` (built only under `-P examples`) | Each `*-simple` app ships a fake in-memory `FeatureProvider` config so the app boots with no vendor account; most are gated on `@ConditionalOnProperty(name = "example.fake-provider", havingValue = "true")` (`unleash-simple` predates the pattern and registers its `@Primary` fake unconditionally) | None |

The example apps are not built by CI (no `-P examples`), so example-level smoke testing is a manual `cd examples && mvn clean package` exercise; the fake-provider gate exists precisely so that exercise needs no API keys.

## Behavioral contract

These requirements are the normative contract the test suite must keep proving:

- **Coverage floor.** The build MUST fail the `verify` phase when any module's instruction-coverage ratio drops below its recorded per-module floor, and MUST pass when every module meets or exceeds it.
- **Provider enabled-gate.** Each backend provider auto-config MUST create its configuration and `FeatureProvider` beans when its `enabled` property is true or unset, and MUST NOT create them when it is false.
  - **`multiprovider` is exempt.** It is a composite aggregator over the providers it already aggregates — enablement is governed by those providers, it has never exposed an `enabled` property, and adding one would change provider runtime behavior and the public configuration surface. (`envvar` and `jsonlogic` do expose the gate and remain covered.)
- **Back-off to user beans.** When the application defines its own `FeatureProvider` bean, auto-config MUST NOT register a competing provider bean.
- **Customizers applied.** Where a provider exposes a customizer extension point, a registered customizer bean MUST be applied to the provider configuration before it is built.
- **`@ToggleOnFlag` decision semantics.** The aspect MUST invoke the target method when the flag resolves to true; MUST invoke the declared `orElse` fallback when the flag is false and a fallback is present; and MUST proceed with the target method when the flag is false and no fallback is declared.
- **`@ToggleOnFlag` attribute validation.** When the SpEL `attributes()` expression resolves to a map with a duplicate key, or does not resolve to a map at all, the aspect MUST raise a clear error rather than silently proceeding.
- **Lifecycle events wired.** The default `OpenFeatureAPI` customizer (`OpenFeatureAPIAutoConfiguration`) MUST register handlers for the provider ready, error, stale, and configuration-changed events.

## Coverage gate mechanism

JaCoCo is configured in the root `pom.xml` `pluginManagement` (plugin version 0.8.16) with three executions:

| Execution | Phase | Effect |
| --- | --- | --- |
| `prepare-agent` | default | attaches the coverage agent (`target/jacoco.exec`) |
| `report` | `verify` | per-module HTML/XML report under `target/site/jacoco` |
| `check` | `verify` | `haltOnFailure=true`; one rule: element `BUNDLE`, counter `INSTRUCTION`, value `COVEREDRATIO`, minimum `${jacoco.instruction.minimum}` |

The root pom sets `<jacoco.instruction.minimum>0.0</jacoco.instruction.minimum>` so the gate is green everywhere by default; each code module overrides the property in its own `<properties>` with the ratio it actually achieves. This is a **ratchet**: raise the floor when you add tests, and any regression below the recorded value fails `verify`.

### Per-module floors (verbatim from each module's pom)

| Module | Floor | Module | Floor |
| --- | --- | --- | --- |
| spring-openfeature | 1.00 | launchdarkly | 1.00 |
| spring-boot-openfeature-autoconfigure | 0.97 | datadog | 1.00 |
| configcat | 0.72 | split | 1.00 |
| envvar | 1.00 | devcycle | 1.00 |
| flagd | 0.89 | featbit | 1.00 |
| flagsmith | 1.00 | featureflip | 1.00 |
| flipt | 1.00 | mixpanel | 1.00 |
| gofeatureflag | 0.91 | kameleoon | 1.00 |
| jsonlogic | 0.94 | confidence | 1.00 |
| multiprovider | 0.87 | octopusdeploy | 1.00 |
| ofrep | 1.0 | vwo | 1.00 |
| statsig | 1.00 | superposition | 1.00 |
| unleash | 0.90 | ageris | 1.00 |
| growthbook | 0.94 | cloudbees | 1.00 |
| gcp | 1.00 | configdirector | 1.00 |
| optimizely | 1.0 | | |

(provider module names above are abbreviated to `spring-boot-starter-openfeature-<name>`.)

The two non-code modules — `spring-boot-starter-openfeature` (aggregating starter) and `spring-boot-openfeature-dependencies` (BOM) — carry no override and stay at the root default `0.0`; they contain no instructions to cover.

## CI and known gaps

`.github/workflows/pull_request.yml` runs `./mvnw -B --no-transfer-progress clean verify` on `ubuntu-latest` with Temurin JDK 17, then uploads `**/jacoco.xml` as an artifact and reports to Coveralls. The per-module `check` gate therefore runs on every PR. Two gaps:

- **The aggregate report is not built in CI.** The SonarCloud steps are commented out and CI does not pass `-P sonar`, so the `aggregate-report` module (which builds the cross-module `report-aggregate` JaCoCo report for Sonar) is only exercised by local `./mvnw -P sonar clean verify`.
- **The Unleash real-endpoint gate is vacuous in CI.** `UnleashEndpointIntegrationTest` self-skips (`Assumptions.assumeTrue(isEndpointUp(), …)`) and CI never starts the docker-compose fixture, so no PR ever exercises the live-server assertions. The unstarted plan to make it self-provisioning is tracked in [backlog.md](backlog.md).
