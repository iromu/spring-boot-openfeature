---
name: make lint must name the checkstyle-validation execution
description: The Makefile lint/checkstyle targets have to invoke checkstyle:check@checkstyle-validation; a bare checkstyle:check silently runs sun_checks and reports rules the project never enforced.
type: feedback
---

`make lint` and `make checkstyle` must invoke `./mvnw checkstyle:check@checkstyle-validation`.
Keep the `@checkstyle-validation` suffix.

**Why:** the `maven-checkstyle-plugin` configuration in the root `pom.xml` — `configLocation`
(the Spring ruleset), `includeTestSourceDirectory=false`, `violationIgnore=SpringNullability` —
lives inside the `checkstyle-validation` *execution*, not at plugin level. A goal invoked from the
command line gets the `default-cli` execution and inherits none of it, so it falls back to
Checkstyle's bundled `sun_checks.xml`. On this repo that reports 68 violations the project does not
enforce at all, including 80-character `LineLength` and `FileTabCharacter` — and the repo indents
Java with tabs deliberately, so the "violations" contradict the documented convention. Dropping the
suffix therefore makes lint red on clean code while being blind to the Spring rules that matter.

This target has already failed silently once: it shipped as `.PHONY: lint` with no rule at all, so
`make lint` printed "Nothing to be done for 'lint'" and exited 0, and the local `make-lint`
pre-commit hook reported Passed having run nothing — while the standalone checkstyle hook had
already been removed from `.pre-commit-config.yaml`.

**How to apply:** never judge a lint target by its exit code. Prove it bites before trusting green:
add a temporary Java file with a `java.util.*` star import and confirm the goal fails with
`AvoidStarImport`, then delete it and confirm it passes. Same check when changing the plugin's
configuration or moving settings between `<configuration>` and an `<execution>`.
