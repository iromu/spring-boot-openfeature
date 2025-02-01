# Spring Starter Project with OpenFeature
[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)
![Build Status](https://github.com/iromu/spring-boot-openfeature/actions/workflows/snapshots.yml/badge.svg?branch=main)


This is a Spring Boot starter project that demonstrates the integration of [OpenFeature](https://openfeature.dev/), an open standard for feature flag management.

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

OpenFeature allows you to integrate with a feature flag management provider. Update the configuration in `application.properties` or `application.yml`.

For example, if you're using Unleash:

```properties
spring:
  openfeature:
    unleash:
      app-name: ${spring.application.name}
      environment: development
      unleash-api: http://unleash-instance:54242/api/
      unleash-token: 'default:development.your-api-key'
  application:
    name: UnleashApplication
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
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.iromu.openfeature</groupId>
            <artifactId>spring-boot-openfeature-dependencies</artifactId>
            <version>${spring-boot-openfeature-dependencies.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.iromu.openfeature</groupId>
        <artifactId>spring-boot-starter-openfeature</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.openfeature.contrib.providers</groupId>
        <artifactId>unleash</artifactId>
    </dependency>
</dependencies>
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
