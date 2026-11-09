## 1. Premises and feasibility gate

- [x] 1.1 Resolve `com.split:split-openfeature-provider-java` from Maven Central, inspect its API surface via `javap` or source. Record findings in design.md.
- [x] 1.2 Determine whether the provider constructor is lazy or eagerly connects. Record in design.md.
- [x] 1.3 Prove lockstep: `./mvnw -B -pl spring-boot-starter-openfeature-split dependency:list` shows `dev.openfeature:sdk` = `sdk.version`.

## 2. Build wiring

- [x] 2.1 Root `pom.xml`: `<split-openfeature.version>` + `<module>`; BOM managed row.
- [x] 2.2 Module pom mirroring the unleash reference.

## 3. Starter classes

- [x] 3.1 Write `SplitProperties` (`spring.openfeature.split` prefix; `enabled=true`, `authorizationKey`, optional domain) and `SplitCustomizer`.
- [x] 3.2 Write `SplitAutoConfiguration`: family-standard gates, options bean, provider bean. Register in `AutoConfiguration.imports`.

## 4. Test tier

- [x] 4.1 `SplitAutoConfigurationTest` contract tests: disabled, back-off, customizer, default bean.
- [x] 4.2 Prove the credential-free guarantee.

## 5. Coverage floor and full gate

- [x] 5.1 Measure coverage, set `jacoco.instruction.minimum`.
- [x] 5.2 Full gate `./mvnw -B -Psonar clean verify`.
