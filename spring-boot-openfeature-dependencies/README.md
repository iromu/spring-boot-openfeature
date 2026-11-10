# spring-boot-openfeature-dependencies

A [BOM](https://maven.apache.org/glossary.html#BillOfMaterials) that pins the versions of the
OpenFeature SDK and every feature-flag provider used by this project. Import it to manage versions
of the starters and their underlying provider libraries so they stay consistent (and the OpenFeature
SDK stays in lockstep) across your build.

## Importing the BOM

**Maven**

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.iromu.openfeature</groupId>
            <artifactId>spring-boot-openfeature-dependencies</artifactId>
            <version>${spring-boot-openfeature.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Gradle**

```groovy
dependencies {
    implementation platform('org.iromu.openfeature:spring-boot-openfeature-dependencies:${springBootOpenfeatureVersion}')
}
```

## What it manages

- `dev.openfeature:sdk` — pinned to a single version so every provider starter uses the same OpenFeature
  SDK, even when a vendor provider transitively pulls in a different one.
- Each OpenFeature contrib provider (`configcat`, `env-var`, `flagd`, `flagsmith`, `flipt`, `gcp`,
  `go-feature-flag`, `jsonlogic-eval-provider`, `multiprovider`, `ofrep`, `statsig`, `unleash`,
  `optimizely`, `growthbook`) and its underlying vendor SDK.
- Each third-party / vendor provider (LaunchDarkly, Datadog, Split, DevCycle, FeatBit, FeatureFlip,
  Mixpanel, Kameleoon, Confidence, Octopus Deploy, VWO, Superposition, Ageris, CloudBees,
  ConfigDirector) and its underlying client.

The published POM is flattened (via the `flatten-maven-plugin`) so the resolved versions are inlined
rather than referencing the `${...}` properties of the build.

For the exact current versions of every managed coordinate, see
[`docs/dependency-versions.md`](../docs/dependency-versions.md).
