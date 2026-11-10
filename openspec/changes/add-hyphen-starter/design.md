## Context

Hyphen AI publishes an OpenFeature Java provider requiring an API key to connect to the Hyphen AI service.

## Goals / Non-Goals

**Goals:**
- The auto-configuration contract is testable with zero Hyphen AI credentials on the host.
- The API key is an explicit, tested property.
- Follow the established provider-starter pattern.

**Non-Goals:**
- Reaching provider-vs-backend correctness.

## Decisions

**D1 — One module, single provider variant.**
Straightforward properties → config → provider bean.

**D2 — API key is property-sourced: `spring.openfeature.hyphen.api-key`.**
The auto-config SHALL fail-fast if the key is unset and the starter is enabled.

**D3 — Test posture: back-off for the contract tier.**
The back-off pattern is sufficient; the provider likely opens a network connection on construction.

**D4 — BOM prices the provider coordinate.**
Pinned at `hyphen-openfeature.version`. Lockstep check asserts `dev.openfeature:sdk` = `sdk.version`.

## Risks / Trade-offs

- [Provider constructor opens a network connection] → D3's back-off avoids this.
- [Provider churn] → BOM-pinned + `dependency:list` assertion.

## Migration Plan

Additive new module; rollback is removing the `<module>` entry, the BOM rows, and the property class.

## Open Questions

- Exact Maven coordinate and latest stable version.
- Whether the provider's constructor is lazy or eagerly connects.

## Findings (2026-11-10, coordinate resolution — task 1.1)

**Coordinate resolved:** `dev.hyphen:openfeature-provider-java:1.0.0`, from the `provider` branch of github.com/Hyphen/openfeature-provider-java ("The official Hyphen Toggle OpenFeature Provider"). The `main` branch holds only a `LICENSE`; the provider source exists only on the `provider` branch. API surface (read from source, since the jar is unresolvable — see below): `dev.hyphen.openfeature.HyphenProvider` with constructor `(String publicKey, HyphenProviderOptions options)`; `HyphenProviderOptions` is builder-style and requires `application` and `environment` (both `Objects.requireNonNull`), with optional `horizonUrls`, `enableToggleUsage` (default `true`) and `cacheTtlSeconds` (default `300`). The vendor pom pins `dev.openfeature:sdk:1.14.0` — nine minor versions behind this repo's `sdk.version` (`1.23.0`), so a lockstep pin would be required in the starter.

**BLOCKER — artifact is not on Maven Central, and there is no evidence it has ever been published anywhere.** Verified 2026-11-10 from raw bytes:

- `repo1.maven.org/maven2/dev/hyphen/openfeature-provider-java/maven-metadata.xml` → HTTP 404; the `1.0.0` POM and JAR URLs → HTTP 404 (no artifact ever published to Central under `dev.hyphen`).
- Maven Central search index (search.maven.org): 0 hits for `g:"dev.hyphen"` and for `hyphen openfeature`; a plain `hyphen` query returns only unrelated artifacts (`org.daisy.libs:hyphen`, `at.hyphen:android-sdk-ui`, …).
- The vendor pom declares **no `distributionManagement`** and no GPG/release plugins; the repo has **no git tags** and **no CI** (no `.github/` on the `provider` branch), so there is no release pipeline that could have published the artifact to Central or to GitHub Packages.
- GitHub Packages cannot be ruled out positively: an anonymous GET of `maven.pkg.github.com/hyphen/openfeature-provider-java/dev/hyphen/openfeature-provider-java/maven-metadata.xml` returns 401 (GitHub Packages requires authentication even for public repos), and no `GITHUB_TOKEN` is available on this host. But the absence of tags/CI/distributionManagement means nothing is expected to be there.

**Consequences:**

- This change cannot be implemented as specified: the starter's `dev.hyphen:openfeature-provider-java` dependency would be unresolvable by this repo's CI and by any consumer. Every other starter in the fleet depends on a Maven Central artifact.
- Task 1.3 (lockstep `dependency:list`) is moot until the artifact is resolvable.
- Task 1.2 (constructor laziness) is answerable from source only: the constructor is **eager** — it builds a `HyphenClient(publicKey, options)` (OkHttp-based per the vendor pom) at construction time, so the D3 back-off test posture would have been the right one.

**Exit options (decision needed):** (a) defer the change until Hyphen publishes the provider to Maven Central (watch the repo — it currently has no CI, no tags, and no `distributionManagement`); (b) implement the starter against a non-Central artifact (GitHub Packages + consumer-side token + CI secret, or vendoring the provider source) — a deviation from the fleet convention requiring explicit sign-off; (c) drop the change.
