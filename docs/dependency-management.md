# Dependency Management

How third-party versions are pinned, who proposes bumps, and what an upgrade must satisfy before it merges. The full plugin/dependency currency audit lives in [dependency-versions.md](dependency-versions.md); this page is the policy around it. See also [architecture.md](architecture.md) for the module layout and [provider-starter-contract.md](provider-starter-contract.md) for what a starter must keep working.

## Where versions live

| Location | What it pins |
| --- | --- |
| Root `pom.xml` `<properties>` | All version properties: `spring-boot.version` (`4.1.2`), `sdk.version` (`1.23.0`), one property per provider library and its vendor SDK, plus shared coordinates (`okhttp.version` `5.5.1`, `json.version` `20260814`). Project version is `${revision}` (`4.1.5-SNAPSHOT`). |
| Root `pom.xml` `<dependencyManagement>` | Imports `com.squareup.okhttp3:okhttp-bom` at `${okhttp.version}` — the OkHttp floor for the whole reactor. |
| Root `pom.xml` build sections | Build-plugin versions: JaCoCo `0.8.16`, Checkstyle plugin `3.6.0` (Checkstyle `12.3.1`), compiler `3.16.0`, versions-maven-plugin `2.24.0`, spring-javaformat `0.0.48`, flatten `1.9.0`, source `3.4.0`, javadoc `3.12.0`. The `publish` profile adds GPG `3.2.8` and central-publishing `0.12.0`. |
| `spring-boot-openfeature-dependencies/pom.xml` | The BOM consumers import. Every provider coordinate resolves through a root-pom property — the BOM itself declares no literals — and pins `dev.openfeature:sdk` at `${sdk.version}` so a vendor provider's transitive SDK never wins. Where a provider ships its vendor SDK in runtime scope only (Unleash, Flagsmith, GrowthBook, VWO, LaunchDarkly, FeatBit, Kameleoon), the BOM excludes the transitive copy and manages the vendor SDK explicitly. |
| `examples/pom.xml` (own reactor, `-P examples`) | Separate parent (`spring-boot-starter-parent` `4.1.1`) and its own `${revision}` (`4.1.4-SNAPSHOT`); `spring-boot-openfeature.version` tracks that revision so examples resolve the locally-built snapshot starter, not a stale published artifact. |

The root pom also declares the `jitpack.io` repository — required by the GrowthBook provider (see below).

**Inventory summary:** ~46 version properties in the root pom — `spring-boot.version`, `sdk.version`, `io.spring.javaformat.version`, and 42 third-party dependency properties grouped as: contrib provider pins (`dev.openfeature.contrib.providers.*`: configcat, env-var, flagd, flagsmith, flipt, gcp, go-feature-flag, jsonlogic, multiprovider, ofrep, statsig, optimizely, unleash), vendor OpenFeature providers published outside contrib (LaunchDarkly, Datadog, Split, DevCycle, FeatBit, FeatureFlip, Mixpanel, Kameleoon, Confidence, Octopus Deploy, VWO, Superposition, Ageris, CloudBees, ConfigDirector, GrowthBook), and the vendor SDKs those starters compile against. The per-row latest-vs-pinned audit is in [dependency-versions.md](dependency-versions.md) — do not hand-maintain a second table.

## The lockstep rule

**The OpenFeature SDK and every provider version move as one mutually-compatible set.**

- A contrib provider pin is only valid against the pinned `sdk.version`; bumping one without checking the other is not allowed.
- The reactor must never resolve an SDK below the lower bound of any provider's declared SDK range, and an explicitly-pinned `sdk.version` must never mask a provider that requires a newer SDK — bump the SDK rather than run a provider against an older one.
- Several starters document this inline in the root pom (Mixpanel, VWO, Superposition, CloudBees, ConfigDirector transitively pull an older `dev.openfeature:sdk`; the BOM pins them to `${sdk.version}`). Ageris declares the SDK `provided`, so the consumer's SDK arrives at `${sdk.version}` via `spring-openfeature`.

**Rationale:** the SDK's `FeatureProvider`/evaluation-context API is the contract every provider is compiled against; a provider resolved against an SDK outside its window fails at runtime, not at build time.

## Upgrade policy

Condensed from the retired `dependency-upgrades` OpenSpec capability. An upgrade PR MUST:

- **Lockstep:** resolve SDK + all contrib providers as one compatible set (rule above); the module compiles and its tests run green at the new combination.
- **Verify on the CI-aligned JDK 17:** full `verify` passes on a complete JDK 17 that provides both the compiler and the `javadoc` tool (build with `-P sonar` so the aggregate coverage report is built — see [quality-and-testing.md](quality-and-testing.md)). A JRE-only JDK 17 (missing `javadoc`) is not a verification path, and switching to a different JDK major to dodge the missing tool is prohibited — install the full JDK 17.
- **Preserve the provider auto-config contract:** after a provider upgrade, activation/back-off/customizer behavior still holds ([provider-starter-contract.md](provider-starter-contract.md)). If upstream changed a default evaluation mode, tests drive the provider via a stubbed endpoint (or force the prior mode) — never an unstubbed remote call that hangs the suite. A test-supplied concrete provider instance is built through its real constructor so the SDK event-attachment field initializes.
- **Examples track the local build:** the examples reactor resolves the project's own starter from the locally-built snapshot, not a published artifact.
- **No binary-incompatible transitive bump:** the managed set MUST NOT be left in a state where an in-tree consumer is built against a symbol absent from a transitively-upgraded major. When an upgrade is blocked, either **hold it** at the current version with the block documented here, or **remove the incompatibility first** (spec amendment, 2026-09-14) — e.g. the §7 sweep promoted `flagsmith-java-client` to the OkHttp-5-native release (since taken further, to `8.4.0`) and moved `growthbook-sdk-java` to a version whose `lib` declares OkHttp 5, instead of holding. Forcing the incompatible version is never compliant.

## Known pinned / blocked coordinates

Recorded findings; re-verify against the pom before acting (see [backlog.md](backlog.md) for open items).

| Coordinate | Pinned | Status & reason | Recorded |
| --- | --- | --- | --- |
| `com.github.growthbook:growthbook-openfeature-provider-java` | `0.0.2` | Published to **JitPack only**, not Maven Central. Dependabot is blind to it (its maven ecosystem checks Central); the root pom declares the `jitpack.io` repository to resolve it. Needs manual watching on a GrowthBook release. | 2026-11-10 audit |
| `com.vwo.sdk:vwo-fme-java-sdk` | `1.8.1` | Upgrade path to `1.65.0` is **blocked by the provider**: `vwo-openfeature-provider-java` `1.0.0` was compiled against `1.8.1` (the starter compiles against `com.vwo.VWO` from it). Held per policy; unblock requires the provider vendor to move. | 2026-11-10 audit |
| `com.kameleoon:kameleoon-openfeature-java` / `kameleoon-client-java` | `0.0.1` / `4.25.0` | Kameleoon's SDK and OpenFeature provider are **GPL-3.0** (copyleft), unlike the Apache-2.0 rest of the fleet. Not a build block — a licensing consideration to surface to consumers of that starter. | 2026-11-10 audit |
| `com.squareup.okhttp3:okhttp-eventsource` (transitive, Unleash classpath) | `4.3.0` (unmanaged) | Cross-major coordinate outside the `okhttp-bom` `5.5.x` floor; its OkHttp symbols were verified present under `5.5.0`, so it is a hardening follow-up, not a shipped defect. Tracked from the spec's §7 note. | 2026-09-14 |

## How updates are proposed

- **Dependabot** (`.github/dependabot.yml`): weekly `maven` updates on the root and weekly `github-actions` updates. It opens PRs; it does not auto-merge. JitPack-only coordinates are invisible to it (above).
- **Dependency review** (`.github/workflows/dependency-review.yml`, `actions/dependency-review-action` v5.0.0): flags vulnerable and newly-added dependencies on every PR.
- Dependabot PRs still carry the full upgrade policy above — the lockstep check and JDK 17 `verify` are the reviewer's job, not the bot's.

## Regenerating the version audit

`docs/dependency-versions.md` is generated, not hand-edited. Ask the agent to run the project's **`maven-version-audit` skill** (e.g. "regenerate docs/dependency-versions.md"); it harvests declared coordinates from the POMs, reads each coordinate's `maven-metadata.xml` from the repositories Maven itself reports, and re-renders the file from `dependency:list` truth. Use it for any "is X on the latest version / which plugins are behind?" question instead of hand-reading POMs or `mvn versions:*`.
