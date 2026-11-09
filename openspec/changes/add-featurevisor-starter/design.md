## Context

Featurevisor publishes an OpenFeature Java provider requiring an API key to connect to the Featurevisor service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Featurevisor credentials on the host.
- The API key is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — API key is property-sourced: `spring.openfeature.featurevisor.api-key`.**
The auto-config SHALL fail-fast if the key is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The back-off pattern is sufficient; the provider likely opens a network connection on construction.

**D4 — BOM prices the provider coordinate.**
Pinned at `featurevisor-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Open Questions

- Exact Maven coordinate and latest stable version.
- Whether the provider's constructor is lazy or eagerly connects.

## Findings (2026-11-09, coordinate resolution — task 1.1)

**Coordinate resolved:** `com.featurevisor:featurevisor-openfeature` (groupId is `com.featurevisor`, NOT `io.featurevisor`), latest stable `5.0.0` (tags v0.0.1 … v5.0.0 at github.com/featurevisor/featurevisor-java; the provider is the `featurevisor-openfeature` module, depending on `com.featurevisor:featurevisor-java` at the same version and on `dev.openfeature:sdk` pinned by the vendor at `1.23.0`).

**BLOCKER — artifact is not on Maven Central.** It is distributed exclusively to GitHub Packages (`https://maven.pkg.github.com/featurevisor/featurevisor-java`), and GitHub Packages requires authentication for Maven downloads even for public repos (anonymous GET of the maven-metadata.xml returns 401). Verified 2026-11-09:

- `repo1.maven.org/maven2/com/featurevisor/` and `.../io/featurevisor/` → 404 (no artifact ever published to Central under either group).
- Maven Central search index: 0 hits for `featurevisor` and `g:io.featurevisor`.
- `dev/openfeature/contrib/providers/` on Central: no `featurevisor` provider (13 contrib providers, none is Featurevisor).
- Vendor's own Java SDK docs (featurevisor.com/docs/sdks/java) instruct consumers to add the GitHub Packages repository AND a `settings.xml` `<server>` entry with a GitHub token (`read:packages` scope).
- `distributionManagement` in the featurevisor-java pom points at GitHub Packages on every tag (v0.1.0 → v5.0.0).

**Consequences:**

- This change cannot be implemented as specified: the starter's `com.featurevisor:featurevisor-openfeature` dependency would be unresolvable by this repo's CI (no GitHub Packages credentials in the build workflow) and by any consumer who has not configured a personal GitHub token in `~/.m2/settings.xml`. Every other starter in the fleet depends on Maven Central artifacts.
- The provider jar cannot be downloaded on this host (no `GITHUB_TOKEN` in the environment, no GitHub server entry in `~/.m2/settings.xml`), so task 1.2 (constructor laziness) and the D2/D3 decisions that depend on the API surface cannot be verified.
- The lockstep check (task 1.3) is moot until the artifact is resolvable; the vendor's own pom does pin `dev.openfeature:sdk` to `1.23.0`, which matches this repo's `sdk.version` at v5.0.0.

**Exit options (decision needed):** (a) defer the change until Featurevisor publishes to Maven Central (watch the repo's release process / OpenFeature ecosystem listing); (b) implement the starter against the GitHub Packages artifact with a documented consumer-side repository+token requirement and a CI secret — a deviation from the fleet convention requiring explicit sign-off; (c) drop the change.
