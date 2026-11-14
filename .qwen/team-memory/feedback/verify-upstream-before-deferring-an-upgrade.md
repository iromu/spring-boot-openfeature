---
name: Verify upstream availability before deferring an upgrade as blocked
description: Never record a dependency upgrade as "blocked / no compatible upstream build exists" without probing the repository for it
type: feedback
---

Before writing "no upstream build tracks X, so this upgrade is deferred" into a plan or
`design.md`, probe the repository for it. Resolve the candidate directly, e.g.
`JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1 ./mvnw -B dependency:get -DgroupId=… -DartifactId=… -Dversion=RELEASE -Dtransitive=false`
and read the `<version>` list out of the cached `maven-metadata.xml`; then read the
candidate's own pom to see what it actually declares.

**Why:** `design.md` Decision 6 in `upgrade-dependencies-2026-09` recorded that "no flagsmith
build tracking OkHttp 5 exists", and that single unverified sentence justified deferring the
OkHttp 5 line for the whole change. It was false: `com.flagsmith:flagsmith-java-client:8.1.1`
exists (only `7.4.3` and `8.1.1` were ever published) and declares `okhttp.version=5.0.0` with
artifactId `okhttp-jvm`. The block was an artifact of a stale claim, not of upstream reality, and
the whole `okhttp` floor move was held hostage to it. Note the two failure modes are different:
`-o`/offline probes fabricate *absence* (they cannot see the network), whereas a metadata probe
that returns a version list is positive evidence — so distinguish "I could not look" from "there
is nothing there", and never let the former be written down as the latter.

**How to apply:** when a dependency upgrade looks blocked, first prove the blocker is real by
naming the artifact, the version, and the specific symbol/dependency that breaks. If the claim is
about upstream ("no release exists that supports Y"), verify it with `dependency:get` plus a read of
the candidate pom before believing it, and prefer a *newer* release of the blocking dependency over
downgrading, excluding, or pinning the floor. When a transitive must be re-pinned, follow this repo's
existing override pattern (exclude it from the managed provider, re-declare it unversioned in the
module, price it from a root property) and read the resolved version back with `dependency:list` —
excluding without re-declaring drops the artifact entirely, because dependency management only prices
a dependency some pom already requests.
