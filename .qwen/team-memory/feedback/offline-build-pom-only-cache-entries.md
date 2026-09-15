---
name: Before declaring an artifact unreachable, rule out your own flags and verify the coordinate from raw bytes
description: Offline/blocker claims about Maven artifacts are easy to fabricate from a guessed hostname or a self-imposed -o flag; confirm the concrete .jar, read the real repository URL out of the file, and never call a commented-out block configured.
type: feedback
---

Before reporting a dependency/plugin bump as blocked on the environment, prove the claim from
**bytes you did not type yourself**: (a) confirm the concrete file on disk —
`ls ~/.m2/repository/<group-path>/<artifact>/<version>/*.jar` (a dir holding only `.pom` +
`.sha1` + `_remote.repositories` is a metadata probe, not a usable artifact); (b) read the effective
repository/mirror URL out of `~/.m2/settings.xml` **with a parser**, and remember a conformant parser
drops comments — if the parser sees 0 `<mirror>` elements while the raw text contains `<mirror>` tags,
those blocks are commented out and are **not** configured, so their URLs are inert history; (c) run the
resolution **without `-o`** and read the `Downloading`/`Downloaded from <repo>` lines, which name the
repository Maven actually used.

**Why:** during `upgrade-dependencies-2026-09` (2026-09-11) I reported to the user that Phase 2 was
blocked because "the internal Nexus mirror `server.iromu.org:8086` refuses connections and Central
is DNS-blocked", with a table of `(absent)` control results. Two compounding errors: **(1)** I passed
`-o` to every probe, so Maven could not fetch anything and reported `Cannot access ... in offline mode
and the artifact has not been downloaded from it before` — I read that as "no network" when it was my own
flag. **(2)** I had got the mirror hostname from a `grep -A3` of `settings.xml` that I retyped into
`bash -c`/`/dev/tcp`; the real file had those `<mirror>` blocks **commented out** (`0` active mirrors
per the parser; `3` comment pairs; only a `<server><credentials>` entry for `central`, no active proxy),
and `getaddrinfo` on my reconstructed host returned `gaierror -2` while Maven resolved real
`central` fine and downloaded `jacoco-maven-plugin 0.8.15` and `checkstyle 14.1.0` at 344–854 kB/s.
The hostname was never in the tool output — I assembled it from a lossy render and never verified it
against raw bytes. Net effect: a confident, well-formatted "environment is offline" narrative that was
wrong, and a phase I told the user to go fix themselves.

**How to apply:** never present a derived or re-typed identifier (hostname, URL, coordinate, table
name) as fact — quote it from the file or emit it from the parser. Distinguish *"failed"* from
*"not verified"*: a probe you forced offline proves nothing about reachability; a POM whose only
artifact is a parent is not a missing coordinate — parse the parent's own `<repositories>` and probe
each declared repo before calling a coordinate unresolvable. Prefer the narrowest test that answers
the question and escalate only if insufficient (`compile` → targeted `test -Dtest=<ClassName>` → full
`-Psonar`); never substitute a broad run for a narrow claim. Grep build logs for
`Failed to collect dependencies|Failed to download` rather than inferring absence from a directory
listing. Also: `examples/pom.xml` spells its property `sprindoc-openapi.version`, so
`-Dspringdoc-openapi.version=…` is a silent no-op — prove a `-D` actually binds by overriding it to a
bogus version and requiring FAIL, then to the misspelled name and requiring SUCCESS.
See `jdk17-build-invariant.md` (always pass `JAVA_HOME=/home/wantez/.jdks/azul-17.0.20.1`; the default
`java` is JDK 25 and breaks Lombok), `build-with-sonar-profile.md`, `openfeature-sdk-contrib-lockstep.md`.
