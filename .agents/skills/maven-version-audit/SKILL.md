---
name: maven-version-audit
description: |-
  Use this skill whenever the user wants the current/latest version of anything Maven in this repo, or wants to know which Maven build plugins, dependencies, version properties, BOM imports, parents, or parent-pinned plugins (even the profile-gated examples reactor) are behind and should be bumped. Trigger on phrasings like "is a plugin on the newest version / should I raise it?", "what's the latest release of the OpenFeature SDK — are our contrib provider plugins behind?", "what version does the parent pin for the version-less plugins?", "which plugins are behind?", before opening a dependency-upgrade change, and to regenerate docs/dependency-versions.md. Treat any question about version currency of a Maven coordinate as this skill's job, even if the user never says "audit" or "check". It is the ground-truth source, not hand-reading POMs or `mvn versions:*`. Never for npm/package-lock, adding a new dependency or module, release notes or changelogs, build failures, or test runs.
---

# Maven Version Audit

Answer "what is the latest version of X / is X outdated / is an upgrade outstanding" for a Maven
project's coordinates with machine-harvested evidence, and (re)generate the audit document using
the pipeline bundled under this skill's `scripts/`.

The whole point is that the numbers are **harvested, not looked up by hand**: coordinates and
pinned versions are parsed from the POM bytes, "latest" comes from each coordinate's own
`maven-metadata.xml` (the very file a Maven client reads), repository roots are lifted out of
Maven's own `Downloaded from <repo>:` log lines, and every fetched file is rejected unless its
inner `<groupId>/<artifactId>` match the coordinate requested. Never reconstruct a URL, retype a
version, or trust a browser view.

## Two modes - pick by what was asked

- **Report mode** (default for "is X outdated / what's the latest / is a bump outstanding"):
  run the pipeline, then answer the specific question from the generated tables. Do not rewrite
  the checked-in doc unless asked.
- **Regenerate mode** (when asked to "refresh/regenerate the audit", "update the version doc",
  or as a pre-step of a version-bump change): run the full pipeline including `step3_doc.py`
  (which rewrites the audit document), then run all three self-checks and report.

## Where things live (all overridable, nothing hardcoded)

Every path the pipeline uses is resolved by `scripts/_va_paths.py` and can be pointed elsewhere
through the environment, so the skill is not welded to any one checkout layout, build-tool install,
or harness:

| env var | meaning | default |
| --- | --- | --- |
| `VERSION_AUDIT_ROOT` | the Maven project root to audit | nearest ancestor holding `pom.xml` + `.git` |
| `VERSION_AUDIT_TMP` | scratch (fetched metadata, maven logs) | `<root>/target/version-audit` (ignored by Maven builds, skipped by the POM walk) |
| `VERSION_AUDIT_DOC` | the audit document to write | `<root>/docs/dependency-versions.md` |
| `MVN` | the Maven command | `./mvnw` when present, else `mvn` |

Run from the project root. The scripts find the root themselves, so they work from wherever the
skill is installed - do not hardcode the skill's install path.

## The bundled scripts

The pipeline ships with this skill in its `scripts/` directory: `step1_coords.py`,
`step2_download.py`, `step3_doc.py`, the three self-checks `verify_ordering.py`, `crosscheck.py`,
`qa_doc.py`, and the shared `scripts/_va_paths.py` they all import. Set `SK` to that directory -
it is this skill's base directory (handed to you when the skill is invoked) joined with
`scripts/`. Invoke the scripts by that path; they read every other location from the table above.

## The canonical pipeline

Run in order; the Maven steps write the logs the Python steps read, into the same scratch dir.

```bash
SK=<this skill's scripts/ directory>            # the directory holding the scripts named above
TMP=${VERSION_AUDIT_TMP:-target/version-audit}
MVN=${MVN:-$(test -f ./mvnw && echo ./mvnw || echo mvn)}
mkdir -p "$TMP"

# 1. harvest every declared coordinate + pinned version out of the POM bytes -> $TMP/coords.tsv
python3 "$SK/step1_coords.py"

# 2. let Maven log its own repository traffic; read the repo roots back out of THAT log
"$MVN" -B -U org.codehaus.mojo:versions-maven-plugin:2.21.0:display-property-updates > "$TMP/props.log" 2>&1

# 3. Maven's own resolved closure (effective "in use" versions, incl. BOM-managed rows)
"$MVN" -B dependency:list > "$TMP/deplist.log" 2>&1

# 4. effective-POM print, for the version a parent pins on a plugin that declares none.
"$MVN" -B help:effective-pom > "$TMP/effpom.log" 2>&1
#    a profile gated sub-reactor is absent from the default reactor, so re-run the same
#    goal with -P <profile> (e.g. -P examples) and append, or its version-less
#    coordinates are silently missing:
#      "$MVN" -B -P examples help:effective-pom >> "$TMP/effpom.log" 2>&1

# 5. fetch maven-metadata.xml for every external coordinate across all declared repos
python3 "$SK/step2_download.py"

# 6. render the audit document (rewrites $VERSION_AUDIT_DOC)
python3 "$SK/step3_doc.py"
```

If the project has no wrapper, `MVN` falls back to a `mvn` on the PATH; set `MVN` explicitly if
neither is right. If the user only wants an answer about one or a few coordinates, you can still run
the whole pipeline (it is the reliable path); the doc is the source you read the answer from. Do not
fabricate a "latest" from memory or from a directory listing.

## Self-checks (run these in Regenerate mode; they are independent of the renderer)

```bash
SK=<this skill's scripts/ directory>
python3 "$SK/verify_ordering.py"   # re-derives ordering with a 2nd impl; checks latest_any maximality
python3 "$SK/crosscheck.py"         # 'behind' verdicts vs Maven's own versions plugin report (depupd.log)
python3 "$SK/qa_doc.py"             # rectangular tables + no harvested coordinate dropped from the doc
```

`crosscheck.py` reads `$TMP/depupd.log`; produce it with
`"$MVN" -B org.codehaus.mojo:versions-maven-plugin:2.21.0:display-dependency-updates > "$TMP/depupd.log" 2>&1`
if it is absent. A checker that fails is a real signal, not noise - read it before trusting the doc.

## Reading the version columns ("latest" is ambiguous, so all three are given)

| Column | Meaning |
| --- | --- |
| `pinned` | version the build declares (`${...}` placeholders resolved) |
| `in use` | version Maven actually resolved; differs from `pinned` when a BOM/parent decides |
| `release (repo)` | the `<release>` element the repository publishes |
| `latest plain` | highest version whose every dot component is purely numeric |
| `latest any` | highest version overall, qualifiers (`-alpha`/`-rc`/`-jre`) included |
| verdict | `current`, **behind**, *pre-release/qualified pin*, *unpinned*, *ahead of repo*, *not found* |

## Traps that bite (general patterns; the parenthesised cases are this repo's instances)

- **A local cache folder is not proof a version exists.** A directory holding only
  `<artifact>-<version>.jar.lastUpdated` / `.pom.lastUpdated` markers records a *failed* fetch, not
  a published artifact. Trust the `maven-metadata.xml` columns over a `find`/`ls` of the local
  `~/.m2` cache. (GrowthBook SDK `0.11.1` here is exactly this phantom.)
- **`display-property-updates` hides major upgrades.** That goal suppresses major-version bumps by
  default, so it will not agree with the **behind** rows; the tables come from the raw metadata
  version lists and do include major jumps. Do not conclude "nothing to do" from that goal alone.
- **Profile-gated sub-reactors are invisible to the default reactor.** A sub-reactor that builds only
  under a profile, and parents off its own pom rather than the root, is audited from POM bytes but
  absent from any default-reactor Maven report; ask for that profile when a version-less plugin's
  version comes from that parent. (The `examples` reactor under `-P examples` here.)
- **A provider family may move in lockstep.** When a set of artifacts is compiled against one another
  and breaks at *runtime*, not compile time, if they drift, upgrade the whole family as one change and
  re-run the full reactor (a first module failure skips every downstream module, hiding the real blast
  radius). (`dev.openfeature:sdk` and the `dev.openfeature.contrib.providers:*` artifacts here.)
- **`*not found*` means no declared repo returned content-verified metadata** for that coordinate -
  it is not the same as "no newer version".

## How to answer an "is X outdated?" question

1. Run the pipeline (or read the current audit document if it is fresh enough).
2. Find the coordinate's row; report `pinned`, `in use`, and the three latest columns.
3. State the verdict and, if **behind**, the exact target version to move to.
4. If the coordinate belongs to a lockstep family, add that caveat. If it is a *pre-release/qualified
   pin*, say the pin may be deliberate and to compare `latest plain` before treating it as a defect.
5. Cite the re-check URL for anything load-bearing:
   `<repo root>/<groupId with '.'->'/'>/<artifactId>/maven-metadata.xml`.

Never edit the tables in the audit document by hand - regenerate it by running the pipeline; the
checkers will fail if a table drifts from the harvested data.
