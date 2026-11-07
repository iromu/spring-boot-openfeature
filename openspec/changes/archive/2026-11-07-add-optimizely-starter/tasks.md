## 1. Premises and feasibility gates (prove before building — the two D-spikes)

- [x] 1.1 Resolve the vendor SDK locally and enumerate the sdk-key route (D3 spike): `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B dependency:get -DgroupId=com.optimizely.ab -DartifactId=core-api -Dversion=<candidate> -Dtransitive=true`, then `javap` the `ProjectConfig`/`ProjectConfigManager`/factory types. Record which construction path carries sdk-key/domain (starter-built `ProjectConfig` vs customizer-supplied collaborators) into `design.md` D3 — the recorded outcome selects the property surface; do not guess past the evidence.
- [x] 1.2 Prove datafile mode boots offline (D2 spike): construct `OptimizelyProviderConfig` with only `datafile` set to a minimal test datafile, build `new OptimizelyProvider(config)`, and register it through the client auto-config in a scratch context. Verify no network/polling/ODP traffic occurs (no listener bound to the endpoint; run against a deliberately unroutable base if the vendor client exposes one) and the provider evaluates from the datafile. If vendor machinery starts regardless, stop and adopt the back-off-only posture named in D2 before authoring the test tier.
- [x] 1.3 Prove the SDK and vendor pair resolve at the pinned levels: with group 2 wired, `./mvnw -B -pl spring-boot-starter-openfeature-optimizely dependency:list` must show `dev.openfeature:sdk` = root `sdk.version` (`1.22.1`) and both `com.optimizely.ab` artifacts at the BOM's `optimizely-sdk.version`. Anything else is a stop-and-escalate lockstep finding.

## 2. Build wiring (BOM-priced, vendor pair included)

- [x] 2.1 Root `pom.xml`: add `<optimizely.version>1.0.0</optimizely.version>`, `<optimizely-sdk.version>` (value from 1.1's resolved vendor release), and the `<module>` entry. `spring-boot-openfeature-dependencies/pom.xml`: managed rows for `dev.openfeature.contrib.providers:optimizely`, `com.optimizely.ab:core-api`, `com.optimizely.ab:core-httpclient-impl` priced by those properties. Verify by file read-back and `./mvnw -B validate` with the new module listed.
- [x] 2.2 Module pom mirroring the statsig module (same parent/BOM imports, `spring-boot-openfeature-autoconfigure`, lombok provided, `dev.openfeature.contrib.providers:optimizely` **unversioned**, `spring-boot-starter-test` test, JaCoCo/Checkstyle sections). Verify via `dependency:list`: provider and vendor pair present at BOM prices, no `<version>` literals in the module pom.

## 3. Starter classes (vendor-backed family: statsig/unleash shape)

- [x] 3.1 Write `OptimizelyProperties` (`spring.openfeature.optimizely` prefix; `enabled=true`, `datafile`, `sdk-key`, `datafile-auth-token`, `config-url`, `poll-interval`) and `OptimizelyCustomizer` (receives the `OptimizelyProviderConfig` builder), with the Apache header and author convention. Verify: compiles, formatter/checkstyle-clean under `validate`.
- [x] 3.2 Write `OptimizelyAutoConfiguration` per the family pattern (`@AutoConfigureBefore` incl. the multiprovider name guard, `@ConditionalOnClass(OptimizelyProvider.class)`, `enabled` gate, `@ConditionalOnMissingBean` config + provider beans; provider via `new OptimizelyProvider(config)`; customizer `orderedStream()` applied to the config builder; property→config mapping per 1.1's recorded outcome). Register in the `AutoConfiguration.imports` file. Verify: imports names exactly the class; `-pl` `verify` green.

## 4. Test tier (spec scenarios; statsig back-off harness)

- [x] 4.1 `OptimizelyAutoConfigurationTest.shouldBackOffWhenUserSuppliesProvider` and `shouldNotSupplyProviderWhenDisabled` first — both use the user-mock-provider back-off so the real vendor machinery never constructs; verify both report run, not skipped.
- [x] 4.2 `shouldSupplyDefaultBeans` (datafile mode from 1.2): `hasSingleBean(FeatureProvider.class).hasBean("optimizelyProvider").hasSingleBean(Client.class).hasBean("client")` with `datafile` pointed at the test resource; verify run-not-skipped and that the run also validates D2's offline claim inside the real test tier.
- [x] 4.3 `shouldApplyCustomizerToBuiltConfiguration` (recording customizer on the config builder) and `shouldBindDatafileProperty` (assembled config carries the property value, asserted from a capturing customizer's copy, not from the properties bean). Verify all run.
- [x] 4.4 Run `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -pl spring-boot-starter-openfeature-optimizely test` and verify `Skipped: 0` — no test in this module is allowed to self-skip; if 1.2 forced the fallback posture, the datafile-dependent tests are `@Tag`-gated **and** this task records where that gate's existence is justified in `design.md`, otherwise they must run.

## 5. Coverage floor and full gate

- [x] 5.1 Measure instruction coverage from the module's JaCoCo CSV on a clean run, round down to 0.01, set `jacoco.instruction.minimum` in the module pom; verify property equals measured value and `verify` passes.
- [x] 5.2 Full gate `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B -Psonar clean verify`: `BUILD SUCCESS`, 18 modules `SUCCESS`, none `SKIPPED`, 0 Checkstyle violations, all floors met — verified by grepping the log for `SKIPPED`/`FAILURE [` rather than trusting the final line. Confirm no `pull_request.yml` edit was needed.
