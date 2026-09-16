# Spring Boot OpenFeature

![Build Status](https://github.com/iromu/spring-boot-openfeature/actions/workflows/release.yml/badge.svg?branch=main)![Build Status](https://github.com/iromu/spring-boot-openfeature/actions/workflows/snapshots.yml/badge.svg?branch=main)
[![Coveralls](https://img.shields.io/coverallsCoverage/github/iromu/spring-boot-openfeature)](https://coveralls.io/github/iromu/spring-boot-openfeature?branch=main)

[![Maven Central](https://img.shields.io/maven-central/v/org.iromu.openfeature/spring-boot-openfeature?label=Release)](https://repo1.maven.org/maven2/org/iromu/openfeature/)
[![Maven Snapshot](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Forg%2Firomu%2Fopenfeature%2Fspring-boot-openfeature%2Fmaven-metadata.xml&label=Snapshot)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/org/iromu/openfeature/)

This is a Spring Boot starter that integrates [OpenFeature](https://openfeature.dev/), an open standard for feature flag
management. Add one starter dependency, set a few `spring.openfeature.*` properties, and feature flags work in your
application — no manual wiring of the OpenFeature SDK.

## Features

- **Auto-Configuration** — a ready-made `dev.openfeature.sdk.Client` bean, created as soon as a `FeatureProvider` bean
  is on the context.
- **Declarative Flags** — the `@ToggleOnFlag` annotation gates method execution on a flag, with SpEL evaluation context
  and an `orElse` fallback method.
- **Health Indicator** — a Spring Boot Actuator health check for the configured flag provider.
- **Provider Starters** — one starter module per feature-flag backend, each configured under its own
  `spring.openfeature.<provider>.*` prefix.
- **Extension Points** — `ClientCustomizer`, `OpenFeatureAPICustomizer`, and per-provider customizers let you tweak
  the auto-configured beans.
- **Security Integration** — when Spring Security is on the classpath, `userId` and `authorities` are automatically
  added to every flag evaluation context.

## Prerequisites

- Java 17
- Maven (the repository ships the Maven wrapper `./mvnw`)

## Getting Started

### 1. Add the Dependency

Every provider has its own starter. For Unleash:

**Maven**

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.iromu.openfeature</groupId>
            <artifactId>spring-boot-starter-openfeature-unleash</artifactId>
        </dependency>
    </dependencies>

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
</project>
```

**Gradle**

```groovy
dependencyManagement {
    imports {
        mavenBom "org.iromu.openfeature:spring-boot-openfeature-dependencies:${springBootOpenFeatureVersion}"
    }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-unleash'
}
```

The BOM (`spring-boot-openfeature-dependencies`) pins the OpenFeature SDK and every provider library version, so you
do not need to declare versions yourself.

### 2. Configure the Provider

Configure the provider under its `spring.openfeature.<provider>.*` prefix in `application.yml` (or
`application.properties`). For example, Unleash:

```yaml
spring:
  application:
    name: UnleashApplication
  openfeature:
    unleash:
      app-name: ${spring.application.name}
      environment: development
      unleash-api: http://unleash-instance:4242/api/
      unleash-token: 'default:development.your-api-key'
```

Each provider starter also accepts an `enabled` property (`spring.openfeature.<provider>.enabled`) to switch its
auto-configuration on or off. Refer to the provider's own documentation for the full set of options.

### 3. Use the `Client`

Inject the auto-configured `Client` and evaluate flags:

```java
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.Value;

@RestController
@RequestMapping("/feature")
public class FeatureController {

    private final Client client;

    public FeatureController(Client client) {
        this.client = client;
    }

    @GetMapping("{name}")
    public Boolean feature(@PathVariable("name") final String name) {
        return this.client.getBooleanValue(name, false);
    }

    @GetMapping("user/{id}")
    public Boolean featureOnUserId(@PathVariable("id") final String id) {
        return this.client.getBooleanValue("users-flag", false,
                new ImmutableContext(Map.of("userId", new Value(id))));
    }
}
```

### 4. Use `@ToggleOnFlag`

The `@ToggleOnFlag` annotation (from `org.iromu.openfeature.boot.aop`) conditionally executes a method based on a
flag. `attributes` accepts a SpEL map that is evaluated into the flag evaluation context, and `orElse` names a
fallback method invoked when the flag evaluates to `false`.

Given an Unleash flag defined as:

```json
{
  "name": "users-flag",
  "enabled": true,
  "strategies": [
    {
      "name": "userWithId",
      "parameters": {
        "userIds": "111,234"
      }
    }
  ]
}
```

```java
import org.iromu.openfeature.boot.aop.ToggleOnFlag;

@RestController
@RequestMapping("/feature")
public class UserController {

    @GetMapping("annotated/user/{id}")
    @ToggleOnFlag(key = "users-flag", attributes = "{'userId': #id}", orElse = "featureOnUserIdDisabled")
    public String featureOnUserIdAnnotated(@PathVariable("id") final String id) {
        return "User allowed";
    }

    public String featureOnUserIdDisabled(final String id) {
        return "User not allowed";
    }
}
```

## OpenFeature Providers

Each backend has its own starter module under `org.iromu.openfeature`:

| Provider      | Starter artifact                                     |
|---------------|------------------------------------------------------|
| ConfigCat     | `spring-boot-starter-openfeature-configcat`          |
| Env Var       | `spring-boot-starter-openfeature-envvar`             |
| flagd         | `spring-boot-starter-openfeature-flagd`              |
| Flagsmith     | `spring-boot-starter-openfeature-flagsmith`          |
| Flipt         | `spring-boot-starter-openfeature-flipt`              |
| GoFeatureFlag | `spring-boot-starter-openfeature-gofeatureflag`      |
| GrowthBook    | `spring-boot-starter-openfeature-growthbook`         |
| JsonLogic     | `spring-boot-starter-openfeature-jsonlogic`          |
| Multi-Provider| `spring-boot-starter-openfeature-multiprovider`      |
| Statsig       | `spring-boot-starter-openfeature-statsig`            |
| Unleash       | `spring-boot-starter-openfeature-unleash`            |

To switch providers, replace the starter dependency and update the configuration as per the provider's documentation.

## Example Applications

The `examples/` directory contains two runnable applications (built with the `examples` profile or as a separate
reactor; they resolve the starters from the local build or the snapshot repository):

- **`unleash-simple`** — runs on port `9998` with a bundled in-memory fake Unleash provider; no external server
  needed.
- **`unleash-advanced`** — runs on port `9999` against a mocked Unleash API server, using a local `features.json`
  backup file.

```bash
$ cd examples
$ mvn clean package
```

Then run the packaged application, for example:

```bash
$ java -jar unleash-simple/target/unleash-simple-${revision}.jar
```

Try the endpoints (`unleash-simple`):

```bash
$ curl http://localhost:9998/feature/random-boolean-flag
$ curl http://localhost:9998/feature/greet/world
```

And with `unleash-advanced` running on `9999`:

```bash
$ curl http://localhost:9999/feature/annotated/user/111
```

## Building the Project

All commands use the Maven wrapper from the repository root:

```bash
# Full build (compile, checkstyle, formatting, tests, coverage)
$ ./mvnw clean verify

# Skip tests
$ ./mvnw clean verify -DskipTests

# Build a single module and its dependencies
$ ./mvnw -pl spring-boot-starter-openfeature-unleash -am clean verify
```

Code style is enforced by Spring Java Format and Checkstyle, which run automatically during the `validate` phase.

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for the full guide on reporting bugs, suggesting
features, code style, testing, and the pull request process.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE.txt) file for details.

## Resources

- [OpenFeature Documentation](https://docs.openfeature.dev/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Unleash Documentation](https://docs.getunleash.io/)
