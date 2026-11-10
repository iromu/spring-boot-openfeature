## 1. Premises and feasibility gate

- [x] 1.1 Resolve the Hyphen AI OpenFeature provider from Maven Central, inspect its API surface. Record findings in design.md. — **Resolved and BLOCKED:** `dev.hyphen:openfeature-provider-java:1.0.0` is not on Maven Central (HTTP 404 for metadata/POM/JAR; 0 search-index hits) and the vendor repo shows no release pipeline (no tags, no CI, no `distributionManagement`). Findings and exit options recorded in design.md; the API surface was inspected from the `provider` branch source instead of the jar.
- [ ] 1.2 Determine whether the provider constructor is lazy or eagerly connects. Record in design.md. — **Blocked** by 1.1 (jar unresolvable on this host); answered from source in design.md — the constructor is eager.
- [ ] 1.3 Prove lockstep: `./mvnw -B -pl spring-boot-starter-openfeature-hyphen dependency:list` shows `dev.openfeature:sdk` = `sdk.version`. — **Blocked** by 1.1 (module cannot build until the artifact is resolvable).

## 2. Build wiring

- [ ] 2.1 Root `pom.xml`: `<hyphen-openfeature.version>` + `<module>`; BOM managed row.
- [ ] 2.2 Module pom mirroring the unleash reference.

## 3. Starter classes

- [ ] 3.1 Write `HyphenProperties` (`spring.openfeature.hyphen` prefix; `enabled=true`, `apiKey`) and `HyphenCustomizer`.
- [ ] 3.2 Write `HyphenAutoConfiguration`: family-standard gates, options bean, provider bean. Register in `AutoConfiguration.imports`.

## 4. Test tier

- [ ] 4.1 `HyphenAutoConfigurationTest` contract tests: disabled, back-off, customizer, default bean.
- [ ] 4.2 Prove the credential-free guarantee.

## 5. Coverage floor and full gate

- [ ] 5.1 Measure coverage, set `jacoco.instruction.minimum`.
- [ ] 5.2 Full gate `./mvnw -B -Psonar clean verify`.
