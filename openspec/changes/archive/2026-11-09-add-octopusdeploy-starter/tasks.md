## 1. Premises and feasibility gate

- [ ] 1.1 Resolve the Octopus Deploy OpenFeature provider from Maven Central, inspect its API surface. Record findings in design.md.
- [ ] 1.2 Determine whether the provider constructor is lazy or eagerly connects. Record in design.md.
- [ ] 1.3 Prove lockstep: `./mvnw -B -pl spring-boot-starter-openfeature-octopusdeploy dependency:list` shows `dev.openfeature:sdk` = `sdk.version`.

## 2. Build wiring

- [ ] 2.1 Root `pom.xml`: `<octopusdeploy-openfeature.version>` + `<module>`; BOM managed row.
- [ ] 2.2 Module pom mirroring the unleash reference.

## 3. Starter classes

- [ ] 3.1 Write `OctopusDeployProperties` (`spring.openfeature.octopusdeploy` prefix; `enabled=true`, `apiKey`, `serverUrl`) and `OctopusDeployCustomizer`.
- [ ] 3.2 Write `OctopusDeployAutoConfiguration`: family-standard gates, options bean, provider bean. Register in `AutoConfiguration.imports`.

## 4. Test tier

- [ ] 4.1 `OctopusDeployAutoConfigurationTest` contract tests: disabled, back-off, customizer, default bean.
- [ ] 4.2 Prove the credential-free guarantee.

## 5. Coverage floor and full gate

- [ ] 5.1 Measure coverage, set `jacoco.instruction.minimum`.
- [ ] 5.2 Full gate `./mvnw -B -Psonar clean verify`.
