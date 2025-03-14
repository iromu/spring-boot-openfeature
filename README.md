# Spring Starter OpenFeature

[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)
![Build Status](https://github.com/iromu/spring-boot-openfeature/actions/workflows/snapshots.yml/badge.svg?branch=main)
![Sonar Coverage](https://img.shields.io/sonar/coverage/iromu_spring-boot-openfeature?server=https%3A%2F%2Fsonarcloud.io)
[![Coveralls](https://img.shields.io/coverallsCoverage/github/iromu/spring-boot-openfeature)](https://coveralls.io/github/iromu/spring-boot-openfeature?branch=main)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=iromu_spring-boot-openfeature&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=iromu_spring-boot-openfeature)
[![Maven Central](https://img.shields.io/maven-central/v/org.iromu.openfeature/spring-boot-openfeature?label=release)](https://repo1.maven.org/maven2/org/iromu/openfeature/)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Forg%2Firomu%2Fopenfeature%2Fspring-boot-openfeature%2Fmaven-metadata.xml&label=snapshot)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/org/iromu/openfeature/)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/iromu/spring-boot-openfeature/badge)](https://securityscorecards.dev/viewer/?uri=github.com/iromu/spring-boot-openfeature)

This is a Spring Boot starter that integrates [OpenFeature](https://openfeature.dev/), an open standard for feature flag
management.

## Features

- **OpenFeature Integration**: Easily toggle features and manage feature flags in your application.
- **Spring Boot Framework**: Built with Spring Boot for rapid development.
- **Extensibility**: Supports multiple feature flag providers via OpenFeature SDK.

## Prerequisites

Before running the application, ensure you have the following installed:

- Java 17 or higher
- Maven 3.8++

## Getting Started

### 1. Clone the Repository

```bash
$ git clone https://github.com/iromu/spring-boot-openfeature.git
$ cd spring-boot-openfeature
```

### 2. Configure OpenFeature Provider

OpenFeature allows you to integrate with a feature flag management provider. Update the configuration in
`application.properties` or `application.yml`.

For example, if you're using Unleash:

```properties
spring:
    openfeature:
        unleash:
            app-name:${spring.application.name}
            environment:development
            unleash-api:http://unleash-instance:54242/api/
            unleash-token:'default:development.your-api-key'
    application:
        name:UnleashApplication
```

Refer to the provider's documentation for specific configuration details.

### 3. Build and Run the Example Applications

Using Maven:

```bash
$ cd examples
$ mvn clean package
```

### 4. Testing Feature Flags

To test feature flags, create a simple feature toggle in your provider and use OpenFeature APIs in your code. Example:

```java
import dev.openfeature.sdk.Client;

@RestController
public class FeatureController {

    private final Client client;
    
    public FeatureController(Client client) {
        this.client = client;
    }
    
    @GetMapping("/feature-status")
    public String getFeatureStatus() {
        boolean isFeatureEnabled = client.getBooleanValue("my-feature", false);
        return isFeatureEnabled ? "Feature is enabled!" : "Feature is disabled.";
    }

    @GetMapping("/user/{id}")
    public Boolean featureOnUserId(@PathVariable("id") final String id) {
        return client.getBooleanValue("users-flag", false, new ImmutableContext(Map.of("userId", new Value(id))));
    }
}
```

Navigate to `/feature-status` to see the feature toggle in action.



### 5. Annotations

With a strategy defined like:

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
@RestController
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

OpenFeature supports multiple feature flag providers, including:

- Unleash
- Split
- ...

To switch providers, replace the dependency and update configuration as per the provider's documentation.

## Dependencies

Key dependencies for this project:

- Spring Boot Starter Web
- Spring Boot OpenFeature Starter
- OpenFeature Provider (e.g., Unleash or Split)

Add the OpenFeature SDK and provider dependencies to your `pom.xml` or `build.gradle`:

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
            <version>${spring-boot-openfeature.version}</version>
        </dependency>
    </dependencies>
</project>
```

**Gradle**

```groovy
dependencyManagement {
    imports {
        mavenBom "org.iromu.openfeature:spring-boot-openfeature-dependencies:${springBootOpenFeatureDependenciesVersion}"
    }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.iromu.openfeature:spring-boot-starter-openfeature'
    implementation 'dev.openfeature.contrib.providers:unleash'
}
```

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a new branch
3. Make your changes and test thoroughly
4. Submit a pull request

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE.txt) file for details.

## Resources

- [OpenFeature Documentation](https://docs.openfeature.dev/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Unleash Documentation](https://docs.getunleash.io/)

---

Happy coding! 🚀
