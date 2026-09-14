# Maven plugin and dependency version audit

Generated 2026-09-13 12:42:41 from the repository's own build metadata by the scripts under
`docs/version-audit/` (`step1_coords.py` harvests coordinates from the POMs,
`step2_download.py` fetches the repository metadata, `step3_doc.py` renders this file).

## How this was produced

1. **Declared coordinates and pinned versions** are parsed out of this repository's
   `pom.xml` files. Nothing below is typed in by hand, so a row cannot drift from
   what the build actually says.
2. **Latest versions** are read from each coordinate's `maven-metadata.xml` - the very
   file a Maven client consults to decide what is available. This is the authoritative
   repository index (the same data a browser view such as `mvnrepository.com` renders).
3. **Repository roots were not reconstructed**: they were harvested from the
   `Downloading/Downloaded from <repo>:` lines Maven itself wrote to its log, then
   reduced to the repository root by stripping the coordinate path that step1 parsed
   out of the POMs.
4. **Every response is content-checked**: the `<groupId>`/`<artifactId>` inside a
   fetched metadata file must equal the coordinate that was asked for, or the hit is
   discarded. A permissive error page cannot masquerade as a result.
5. **`in use` comes from Maven's own `dependency:list`**, so BOM-managed rows show the
   version really resolved rather than a guess about the BOM.

Repositories consulted (as declared by the build itself):

| Repository | Root used for metadata lookups |
| --- | --- |
| `central` | `https://repo.maven.apache.org/maven2` |
| `jitpack.io` | `https://jitpack.io` |

Any row can be re-checked directly:

```
<repo root>/<groupId with '.' replaced by '/'>/<artifactId>/maven-metadata.xml
```

Reading the version columns ("latest" is ambiguous, so all three are given):

| Column | Meaning |
| --- | --- |
| `pinned` | version the build declares (`${...}` placeholders resolved) |
| `in use` | version Maven actually resolved; differs from `pinned` when a BOM/parent decides |
| `release (repo)` | the `<release>` element the repository publishes |
| `latest plain` | highest version whose every dot component is purely numeric |
| `latest any` | highest version overall, qualifiers such as `-alpha`/`-rc` included |
| verdict | `current`, **behind**, *pre-release/qualified pin*, *unpinned*, *ahead of repo*, *not found* |

## 1. Maven build plugins

Plugins bound by `<build><plugins>` / `<pluginManagement>` across the reactor. A plugin
that declares no version is pinned by a parent's `pluginManagement`; the version Maven
actually bound is then taken from its own effective-POM output rather than omitted.

| Plugin | pinned | release (repo) | latest plain | latest any | verdict | repo |
| --- | --- | --- | --- | --- | --- | --- |
| `io.spring.javaformat:spring-javaformat-maven-plugin` | `0.0.48` *(declared as* `${io.spring.javaformat.version}`*)* | `0.0.48` | `0.0.48` | `0.0.48` | current | central |
| `org.apache.maven.plugins:maven-checkstyle-plugin` | `3.6.0` | `3.6.0` | `3.6.0` | `3.6.0` | current | central |
| `org.apache.maven.plugins:maven-compiler-plugin` | `3.16.0` | `4.0.0-beta-5` | `3.16.0` | `4.0.0-beta-5` | current | central |
| `org.apache.maven.plugins:maven-gpg-plugin` | `3.2.8` | `3.2.8` | `3.2.8` | `3.2.8` | current | central |
| `org.apache.maven.plugins:maven-javadoc-plugin` | `3.12.0` | `3.12.0` | `3.12.0` | `3.12.0` | current | central |
| `org.apache.maven.plugins:maven-source-plugin` | `3.4.0` | `4.0.0-beta-1` | `3.4.0` | `4.0.0-beta-1` | current | central |
| `org.codehaus.gmaven:groovy-maven-plugin` | `2.1.1` | `2.1.1` | `2.1.1` | `2.1.1` | current | central |
| `org.codehaus.mojo:flatten-maven-plugin` | `1.8.0` | `1.8.0` | `1.8.0` | `1.8.0` | current | central |
| `org.codehaus.mojo:versions-maven-plugin` | `2.21.0` | `2.22.0` | `2.22.0` | `2.22.0` | **behind** | central |
| `org.graalvm.buildtools:native-maven-plugin` | `1.1.8` *(inherited from a parent)* | `1.1.12` | `1.1.12` | `1.1.12` | **behind** | central |
| `org.jacoco:jacoco-maven-plugin` | `0.8.15` | `0.8.15` | `0.8.15` | `0.8.15` | current | central |
| `org.sonatype.central:central-publishing-maven-plugin` | `0.11.0` | `0.11.0` | `0.11.0` | `0.11.0` | current | central |
| `org.springframework.boot:spring-boot-maven-plugin` | `4.1.1` *(inherited from a parent)* | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |

Behind the latest plain release:

- `org.codehaus.mojo:versions-maven-plugin`: pinned `2.21.0`, latest plain `2.22.0`
- `org.graalvm.buildtools:native-maven-plugin`: pinned `1.1.8`, latest plain `1.1.12`

## 2. Version properties

Each version property the build declares, the coordinate it drives, and where that
coordinate stands against the repository.

| Property | pinned | drives | release (repo) | latest plain | latest any | verdict |
| --- | --- | --- | --- | --- | --- | --- |
| `sprindoc-openapi.version` | `3.1.1` | `org.springdoc:springdoc-openapi-starter-webflux-ui` | `3.1.1` | `3.1.1` | `3.1.1` | current |
| `configcat.version` | `0.2.1` | `dev.openfeature.contrib.providers:configcat` | `0.2.1` | `0.2.1` | `0.2.1` | current |
| `env-var.version` | `0.0.12` | `dev.openfeature.contrib.providers:env-var` | `0.0.12` | `0.0.12` | `0.0.12` | current |
| `flagd.version` | `0.14.1` | `dev.openfeature.contrib.providers:flagd` | `0.14.1` | `0.14.1` | `0.14.1` | current |
| `flagsmith-java-client.version` | `8.1.1` | `com.flagsmith:flagsmith-java-client` | `8.1.1` | `8.1.1` | `8.1.1` | current |
| `flagsmith.version` | `0.0.13` | `dev.openfeature.contrib.providers:flagsmith` | `0.0.13` | `0.0.13` | `0.0.13` | current |
| `flipt.version` | `0.1.4` | `dev.openfeature.contrib.providers:flipt` | `0.1.4` | `0.1.4` | `0.1.4` | current |
| `go-feature-flag.version` | `1.2.1` | `dev.openfeature.contrib.providers:go-feature-flag` | `1.2.1` | `1.2.1` | `1.2.1` | current |
| `growthbook-provider.version` | `0.0.2` | `com.github.growthbook:growthbook-openfeature-provider-java` | `0.0.2` | `0.0.2` | `0.0.2` | current |
| `growthbook-sdk-java.version` | `0.11.0` | `com.github.growthbook:growthbook-sdk-java` | `0.11.0` | `0.11.0` | `0.11.0` | current |
| `io.spring.javaformat.version` | `0.0.48` | `io.spring.javaformat:spring-javaformat-checkstyle`, `io.spring.javaformat:spring-javaformat-maven-plugin` | `0.0.48` | `0.0.48` | `0.0.48` | current |
| `jsonlogic-eval-provider.version` | `1.3.0` | `dev.openfeature.contrib.providers:jsonlogic-eval-provider` | `1.3.0` | `1.3.0` | `1.3.0` | current |
| `multiprovider.version` | `0.0.3` | `dev.openfeature.contrib.providers:multiprovider` | `0.0.3` | `0.0.3` | `0.0.3` | current |
| `okhttp.version` | `5.5.0` | `com.squareup.okhttp3:okhttp-bom` | `5.5.0` | `5.5.0` | `5.5.0` | current |
| `sdk.version` | `1.22.1` | `dev.openfeature:sdk` | `1.22.1` | `1.22.1` | `1.22.1` | current |
| `spring-boot.version` | `4.1.1` | `org.springframework.boot:spring-boot-dependencies` | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current |
| `statsig.version` | `0.2.1` | `dev.openfeature.contrib.providers:statsig` | `0.2.1` | `0.2.1` | `0.2.1` | current |
| `unleash-client-java.version` | `12.3.0` | `io.getunleash:unleash-client-java` | `12.3.0` | `12.3.0` | `12.3.0` | current |
| `unleash.version` | `0.1.3-alpha` | `dev.openfeature.contrib.providers:unleash` | `0.1.3-alpha` | *n/a* | `0.1.3-alpha` | current |

### Build properties that are not artifact versions

| Where | Property | value |
| --- | --- | --- |
| `aggregate-report/pom.xml` | `sonar.coverage.jacoco.xmlReportPaths` | ${basedir}/target/site/jacoco-aggregate/jacoco.xml |
| `examples/pom.xml` | `revision` | 4.1.0-SNAPSHOT |
| `examples/pom.xml` | `spring-boot-openfeature.version` | 4.1.0-SNAPSHOT *(declared as* `${revision}`*)* |
| `pom.xml` | `jacoco.instruction.minimum` | 0.0 |
| `pom.xml` | `java.version` | 17 |
| `pom.xml` | `maven.compiler.source` | 17 *(declared as* `${java.version}`*)* |
| `pom.xml` | `maven.compiler.target` | 17 *(declared as* `${java.version}`*)* |
| `pom.xml` | `project.build.sourceEncoding` | UTF-8 |
| `pom.xml` | `revision` | 4.1.0-SNAPSHOT |
| `spring-boot-openfeature-autoconfigure/pom.xml` | `jacoco.instruction.minimum` | 0.97 |
| `spring-boot-starter-openfeature-configcat/pom.xml` | `jacoco.instruction.minimum` | 0.72 |
| `spring-boot-starter-openfeature-envvar/pom.xml` | `jacoco.instruction.minimum` | 1.00 |
| `spring-boot-starter-openfeature-flagd/pom.xml` | `jacoco.instruction.minimum` | 0.89 |
| `spring-boot-starter-openfeature-flagsmith/pom.xml` | `jacoco.instruction.minimum` | 1.00 |
| `spring-boot-starter-openfeature-flipt/pom.xml` | `jacoco.instruction.minimum` | 1.00 |
| `spring-boot-starter-openfeature-gofeatureflag/pom.xml` | `jacoco.instruction.minimum` | 0.91 |
| `spring-boot-starter-openfeature-growthbook/pom.xml` | `jacoco.instruction.minimum` | 0.94 |
| `spring-boot-starter-openfeature-jsonlogic/pom.xml` | `jacoco.instruction.minimum` | 0.94 |
| `spring-boot-starter-openfeature-multiprovider/pom.xml` | `jacoco.instruction.minimum` | 0.87 |
| `spring-boot-starter-openfeature-statsig/pom.xml` | `jacoco.instruction.minimum` | 1.00 |
| `spring-boot-starter-openfeature-unleash/pom.xml` | `jacoco.instruction.minimum` | 0.90 |
| `spring-openfeature/pom.xml` | `jacoco.instruction.minimum` | 1.00 |

## 3. Parents and imported BOMs

| Kind | Declared in | Coordinate | pinned | release (repo) | latest plain | verdict |
| --- | --- | --- | --- | --- | --- | --- |
| bom import | `pom.xml` | `com.squareup.okhttp3:okhttp-bom` | `5.5.0` | `5.5.0` | `5.5.0` | current |
| parent | `spring-boot-starter-openfeature-statsig/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature-statsig/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| bom import | `spring-boot-starter-openfeature-statsig/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `examples/pom.xml` | `org.springframework.boot:spring-boot-starter-parent` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| parent | `examples/unleash-simple/pom.xml` | `org.iromu.openfeature.examples:examples` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `examples/unleash-advanced/pom.xml` | `org.iromu.openfeature.examples:examples` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-boot-openfeature-autoconfigure/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-openfeature-autoconfigure/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| parent | `spring-boot-starter-openfeature-gofeatureflag/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature-gofeatureflag/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| bom import | `spring-boot-starter-openfeature-gofeatureflag/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-boot-starter-openfeature-flagsmith/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature-flagsmith/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| bom import | `spring-boot-starter-openfeature-flagsmith/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-boot-starter-openfeature-jsonlogic/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature-jsonlogic/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| bom import | `spring-boot-starter-openfeature-jsonlogic/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-boot-starter-openfeature/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-boot-starter-openfeature-configcat/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature-configcat/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| bom import | `spring-boot-starter-openfeature-configcat/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-boot-starter-openfeature-multiprovider/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature-multiprovider/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| bom import | `spring-boot-starter-openfeature-multiprovider/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-boot-starter-openfeature-flipt/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature-flipt/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| bom import | `spring-boot-starter-openfeature-flipt/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-boot-openfeature-dependencies/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-openfeature/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-openfeature/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| parent | `spring-boot-starter-openfeature-envvar/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature-envvar/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| bom import | `spring-boot-starter-openfeature-envvar/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `aggregate-report/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-boot-starter-openfeature-growthbook/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature-growthbook/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| bom import | `spring-boot-starter-openfeature-growthbook/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-boot-starter-openfeature-flagd/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature-flagd/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| bom import | `spring-boot-starter-openfeature-flagd/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| parent | `spring-boot-starter-openfeature-unleash/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature` | *n/a* | *n/a* | *n/a* | *reactor internal* |
| bom import | `spring-boot-starter-openfeature-unleash/pom.xml` | `org.springframework.boot:spring-boot-dependencies` | `4.1.1` | `4.2.0-M1` | `4.1.1` | current |
| bom import | `spring-boot-starter-openfeature-unleash/pom.xml` | `org.iromu.openfeature:spring-boot-openfeature-dependencies` | *n/a* | *n/a* | *n/a* | *reactor internal* |

## 4. Declared project dependencies

Direct dependencies the reactor declares itself.

| Coordinate | pinned | in use | scopes | release (repo) | latest plain | latest any | verdict | repo |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `com.flagsmith:flagsmith-java-client` | `8.1.1` *(declared as* `${flagsmith-java-client.version}`*)* | `8.1.1` | compile | `8.1.1` | `8.1.1` | `8.1.1` | current | central |
| `com.github.growthbook:growthbook-openfeature-provider-java` | `0.0.2` *(declared as* `${growthbook-provider.version}`*)* | `0.0.2` | compile | `0.0.2` | `0.0.2` | `0.0.2` | current | jitpack.io |
| `com.github.growthbook:growthbook-sdk-java` | `0.11.0` *(declared as* `${growthbook-sdk-java.version}`*)* | `0.11.0` | compile | `0.11.0` | `0.11.0` | `0.11.0` | current | jitpack.io |
| `com.squareup.okhttp3:mockwebserver` | `5.3.2` | `5.5.0` | test | `5.5.0` | `5.5.0` | `5.5.0` | **behind** | central |
| `com.squareup.okhttp3:okhttp` | *by BOM* | `5.5.0` | compile, runtime | `5.5.0` | `5.5.0` | `5.5.0` | current | central |
| `com.squareup.okhttp3:okhttp-bom` | `5.5.0` *(declared as* `${okhttp.version}`*)* | *not resolved* | import | `5.5.0` | `5.5.0` | `5.5.0` | current | central |
| `dev.openfeature:sdk` | `1.22.1` *(declared as* `${sdk.version}`*)* | `1.22.1` | compile | `1.22.1` | `1.22.1` | `1.22.1` | current | central |
| `dev.openfeature.contrib.providers:configcat` | `0.2.1` *(declared as* `${configcat.version}`*)* | `0.2.1` | compile | `0.2.1` | `0.2.1` | `0.2.1` | current | central |
| `dev.openfeature.contrib.providers:env-var` | `0.0.12` *(declared as* `${env-var.version}`*)* | `0.0.12` | compile, test | `0.0.12` | `0.0.12` | `0.0.12` | current | central |
| `dev.openfeature.contrib.providers:flagd` | `0.14.1` *(declared as* `${flagd.version}`*)* | `0.14.1` | compile | `0.14.1` | `0.14.1` | `0.14.1` | current | central |
| `dev.openfeature.contrib.providers:flagsmith` | `0.0.13` *(declared as* `${flagsmith.version}`*)* | `0.0.13` | compile | `0.0.13` | `0.0.13` | `0.0.13` | current | central |
| `dev.openfeature.contrib.providers:flipt` | `0.1.4` *(declared as* `${flipt.version}`*)* | `0.1.4` | compile | `0.1.4` | `0.1.4` | `0.1.4` | current | central |
| `dev.openfeature.contrib.providers:go-feature-flag` | `1.2.1` *(declared as* `${go-feature-flag.version}`*)* | `1.2.1` | compile | `1.2.1` | `1.2.1` | `1.2.1` | current | central |
| `dev.openfeature.contrib.providers:jsonlogic-eval-provider` | `1.3.0` *(declared as* `${jsonlogic-eval-provider.version}`*)* | `1.3.0` | compile | `1.3.0` | `1.3.0` | `1.3.0` | current | central |
| `dev.openfeature.contrib.providers:multiprovider` | `0.0.3` *(declared as* `${multiprovider.version}`*)* | `0.0.3` | compile | `0.0.3` | `0.0.3` | `0.0.3` | current | central |
| `dev.openfeature.contrib.providers:statsig` | `0.2.1` *(declared as* `${statsig.version}`*)* | `0.2.1` | compile | `0.2.1` | `0.2.1` | `0.2.1` | current | central |
| `dev.openfeature.contrib.providers:unleash` | `0.1.3-alpha` *(declared as* `${unleash.version}`*)* | `0.1.3-alpha` | compile | `0.1.3-alpha` | *n/a* | `0.1.3-alpha` | current | central |
| `io.getunleash:unleash-client-java` | `12.3.0` *(declared as* `${unleash-client-java.version}`*)* | `12.3.0` | compile | `12.3.0` | `12.3.0` | `12.3.0` | current | central |
| `org.projectlombok:lombok` | *by BOM* | `1.18.46` | provided | `1.18.48` | `1.18.48` | `1.18.48` | **behind** | central |
| `org.slf4j:slf4j-api` | *by BOM* | `2.0.18` | compile | `2.1.0-alpha1` | `2.0.19` | `2.1.0-alpha1` | **behind** | central |
| `org.springdoc:springdoc-openapi-starter-webflux-ui` | `3.1.1` *(declared as* `${sprindoc-openapi.version}`*)* | *not resolved* | compile | `3.1.1` | `3.1.1` | `3.1.1` | current | central |
| `org.springframework.boot:spring-boot-autoconfigure` | *by BOM* | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-autoconfigure-processor` | *by BOM* | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-configuration-processor` | *by BOM* | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-dependencies` | `4.1.1` *(declared as* `${spring-boot.version}`*)* | *not resolved* | import | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter-actuator` | *by BOM* | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter-aspectj` | *by BOM* | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter-security` | *by BOM* | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter-test` | *by BOM* | `4.1.1` | test | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter-webflux` | *by BOM* | *not resolved* | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | *unpinned* | central |
| `org.springframework.security:spring-security-test` | *by BOM* | `7.1.1` | test | `7.2.0-M1` | `7.1.1` | `7.2.0-M1` | current | central |
| `com.puppycrawl.tools:checkstyle` *(plugin tooling)* | `12.3.1` | *not resolved* | compile | `14.1.0` | `14.1.0` | `14.1.0` | **behind** | central |
| `io.spring.javaformat:spring-javaformat-checkstyle` *(plugin tooling)* | `0.0.48` *(declared as* `${io.spring.javaformat.version}`*)* | *not resolved* | compile | `0.0.48` | `0.0.48` | `0.0.48` | current | central |

Behind the latest plain release:

- `com.squareup.okhttp3:mockwebserver`: pinned `5.3.2`, latest plain `5.5.0`
- `org.projectlombok:lombok`: pinned `1.18.46`, latest plain `1.18.48`
- `org.slf4j:slf4j-api`: pinned `2.0.18`, latest plain `2.0.19`
- `com.puppycrawl.tools:checkstyle`: pinned `12.3.1`, latest plain `14.1.0`

### Reactor-internal coordinates

Built by this project, so no repository "latest" applies.

| Coordinate | pinned | declared in |
| --- | --- | --- |
| `org.iromu.openfeature:spring-boot-openfeature-autoconfigure` | `4.1.0-SNAPSHOT` | `spring-boot-starter-openfeature-configcat/pom.xml`, `spring-boot-starter-openfeature-envvar/pom.xml`, `spring-boot-starter-openfeature-flagd/pom.xml`, `spring-boot-starter-openfeature-flagsmith/pom.xml`, `spring-boot-starter-openfeature-flipt/pom.xml`, `spring-boot-starter-openfeature-gofeatureflag/pom.xml`, `spring-boot-starter-openfeature-growthbook/pom.xml`, `spring-boot-starter-openfeature-jsonlogic/pom.xml`, `spring-boot-starter-openfeature-multiprovider/pom.xml`, `spring-boot-starter-openfeature-statsig/pom.xml`, `spring-boot-starter-openfeature-unleash/pom.xml`, `spring-boot-starter-openfeature/pom.xml` |
| `org.iromu.openfeature:spring-boot-openfeature-dependencies` | `4.1.0-SNAPSHOT` | `spring-boot-starter-openfeature-configcat/pom.xml`, `spring-boot-starter-openfeature-envvar/pom.xml`, `spring-boot-starter-openfeature-flagd/pom.xml`, `spring-boot-starter-openfeature-flagsmith/pom.xml`, `spring-boot-starter-openfeature-flipt/pom.xml`, `spring-boot-starter-openfeature-gofeatureflag/pom.xml`, `spring-boot-starter-openfeature-growthbook/pom.xml`, `spring-boot-starter-openfeature-jsonlogic/pom.xml`, `spring-boot-starter-openfeature-multiprovider/pom.xml`, `spring-boot-starter-openfeature-statsig/pom.xml`, `spring-boot-starter-openfeature-unleash/pom.xml`, `spring-boot-starter-openfeature/pom.xml` |
| `org.iromu.openfeature:spring-boot-starter-openfeature-envvar` | `4.1.0-SNAPSHOT` | `spring-boot-starter-openfeature-multiprovider/pom.xml` |
| `org.iromu.openfeature:spring-boot-starter-openfeature-unleash` | `4.1.0-SNAPSHOT` | `examples/unleash-advanced/pom.xml`, `examples/unleash-simple/pom.xml` |
| `org.iromu.openfeature:spring-openfeature` | `4.1.0-SNAPSHOT` | `spring-boot-openfeature-autoconfigure/pom.xml` |

## Appendix A - full resolved dependency closure

Every external coordinate appearing in the resolved closure of the reactor:
**171** coordinates, of which **70** sit behind the latest plain release.

| Coordinate | in use | scopes | release (repo) | latest plain | latest any | verdict | repo |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `ch.qos.logback:logback-classic` | `1.5.38` | compile | `1.6.3` | `1.6.3` | `1.6.3` | **behind** | central |
| `ch.qos.logback:logback-core` | `1.5.38` | compile | `1.6.3` | `1.6.3` | `1.6.3` | **behind** | central |
| `com.configcat:configcat-java-client` | `9.4.3` | compile | `10.0.1` | `10.0.1` | `10.0.1` | **behind** | central |
| `com.dylibso.chicory:log` | `1.7.5` | compile | `1.7.5` | `1.7.5` | `1.7.5` | current | central |
| `com.dylibso.chicory:runtime` | `1.7.5` | compile | `1.7.5` | `1.7.5` | `1.7.5` | current | central |
| `com.dylibso.chicory:wasi` | `1.7.5` | compile | `1.7.5` | `1.7.5` | `1.7.5` | current | central |
| `com.dylibso.chicory:wasm` | `1.7.5` | compile | `1.7.5` | `1.7.5` | `1.7.5` | current | central |
| `com.ethlo.time:itu` | `1.14.0` | compile | `1.14.0` | `1.14.0` | `1.14.0` | current | central |
| `com.fasterxml.jackson.core:jackson-annotations` | `2.21` | compile | `3.0-rc5` | `2.22` | `3.0-rc5` | **behind** | central |
| `com.fasterxml.jackson.core:jackson-core` | `2.21.5` | compile | `2.22.2` | `2.22.2` | `2.22.2` | **behind** | central |
| `com.fasterxml.jackson.core:jackson-databind` | `2.21.5` | compile | `2.22.2` | `2.22.2` | `2.22.2` | **behind** | central |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` | `2.21.5` | compile | `2.22.2` | `2.22.2` | `2.22.2` | **behind** | central |
| `com.fasterxml.jackson.datatype:jackson-datatype-jdk8` | `2.21.5` | compile | `2.22.2` | `2.22.2` | `2.22.2` | **behind** | central |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | `2.21.5` | compile | `2.22.2` | `2.22.2` | `2.22.2` | **behind** | central |
| `com.flagsmith:flagsmith-java-client` | `8.1.1` | compile | `8.1.1` | `8.1.1` | `8.1.1` | current | central |
| `com.github.ben-manes.caffeine:caffeine` | `3.2.4` | compile, runtime | `3.2.4` | `3.2.4` | `3.2.4` | current | central |
| `com.github.growthbook:growthbook-openfeature-provider-java` | `0.0.2` | compile | `0.0.2` | `0.0.2` | `0.0.2` | current | jitpack.io |
| `com.github.growthbook:growthbook-sdk-java` | `0.11.0` | compile | `0.11.0` | `0.11.0` | `0.11.0` | current | jitpack.io |
| `com.github.growthbook.growthbook-sdk-java:growthbook-cache-caffeine` | `0.11.0` | compile | `0.11.0` | `0.11.0` | `0.11.0` | current | jitpack.io |
| `com.github.growthbook.growthbook-sdk-java:growthbook-cache-jcache` | `0.11.0` | compile | `0.11.0` | `0.11.0` | `0.11.0` | current | jitpack.io |
| `com.github.growthbook.growthbook-sdk-java:lib` | `0.11.0` | compile | `0.11.0` | `0.11.0` | `0.11.0` | current | jitpack.io |
| `com.github.spotbugs:spotbugs-annotations` | `4.9.8` | compile | `4.10.4` | `4.10.4` | `4.10.4` | **behind** | central |
| `com.github.ua-parser:uap-java` | `1.6.1` | runtime | `1.6.1` | `1.6.1` | `1.6.1` | current | central |
| `com.google.android:annotations` | `4.1.1.4` | runtime | `4.1.1.4` | `4.1.1.4` | `4.1.1.4` | current | central |
| `com.google.api.grpc:proto-google-common-protos` | `2.64.1` | compile | `2.76.0` | `2.76.0` | `2.76.0` | **behind** | central |
| `com.google.code.findbugs:jsr305` | `3.0.2` | compile, runtime | `3.0.2` | `3.0.2` | `3.0.2` | current | central |
| `com.google.code.gson:gson` | `2.13.2` | compile, runtime | `2.14.0` | `2.14.0` | `2.14.0` | **behind** | central |
| `com.google.errorprone:error_prone_annotations` | `2.41.0`, `2.49.0`, `2.50.0` | compile, runtime | `2.50.0` | `2.50.0` | `2.50.0` | **behind** | central |
| `com.google.flatbuffers:flatbuffers-java` | `25.2.10` | runtime | `25.2.10` | `25.2.10` | `25.2.10` | current | central |
| `com.google.guava:failureaccess` | `1.0.2`, `1.0.3` | compile, runtime | `1.0.3` | `1.0.3` | `1.0.3` | **behind** | central |
| `com.google.guava:guava` | `33.3.1-jre`, `33.6.0-android` | compile, runtime | `33.7.1-jre` | `23.0` | `33.7.1-jre` | *ahead of repo* | central |
| `com.google.guava:listenablefuture` | `9999.0-empty-to-avoid-conflict-with-guava` | compile, runtime | `9999.0-empty-to-avoid-conflict-with-guava` | `1.0` | `9999.0-empty-to-avoid-conflict-with-guava` | current | central |
| `com.google.j2objc:j2objc-annotations` | `3.0.0`, `3.1` | compile, runtime | `3.1` | `3.1` | `3.1` | **behind** | central |
| `com.google.protobuf:protobuf-java` | `4.35.1` | compile | `4.36.1` | `4.36.1` | `4.36.1` | **behind** | central |
| `com.jayway.jsonpath:json-path` | `2.10.0` | compile, test | `3.0.0` | `3.0.0` | `3.0.0` | **behind** | central |
| `com.launchdarkly:launchdarkly-logging` | `1.1.1` | compile | `1.1.1` | `1.1.1` | `1.1.1` | current | central |
| `com.launchdarkly:okhttp-eventsource` | `4.3.0` | compile | `5.0.0` | `5.0.0` | `5.0.0` | **behind** | central |
| `com.networknt:json-schema-validator` | `1.5.9` | compile | `3.0.7` | `3.0.7` | `3.0.7` | **behind** | central |
| `com.sangupta:murmur` | `1.0.0` | compile | `1.0.0` | `1.0.0` | `1.0.0` | current | central |
| `com.squareup.okhttp3:logging-interceptor` | `5.5.0` | runtime | `5.5.0` | `5.5.0` | `5.5.0` | current | central |
| `com.squareup.okhttp3:mockwebserver` | `5.5.0` | test | `5.5.0` | `5.5.0` | `5.5.0` | current | central |
| `com.squareup.okhttp3:mockwebserver3` | `5.5.0` | test | `5.5.0` | `5.5.0` | `5.5.0` | current | central |
| `com.squareup.okhttp3:okhttp` | `5.5.0` | compile, runtime | `5.5.0` | `5.5.0` | `5.5.0` | current | central |
| `com.squareup.okhttp3:okhttp-jvm` | `5.5.0` | compile, runtime, test | `5.5.0` | `5.5.0` | `5.5.0` | current | central |
| `com.squareup.okhttp3:okhttp-sse` | `5.5.0` | runtime | `5.5.0` | `5.5.0` | `5.5.0` | current | central |
| `com.squareup.okio:okio` | `3.18.1`, `3.4.0` | compile, runtime | `3.18.2` | `3.18.2` | `3.18.2` | **behind** | central |
| `com.squareup.okio:okio-jvm` | `3.18.1`, `3.4.0` | compile, runtime, test | `3.18.2` | `3.18.2` | `3.18.2` | **behind** | central |
| `com.statsig:ip3country` | `0.1.5` | runtime | `0.1.6` | `0.1.6` | `0.1.6` | **behind** | central |
| `com.statsig:serversdk` | `1.18.1` | compile | `3.2.0` | `3.2.0` | `3.2.0` | **behind** | central |
| `com.vaadin.external.google:android-json` | `0.0.20131108.vaadin1` | test | `0.0.20131108.vaadin1` | *n/a* | `0.0.20131108.vaadin1` | current | central |
| `commons-codec:commons-codec` | `1.21.0` | compile | `1.22.1` | `1.22.1` | `1.22.1` | **behind** | central |
| `commons-logging:commons-logging` | `1.3.5`, `1.3.6` | compile | `1.4.0` | `1.4.0` | `1.4.0` | **behind** | central |
| `de.skuzzle:semantic-version` | `2.1.0` | compile | `2.1.1` | `2.1.1` | `2.1.1` | **behind** | central |
| `dev.openfeature:sdk` | `1.22.1` | compile | `1.22.1` | `1.22.1` | `1.22.1` | current | central |
| `dev.openfeature.contrib.providers:configcat` | `0.2.1` | compile | `0.2.1` | `0.2.1` | `0.2.1` | current | central |
| `dev.openfeature.contrib.providers:env-var` | `0.0.12` | compile, test | `0.0.12` | `0.0.12` | `0.0.12` | current | central |
| `dev.openfeature.contrib.providers:flagd` | `0.14.1` | compile | `0.14.1` | `0.14.1` | `0.14.1` | current | central |
| `dev.openfeature.contrib.providers:flagsmith` | `0.0.13` | compile | `0.0.13` | `0.0.13` | `0.0.13` | current | central |
| `dev.openfeature.contrib.providers:flipt` | `0.1.4` | compile | `0.1.4` | `0.1.4` | `0.1.4` | current | central |
| `dev.openfeature.contrib.providers:go-feature-flag` | `1.2.1` | compile | `1.2.1` | `1.2.1` | `1.2.1` | current | central |
| `dev.openfeature.contrib.providers:jsonlogic-eval-provider` | `1.3.0` | compile | `1.3.0` | `1.3.0` | `1.3.0` | current | central |
| `dev.openfeature.contrib.providers:multiprovider` | `0.0.3` | compile | `0.0.3` | `0.0.3` | `0.0.3` | current | central |
| `dev.openfeature.contrib.providers:statsig` | `0.2.1` | compile | `0.2.1` | `0.2.1` | `0.2.1` | current | central |
| `dev.openfeature.contrib.providers:unleash` | `0.1.3-alpha` | compile | `0.1.3-alpha` | *n/a* | `0.1.3-alpha` | current | central |
| `dev.openfeature.contrib.tools:flagd-api` | `1.0.0` | compile | `1.0.0` | `1.0.0` | `1.0.0` | current | central |
| `dev.openfeature.contrib.tools:flagd-core` | `2.0.1` | compile | `2.0.1` | `2.0.1` | `2.0.1` | current | central |
| `io.flipt:flipt-java` | `1.1.2` | compile | `1.4.0` | `1.4.0` | `1.4.0` | **behind** | central |
| `io.getunleash:unleash-client-java` | `12.3.0` | compile | `12.3.0` | `12.3.0` | `12.3.0` | current | central |
| `io.getunleash:yggdrasil-engine` | `1.0.3` | compile | `1.0.3` | `1.0.3` | `1.0.3` | current | central |
| `io.github.gzsombor:json-logic-java` | `1.1.3`, `1.1.5` | compile | `1.1.6` | `1.1.6` | `1.1.6` | **behind** | central |
| `io.grpc:grpc-api` | `1.83.1` | compile | `1.84.0` | `1.84.0` | `1.84.0` | **behind** | central |
| `io.grpc:grpc-context` | `1.83.1` | runtime | `1.84.0` | `1.84.0` | `1.84.0` | **behind** | central |
| `io.grpc:grpc-core` | `1.83.1` | compile | `1.84.0` | `1.84.0` | `1.84.0` | **behind** | central |
| `io.grpc:grpc-netty-shaded` | `1.83.1` | compile | `1.84.0` | `1.84.0` | `1.84.0` | **behind** | central |
| `io.grpc:grpc-protobuf` | `1.83.1` | compile | `1.84.0` | `1.84.0` | `1.84.0` | **behind** | central |
| `io.grpc:grpc-protobuf-lite` | `1.83.1` | runtime | `1.84.0` | `1.84.0` | `1.84.0` | **behind** | central |
| `io.grpc:grpc-stub` | `1.83.1` | compile | `1.84.0` | `1.84.0` | `1.84.0` | **behind** | central |
| `io.grpc:grpc-util` | `1.83.1` | runtime | `1.84.0` | `1.84.0` | `1.84.0` | **behind** | central |
| `io.micrometer:micrometer-commons` | `1.17.1` | compile | `1.18.0-M1` | `1.17.1` | `1.18.0-M1` | current | central |
| `io.micrometer:micrometer-core` | `1.17.1` | compile | `1.18.0-M1` | `1.17.1` | `1.18.0-M1` | current | central |
| `io.micrometer:micrometer-jakarta9` | `1.17.1` | compile | `1.18.0-M1` | `1.17.1` | `1.18.0-M1` | current | central |
| `io.micrometer:micrometer-observation` | `1.17.1` | compile | `1.18.0-M1` | `1.17.1` | `1.18.0-M1` | current | central |
| `io.opentelemetry:opentelemetry-api` | `1.62.0` | compile | `1.66.0` | `1.66.0` | `1.66.0` | **behind** | central |
| `io.opentelemetry:opentelemetry-common` | `1.62.0` | compile | `1.66.0` | `1.66.0` | `1.66.0` | **behind** | central |
| `io.opentelemetry:opentelemetry-context` | `1.62.0` | compile | `1.66.0` | `1.66.0` | `1.66.0` | **behind** | central |
| `io.perfmark:perfmark-api` | `0.27.0` | runtime | `0.27.0` | `0.27.0` | `0.27.0` | current | central |
| `io.reactivex.rxjava3:rxjava` | `3.1.12` | compile | `3.1.12` | `3.1.12` | `3.1.12-RC1` | current | central |
| `jakarta.activation:jakarta.activation-api` | `2.1.4` | test | `2.2.0-M2` | `2.1.4` | `2.2.0-M2` | current | central |
| `jakarta.annotation:jakarta.annotation-api` | `3.0.0` | compile | `3.0.0` | `3.0.0` | `3.0.0-M1` | current | central |
| `jakarta.xml.bind:jakarta.xml.bind-api` | `4.0.5` | test | `4.1.0-M1` | `4.0.5` | `4.1.0-M1` | current | central |
| `javax.cache:cache-api` | `1.1.1` | compile | `1.1.1` | `1.1.1` | `1.1.1` | current | central |
| `junit:junit` | `4.13.2` | test | `4.13.2` | `4.13.2` | `4.13.2` | current | central |
| `net.bytebuddy:byte-buddy` | `1.18.11` | compile, test | `1.18.13-jdk5` | `1.18.13` | `1.18.13-jdk5` | **behind** | central |
| `net.bytebuddy:byte-buddy-agent` | `1.18.11` | test | `1.18.13-jdk5` | `1.18.13` | `1.18.13-jdk5` | **behind** | central |
| `net.minidev:accessors-smart` | `2.6.0` | runtime, test | `2.6.0` | `2.6.0` | `2.6.0` | current | central |
| `net.minidev:json-smart` | `2.6.0` | runtime, test | `2.6.0` | `2.6.0` | `2.6.0` | current | central |
| `org.apache.commons:commons-collections4` | `4.4`, `4.5.0` | compile, runtime | `4.6.0` | `4.6.0` | `4.6.0` | **behind** | central |
| `org.apache.commons:commons-lang3` | `3.20.0` | compile, test | `3.20.0` | `3.20.0` | `3.20.0` | current | central |
| `org.apache.commons:commons-math3` | `3.6.1` | runtime | `3.6.1` | `3.6.1` | `3.6.1` | current | central |
| `org.apache.logging.log4j:log4j-api` | `2.25.5` | compile | `3.0.0-beta2` | `2.26.1` | `3.0.0-beta2` | **behind** | central |
| `org.apache.logging.log4j:log4j-to-slf4j` | `2.25.5` | compile | `3.0.0-beta2` | `2.26.1` | `3.0.0-beta2` | **behind** | central |
| `org.apache.maven:maven-artifact` | `3.6.3` | compile | `4.0.0-rc-6` | `3.9.16` | `4.0.0-rc-6` | **behind** | central |
| `org.apiguardian:apiguardian-api` | `1.1.2` | test | `1.1.2` | `1.1.2` | `1.1.2` | current | central |
| `org.aspectj:aspectjweaver` | `1.9.25.1` | compile | `1.9.25` | `1.9.25.1` | `1.9.25.1` | current | central |
| `org.assertj:assertj-core` | `3.27.7` | test | `4.0.0-M1` | `3.27.7` | `4.0.0-M1` | current | central |
| `org.awaitility:awaitility` | `4.3.0` | test | `4.3.0` | `4.3.0` | `4.3.0` | current | central |
| `org.checkerframework:checker-qual` | `3.43.0` | runtime | `4.2.3` | `4.2.3` | `4.2.3` | **behind** | central |
| `org.codehaus.mojo:animal-sniffer-annotations` | `1.27` | compile | `1.28` | `1.28` | `1.28` | **behind** | central |
| `org.codehaus.plexus:plexus-utils` | `3.2.1` | compile | `4.1.0` | `4.1.0` | `4.1.0` | **behind** | central |
| `org.hamcrest:hamcrest` | `3.0` | test | `3.0` | `3.0` | `3.0-rc1` | current | central |
| `org.hamcrest:hamcrest-core` | `3.0` | test | `3.0` | `3.0` | `3.0-rc1` | current | central |
| `org.hdrhistogram:HdrHistogram` | `2.2.2` | runtime | `2.2.2` | `2.2.2` | `2.2.2` | current | central |
| `org.jetbrains:annotations` | `13.0`, `23.0.0`, `24.0.0` | compile, runtime, test | `26.1.0` | `26.1.0` | `26.1.0` | **behind** | central |
| `org.jetbrains.kotlin:kotlin-stdlib` | `2.3.21` | compile, runtime, test | `2.4.20` | `2.4.20` | `2.4.20-RC3` | **behind** | central |
| `org.jetbrains.kotlin:kotlin-stdlib-common` | `1.8.0` | compile | `2.4.20` | `2.4.20` | `2.4.20-RC3` | **behind** | central |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7` | `2.3.21` | compile | `2.4.20` | `2.4.20` | `2.4.20-RC3` | **behind** | central |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8` | `2.3.21` | compile | `2.4.20` | `2.4.20` | `2.4.20-RC3` | **behind** | central |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm` | `1.10.2` | runtime | `1.11.0` | `1.11.0` | `1.11.0-rc02` | **behind** | central |
| `org.jetbrains.kotlinx:kotlinx-coroutines-jdk8` | `1.10.2` | runtime | `1.11.0` | `1.11.0` | `1.11.0-rc02` | **behind** | central |
| `org.json:json` | `20250517` | compile | `20260814` | `20260814` | `20260814` | **behind** | central |
| `org.jspecify:jspecify` | `1.0.1` | compile | `1.0.1` | `1.0.1` | `1.0.1` | current | central |
| `org.junit.jupiter:junit-jupiter` | `6.0.3` | test | `6.1.3` | `6.1.3` | `6.1.3` | **behind** | central |
| `org.junit.jupiter:junit-jupiter-api` | `6.0.3` | test | `6.1.3` | `6.1.3` | `6.1.3` | **behind** | central |
| `org.junit.jupiter:junit-jupiter-engine` | `6.0.3` | test | `6.1.3` | `6.1.3` | `6.1.3` | **behind** | central |
| `org.junit.jupiter:junit-jupiter-params` | `6.0.3` | test | `6.1.3` | `6.1.3` | `6.1.3` | **behind** | central |
| `org.junit.platform:junit-platform-commons` | `6.0.3` | test | `6.1.3` | `6.1.3` | `6.1.3` | **behind** | central |
| `org.junit.platform:junit-platform-engine` | `6.0.3` | test | `6.1.3` | `6.1.3` | `6.1.3` | **behind** | central |
| `org.mockito:mockito-core` | `5.23.0` | test | `5.23.0` | `5.23.0` | `5.23.0` | current | central |
| `org.mockito:mockito-junit-jupiter` | `5.23.0` | test | `5.23.0` | `5.23.0` | `5.23.0` | current | central |
| `org.objenesis:objenesis` | `3.3` | test | `3.6` | `3.6` | `3.6` | **behind** | central |
| `org.opentest4j:opentest4j` | `1.3.0` | test | `1.3.0` | `1.3.0` | `1.3.0-RC2` | current | central |
| `org.ow2.asm:asm` | `9.7.1` | runtime, test | `9.10.1` | `9.10.1` | `9.10.1` | **behind** | central |
| `org.projectlombok:lombok` | `1.18.46` | provided | `1.18.48` | `1.18.48` | `1.18.48` | **behind** | central |
| `org.reactivestreams:reactive-streams` | `1.0.4` | compile | `1.0.4` | `1.0.4` | `1.0.4` | current | central |
| `org.semver4j:semver4j` | `5.8.0` | compile | `6.0.0` | `6.0.0` | `6.0.0` | **behind** | central |
| `org.skyscreamer:jsonassert` | `1.5.3` | test | `2.0-rc1` | `1.5.3` | `2.0-rc1` | current | central |
| `org.slf4j:jul-to-slf4j` | `2.0.18` | compile | `2.1.0-alpha1` | `2.0.19` | `2.1.0-alpha1` | **behind** | central |
| `org.slf4j:slf4j-api` | `2.0.18` | compile | `2.1.0-alpha1` | `2.0.19` | `2.1.0-alpha1` | **behind** | central |
| `org.springframework:spring-aop` | `7.0.9` | compile | `7.1.0-M1` | `7.0.9` | `7.1.0-M1` | current | central |
| `org.springframework:spring-beans` | `7.0.9` | compile | `7.1.0-M1` | `7.0.9` | `7.1.0-M1` | current | central |
| `org.springframework:spring-context` | `7.0.9` | compile | `7.1.0-M1` | `7.0.9` | `7.1.0-M1` | current | central |
| `org.springframework:spring-core` | `7.0.9` | compile | `7.1.0-M1` | `7.0.9` | `7.1.0-M1` | current | central |
| `org.springframework:spring-expression` | `7.0.9` | compile | `7.1.0-M1` | `7.0.9` | `7.1.0-M1` | current | central |
| `org.springframework:spring-test` | `7.0.9` | test | `7.1.0-M1` | `7.0.9` | `7.1.0-M1` | current | central |
| `org.springframework:spring-web` | `7.0.9` | compile | `7.1.0-M1` | `7.0.9` | `7.1.0-M1` | current | central |
| `org.springframework.boot:spring-boot` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-actuator` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-actuator-autoconfigure` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-autoconfigure` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-autoconfigure-processor` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-configuration-processor` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-health` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-micrometer-metrics` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-micrometer-observation` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-security` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter-actuator` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter-aspectj` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter-logging` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter-micrometer-metrics` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter-security` | `4.1.1` | compile | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-starter-test` | `4.1.1` | test | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-test` | `4.1.1` | test | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.boot:spring-boot-test-autoconfigure` | `4.1.1` | test | `4.2.0-M1` | `4.1.1` | `4.2.0-M1` | current | central |
| `org.springframework.security:spring-security-config` | `7.1.1` | compile | `7.2.0-M1` | `7.1.1` | `7.2.0-M1` | current | central |
| `org.springframework.security:spring-security-core` | `7.1.1` | compile | `7.2.0-M1` | `7.1.1` | `7.2.0-M1` | current | central |
| `org.springframework.security:spring-security-crypto` | `7.1.1` | compile | `7.2.0-M1` | `7.1.1` | `7.2.0-M1` | current | central |
| `org.springframework.security:spring-security-test` | `7.1.1` | test | `7.2.0-M1` | `7.1.1` | `7.2.0-M1` | current | central |
| `org.springframework.security:spring-security-web` | `7.1.1` | compile | `7.2.0-M1` | `7.1.1` | `7.2.0-M1` | current | central |
| `org.xmlunit:xmlunit-core` | `2.11.0` | test | `2.13.0` | `2.13.0` | `2.13.0` | **behind** | central |
| `org.yaml:snakeyaml` | `2.6` | compile | `2.7` | `2.7` | `2.7` | **behind** | central |

## Notes and caveats

- `versions-maven-plugin:display-property-updates` reports **no** property updates for
  this reactor. That goal suppresses *major* upgrades by default, so it cannot and does
  not agree with the **behind** rows above; the tables here come from the raw
  `maven-metadata.xml` version lists and therefore do include major jumps.
- `dev.openfeature:sdk` and the `dev.openfeature.contrib.providers:*` artifacts are
  compiled against one another and break at **runtime**, not compile time, when they
  drift apart. Upgrade that family as one lockstep change and re-run the whole reactor
  afterwards - modules after the first failure are skipped, which hides every
  downstream provider module.
- *pre-release/qualified pin* is informational: the build deliberately pins a qualified
  version (an `-alpha`, `-rc`, `-jre`, `-android` ... build). Compare `latest plain`
  before treating it as a defect.
- *by BOM* in the `pinned` column means the POM states no version; it arrives via
  `spring-boot-dependencies` or `spring-boot-openfeature-dependencies`. The `in use`
  column is the truth for those rows.
- A coordinate listed with several `in use` values resolves differently per module; all
  observed versions are shown in one row.
- *not found* means no repository declared by the build returned content-verified
  metadata for that coordinate.
- A local `~/.m2` cache directory is **not** evidence that a version exists. A folder
  holding only `<artifact>-<version>.jar.lastUpdated` / `.pom.lastUpdated` markers is a
  record of a *failed* fetch, not an artifact. The GrowthBook SDK cache shows a
  `0.11.1` directory of exactly that kind while the repository index publishes no such
  version (`<release>` is `0.11.0`, and `0.11.1` is absent from its version list) - so
  trust the `maven-metadata.xml` columns above over a directory listing.
- The `examples/` aggregator is **profile gated** (`-P examples`) and parents off
  `examples/pom.xml`, not the root pom. Its coordinates are audited here from the POM
  bytes, but they are absent from `in use` and from any default-reactor Maven report,
  because the default reactor does not build that tree. Its own `revision` and
  `sprindoc-openapi.version` properties resolve through that parent chain.
- Coverage: plugins, properties, parents/BOM imports and declared dependencies are the
  audit proper; Appendix A is the transitive closure, included so an upgrade review can
  see what a bump would actually move.

## Reproducing

The pipeline lives beside this document under `docs/version-audit/`; scratch data
(fetched metadata, logs) is written to the ignored `.qwen/tmp/`.

```bash
python3 docs/version-audit/step1_coords.py
# let maven log its own repository traffic, then read the roots out of that log
./mvnw -B -U org.codehaus.mojo:versions-maven-plugin:2.21.0:display-property-updates \
    > .qwen/tmp/props.log 2>&1
./mvnw -B dependency:list > .qwen/tmp/deplist.log 2>&1
# maven's effective-pom print, for the version a parent pins on a plugin that
# declares none itself; the examples reactor is profile gated so ask for it too
./mvnw -B -P examples help:effective-pom > .qwen/tmp/effpom.log 2>&1
python3 docs/version-audit/step2_download.py
python3 docs/version-audit/step3_doc.py
```

Two self checks ship with it; both are independent of the renderer:

```bash
# re-derives the ordering with a separate brute force implementation and checks
# maximality of every 'latest any' value against its own version list
python3 docs/version-audit/verify_ordering.py
# compares the 'behind' verdicts against maven's own versions plugin report
python3 docs/version-audit/crosscheck.py
```

The build needs JDK 17 (`JAVA_HOME` pointing at a 17 install); the machine default
`java` is newer and breaks Lombok annotation processing.

