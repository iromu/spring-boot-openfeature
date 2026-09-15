## Purpose

Defines how this project manages its third-party dependency and build-plugin set: the OpenFeature SDK and every contrib provider resolve as one mutually-compatible lockstep set, the build is verified on the CI-aligned JDK 17 toolchain, provider auto-configuration contracts survive upstream upgrades, the examples reactor builds against the locally-built starter, and no binary-incompatible transitive dependency is introduced.

## ADDED Requirements

### Requirement: OpenFeature runtime dependencies resolve as one lockstep set
The OpenFeature SDK and every contrib provider artifact SHALL resolve to a mutually-compatible set within the shared compatibility window. The reactor SHALL NOT resolve an SDK version below the lower bound of any provider's declared SDK range, and SHALL NOT let an explicitly-pinned SDK version mask a provider's higher requirement.

#### Scenario: SDK satisfies every provider's declared range
- **WHEN** the reactor resolves its OpenFeature dependencies at the current version properties
- **THEN** the resolved SDK version is at or above the maximum lower-bound of the SDK range declared by every contrib provider in use

#### Scenario: Provider requiring a newer SDK is not run against an older one
- **WHEN** a contrib provider declares it requires the SDK at or above a version newer than a stale pinned SDK
- **THEN** the effective build resolves the SDK to satisfy that provider rather than keeping the stale pin, and the module compiles and its tests run green

### Requirement: The build is verified on the CI-aligned JDK 17 toolchain
The full `verify` phase, including its documentation-generation step, SHALL pass when run against a full JDK 17 that provides both the compiler and the documentation tool. The verification posture SHALL NOT rely on a JRE-only JDK 17 that lacks the documentation tool, and SHALL NOT substitute a non-CI-target JDK to bypass a missing tool.

#### Scenario: Full verify passes on a complete JDK 17
- **WHEN** the full reactor is built with the coverage/report profile enabled on a JDK 17 that has both the compiler and the documentation tool
- **THEN** every module completes the `verify` phase successfully

#### Scenario: A JRE-only JDK 17 is not used as the verification path
- **WHEN** the build is pointed at a JDK 17 that lacks the documentation tool
- **THEN** the `verify` phase fails at the documentation-generation step, and the documented remedy is to use a complete JDK 17 rather than a different-major JDK

### Requirement: Provider auto-configuration contracts survive an upstream provider upgrade
After a provider is upgraded, its auto-configuration SHALL continue to honor the existing activation, back-off, and customizer contract, and the build SHALL pin any behavior the upstream release changed by default. Specifically, a provider whose default evaluation mode changed upstream SHALL be exercised in a mode that does not depend on an unstubbed remote call, and a user-supplied provider that participates in the shared event-attachment lifecycle SHALL be constructed so that lifecycle initializes it.

#### Scenario: Upgraded provider still backs off to a user-supplied bean
- **WHEN** an upgraded provider auto-configuration runs and the application has supplied its own provider bean
- **THEN** the auto-configuration does not register a competing provider bean and the application-supplied provider remains the one in the context

#### Scenario: Default-mode change does not hang the build
- **WHEN** an upgraded provider now defaults to an in-process/remote evaluation mode that issues a startup configuration fetch
- **THEN** the test drives it in a mode served by a stubbed endpoint (or forces the prior mode) and the suite completes without an unanswered request

#### Scenario: Constructed provider participates in event attachment
- **WHEN** a test supplies a concrete provider instance that the SDK will attach to the event bus
- **THEN** the instance is built through its real constructor (or an explicit stub) so the attachment field is initialized, and the surrounding context loads without a null-attachment error

### Requirement: The examples reactor builds against the locally-built starter
The examples SHALL resolve the project's own starter and documentation dependencies from the locally-built `4.1.0-SNAPSHOT` rather than a stale published artifact, and SHALL build on the documentation-generation line compatible with the Spring Boot 4 parent.

#### Scenario: Examples resolve the local starter
- **WHEN** the examples reactor is built with the project version property overridden to the local snapshot
- **THEN** it resolves the project starter, the OpenAPI integration, and the Spring Boot 4 parent from that version and compiles successfully

### Requirement: No binary-incompatible transitive dependency is introduced
The managed dependency set SHALL NOT be moved to a major version that a consumer in the reactor is built against an older major of, and a blocked upgrade SHALL be retained at its current version with the block documented rather than forced.

#### Scenario: A consumer pinned to an older major is not broken
- **WHEN** a managed library is considered for a major-version bump while an in-tree consumer still references the previous major
- **THEN** the bump is held, the consumer's resolution is unchanged, and the reason for the block is recorded

#### Scenario: Blocked upgrade is documented, not silently applied
- **WHEN** an upstream release is identified as incompatible with the current dependency set
- **THEN** the plan records it as deferred with the specific blocking dependency, and no task in the current change forces the incompatible version
  - *Note (2026-09-14):* "held" is one compliant outcome, not the only one. The normative SHALL is that the managed set is not left in a state where an in-tree consumer is built against an absent/removed symbol of a transitive major. The §7 sweep reached that compliant state by **removing the incompatibility before the move** rather than holding it — promoting `flagsmith-java-client` to the OkHttp-5-native `8.1.1` (so `flagsmith` no longer resolves an `okhttp3` symbol absent under the `5.5.0` floor) and taking `growthbook-sdk-java` `0.11.0` whose `lib` declares `okhttp 5.4.0`. That is an alternative satisfying path, not a violation of this scenario; the residual (an unmanaged cross-major coordinate `okhttp-eventsource:4.3.0` on the Unleash classpath, symbol-verified present in `5.5.0`) is tracked as a hardening follow-up, not a shipped defect.
