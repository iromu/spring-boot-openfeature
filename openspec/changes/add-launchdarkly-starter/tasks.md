## 1. Premises and feasibility gate

- [ ] 1.1 Resolve `com.launchdarkly:openfeature-java-server` from Maven Central, inspect its API surface (provider class name, constructor signature, options/config type) via `javap` or source. Record findings in design.md.
- [ ] 1.2 Determine whether the provider constructor is lazy (no network) or eagerly connects. If eager, the happy-path test is back-off-only. Record in design.md.
- [ ] 1.3 Prove lockstep: with the module wired, `./mvnw -B -pl spring-boot-starter-openfeature-launchdarkly dependency:list` shows `dev.openfeature:sdk` = `sdk.version`.

## 2. Build wiring

- [ ] 2.1 Root `pom.xml`: `<launchdarkly-openfeature.version>` property + `<module>` entry; `spring-boot-openfeature-dependencies/pom.xml`: managed row for the provider coordinate.
- [ ] 2.2 Module pom mirroring the unleash reference (same parent/BOM imports, `spring-boot-openfeature-autoconfigure`, lombok provided, provider dependency unversioned, `spring-boot-starter-test` test, JaCoCo/Checkstyle sections).

## 3. Starter classes

- [ ] 3.1 Write `LaunchDarklyProperties` (`spring.openfeature.launchdarkly` prefix; `enabled=true`, `serviceKey`, plus any provider-specific options) and `LaunchDarklyCustomizer`.
- [ ] 3.2 Write `LaunchDarklyAutoConfiguration`: family-standard gates, `@ConditionalOnMissingBean` options bean, provider bean. Register in `AutoConfiguration.imports`.

## 4. Test tier

- [ ] 4.1 `LaunchDarklyAutoConfigurationTest` contract tests: disabled, back-off, customizer, default bean.
- [ ] 4.2 Prove the credential-free guarantee: run the module tests with no LaunchDarkly credentials available.

## 5. Coverage floor and full gate

- [ ] 5.1 Measure the module's instruction coverage, set `jacoco.instruction.minimum`.
- [ ] 5.2 Full gate `./mvnw -B -Psonar clean verify`: `BUILD SUCCESS`, all modules pass.
