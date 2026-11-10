# aggregate-report

An internal build module that aggregates the JaCoCo coverage of every other module into a single
report, for SonarQube. It is **not** a published artifact and is not a dependency of any starter.

It is only built when the `sonar` Maven profile is active (see the root [`pom.xml`](../pom.xml)); a
plain `./mvnw clean verify` does not include it.

## How it works

1. A `groovy-maven-plugin` step (`patch-maven-dependencies`) adds every reactor module as a `compile`
   dependency of this project so JaCoCo's `report-aggregate` goal can see them (workaround for
   [jacoco#974](https://github.com/jacoco/jacoco/issues/974)).
2. The `jacoco-maven-plugin` `report-aggregate` goal runs in the `verify` phase and merges all
   `**/jacoco.exec` data files into `target/site/jacoco-aggregate/`.
3. The property `sonar.coverage.jacoco.xmlReportPaths` points Sonar at the aggregated
   `target/site/jacoco-aggregate/jacoco.xml`.
