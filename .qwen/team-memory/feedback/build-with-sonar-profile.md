---
name: Verify with the sonar profile so the aggregate coverage report is built
description: Local/agent verification builds must run with -P sonar so the aggregate JaCoCo XML is emitted; CI deliberately does not, so the 16-vs-17 module difference is expected.
type: feedback
---

Run the verification build with the `sonar` profile active:

```bash
JAVA_HOME=<jdk17> ./mvnw -B --no-transfer-progress clean verify -P sonar
```

**Why:** `aggregate-report` is declared only inside the `sonar` profile's `<modules>` block, so it is
absent from the default reactor. That module is the one that runs `jacoco:report-aggregate` to emit
`aggregate-report/target/site/jacoco-aggregate/jacoco.xml`, the file the
`sonar.coverage.jacoco.xmlReportPaths` property points SonarQube at. A plain `./mvnw clean verify`
therefore passes green while never producing the aggregate coverage report at all - a silent gap rather
than a failure. With the profile the reactor is 17 modules rather than 16.

**How to apply:** Treat `-P sonar` as part of the standard verify command, alongside pinning JDK 17. It is
cheap (roughly the same wall time, ~1 min) and it is the only configuration that exercises the
`groovy-maven-plugin` dependency-patching step in `aggregate-report` (it injects every reactor module as a
compile dependency to work around jacoco/jacoco#974), so a plain run also leaves that path untested. When
confirming green, check the aggregate report is actually emitted with one `<group>` per module rather than
trusting `BUILD SUCCESS` alone; the four pom-packaging modules (root aggregator, base starter, dependencies
BOM, `aggregate-report`) legitimately report zero instructions.

**Scope - do not "fix" CI:** the rule above is for *local/agent verification runs only*.
`.github/workflows/pull_request.yml` deliberately runs plain `./mvnw -B --no-transfer-progress clean
verify`, without the profile, so CI builds 16 modules and does not emit the aggregate report. Asked
directly on 2026-09-10 whether to add `-P sonar` there, the answer was no - leave CI as it is. The 16-vs-17
module difference between CI and a local run is therefore expected and correct, not a defect to reconcile.
Do not propose or apply a CI change on this basis.
