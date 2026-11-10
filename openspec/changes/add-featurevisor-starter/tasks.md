## 1. Premises and feasibility gate

- [x] 1.1 Resolve the Featurevisor OpenFeature provider from Maven Central, inspect its API surface. Record findings in design.md. — **Resolved and BLOCKED:** `com.featurevisor:featurevisor-openfeature:5.0.0` is published to GitHub Packages only (auth required; not on Maven Central under any group). Findings and exit options recorded in design.md; the API surface cannot be inspected because the jar is unresolvable without a GitHub token.
- [ ] 1.2 Determine whether the provider constructor is lazy or eagerly connects. Record in design.md. — **Blocked** by 1.1 (jar unresolvable on this host).
- [ ] 1.3 Prove lockstep: `./mvnw -B -pl spring-boot-starter-openfeature-featurevisor dependency:list` shows `dev.openfeature:sdk` = `sdk.version`. — **Blocked** by 1.1 (module cannot build until the artifact is resolvable).

## 2. Build wiring

- [ ] 2.1 Root `pom.xml`: `<featurevisor-openfeature.version>` + `<module>`; BOM managed row.
- [ ] 2.2 Module pom mirroring the unleash reference.

## 3. Starter classes

- [ ] 3.1 Write `FeaturevisorProperties` (`spring.openfeature.featurevisor` prefix; `enabled=true`, `apiKey`) and `FeaturevisorCustomizer`.
- [ ] 3.2 Write `FeaturevisorAutoConfiguration`: family-standard gates, options bean, provider bean. Register in `AutoConfiguration.imports`.

## 4. Test tier

- [ ] 4.1 `FeaturevisorAutoConfigurationTest` contract tests: disabled, back-off, customizer, default bean.
- [ ] 4.2 Prove the credential-free guarantee.

## 5. Coverage floor and full gate

- [ ] 5.1 Measure coverage, set `jacoco.instruction.minimum`.
- [ ] 5.2 Full gate `./mvnw -B -Psonar clean verify`.
