## 1. Core: AOP and autoconfigure (spring-openfeature + spring-boot-openfeature-autoconfigure)

- [x] 1.1 Remove the `System.out.println("Adding to SpEL context: ...")` line from `ToggleOnFlagAspect.resolveAttributesSpEL` and verify no `System.out` remains in `spring-openfeature` main sources and `./mvnw -pl spring-openfeature test` passes.
- [x] 1.2 Add `ToggleOnFlagAspect` edge-case tests using a mocked `Client`: flag-false with no fallback → proceed, flag-false with `orElse` → fallback invoked, SpEL duplicate-key → error, SpEL non-map result → error; verify `./mvnw -pl spring-openfeature test` passes and the previously-uncovered branches are hit (check `spring-openfeature/target/site/jacoco/jacoco.csv`).
- [x] 1.3 Add `ApplicationContextRunner` context tests for `AspectAutoConfiguration` and `HealthAutoConfiguration` and verify `./mvnw -pl spring-openfeature test` passes with both classes no longer at 0% in the JaCoCo CSV.
- [x] 1.4 Extend `OpenFeatureAPIAutoConfigurationTest` with a functional test that obtains the `OpenFeatureAPICustomizer` bean, calls `.customize(mockApi)` on a mocked `OpenFeatureAPI`, and fires the ready/error/stale/configuration-changed events; verify `./mvnw -pl spring-boot-openfeature-autoconfigure test` passes and `OpenFeatureAPIAutoConfiguration` coverage rises toward 100%.
- [x] 1.5 Add a new `ClientAutoConfigurationTest` covering the positive `Client` bean and the back-off case (user/multi-provider present → no auto `Client`); verify `./mvnw -pl spring-boot-openfeature-autoconfigure test` passes and `ClientAutoConfiguration` coverage rises.

## 2. Provider starters — conditional + back-off (+ customizer where it exists)

For each provider, extend the existing `*AutoConfigurationTest` with: (a) `enabled=false` → no `FeatureProvider` bean, (b) user-supplied `FeatureProvider` → `@ConditionalOnMissingBean` back-off. Providers marked *(customizer)* additionally add (c) a registered `*Customizer` that mutates the built config. Verify each with `./mvnw -pl <module> test`.

- [x] 2.1 configcat *(customizer)* — add the 3 assertions; verify `./mvnw -pl spring-boot-starter-openfeature-configcat test` passes and `ConfigCatAutoConfiguration` coverage rises.
- [x] 2.2 envvar — add assertions (a) and (b); verify `./mvnw -pl spring-boot-starter-openfeature-envvar test` passes.
- [x] 2.3 flagd *(customizer)* — add the 3 assertions; verify `./mvnw -pl spring-boot-starter-openfeature-flagd test` passes.
- [x] 2.4 flagsmith *(customizer)* — add the 3 assertions; verify `./mvnw -pl spring-boot-starter-openfeature-flagsmith test` passes.
- [x] 2.5 flipt *(customizer)* — add the 3 assertions; verify `./mvnw -pl spring-boot-starter-openfeature-flipt test` passes.
- [x] 2.6 gofeatureflag *(customizer)* — add the 3 assertions; verify `./mvnw -pl spring-boot-starter-openfeature-gofeatureflag test` passes.
- [x] 2.7 jsonlogic — add assertions (a) and (b); verify `./mvnw -pl spring-boot-starter-openfeature-jsonlogic test` passes.
- [x] 2.8 multiprovider — add assertions (a) and (b); verify `./mvnw -pl spring-boot-starter-openfeature-multiprovider test` passes.
- [x] 2.9 statsig *(customizer)* — add the 3 assertions; verify `./mvnw -pl spring-boot-starter-openfeature-statsig test` passes.
- [x] 2.10 unleash *(customizer)* — add the 3 assertions; verify `./mvnw -pl spring-boot-starter-openfeature-unleash test` passes.
- [x] 2.11 growthbook *(customizer)* — add the 3 assertions; verify `./mvnw -pl spring-boot-starter-openfeature-growthbook test` passes.

## 3. JaCoCo per-module coverage gate

- [ ] 3.1 Add a JaCoCo `check` execution to the root `pom.xml` under the active `build/plugins` (BUNDLE-level, `INSTRUCTION` counter, `COVEREDRATIO` minimum) so it runs in every module's `verify`; verify the check executes by running `./mvnw -pl spring-openfeature verify`.
- [ ] 3.2 Measure each module's post-test instruction coverage from the JaCoCo reports and set each module's `INSTRUCTION` floor to its measured value (the ratchet); verify `./mvnw clean verify` is green with the check active.
- [ ] 3.3 Prove the gate fails on regression: temporarily raise one module's floor above its actual coverage, run `./mvnw -pl <that-module> verify`, confirm it fails, then restore the correct floor; verify the restored build is green.
- [ ] 3.4 Run the full reactor `./mvnw clean verify` and verify it exits 0 with every module passing the coverage check.
