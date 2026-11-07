# spring-boot-starter-openfeature-multiprovider

Spring Boot starter that integrates the [OpenFeature MultiProvider](https://github.com/open-feature/java-sdk-contrib/tree/main/providers/multiprovider).
It delegates flag evaluation across multiple `FeatureProvider` beans, using a configurable strategy to pick
which provider resolves each flag.

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-multiprovider</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-multiprovider'
```

Import the [BOM](../spring-boot-openfeature-dependencies) to manage the version:

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

## Configuration Properties

The multi-provider starter has no dedicated `spring.openfeature.*` properties of its own. It activates
automatically when the `MultiProvider` class is on the classpath and at least one `FeatureProvider` bean
exists in the context.

## How It Works

When you add multiple provider starters (e.g. `spring-boot-starter-openfeature-unleash` and
`spring-boot-starter-openfeature-configcat`), each registers its own `FeatureProvider` bean. The
`MultiProvider` collects all `FeatureProvider` beans and delegates flag evaluation according to a
`Strategy` bean.

By default, the auto-configuration registers a `FirstMatchStrategy`, which asks each provider in order
and returns the first one that can resolve the flag.

### Custom Strategy

Provide your own `Strategy` bean to change the delegation logic:

```java
import dev.openfeature.contrib.providers.multiprovider.Strategy;
import dev.openfeature.contrib.providers.multiprovider.EvaluationDetails;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.EvaluationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiProviderConfig {

    @Bean
    public Strategy customStrategy() {
        return (providers, context, flagKey) -> {
            // Custom logic: e.g. route all "billing.*" flags to the second provider
            if (flagKey.startsWith("billing.")) {
                return providers.get(1);
            }
            return providers.get(0);
        };
    }
}
```

## Example: Two Providers

```xml
<dependencies>
    <dependency>
        <groupId>org.iromu.openfeature</groupId>
        <artifactId>spring-boot-starter-openfeature-multiprovider</artifactId>
    </dependency>
    <dependency>
        <groupId>org.iromu.openfeature</groupId>
        <artifactId>spring-boot-starter-openfeature-unleash</artifactId>
    </dependency>
    <dependency>
        <groupId>org.iromu.openfeature</groupId>
        <artifactId>spring-boot-starter-openfeature-configcat</artifactId>
    </dependency>
</dependencies>
```

```yaml
spring:
  openfeature:
    unleash:
      app-name: my-app
      environment: production
      unleash-api: http://unleash:4242/api/
      unleash-token: 'prod:my-token'
    config-cat:
      sdk-key: your-configcat-key
```

## Usage

The `Client` bean is created by `MultiProviderAutoConfiguration` and bound to the `MultiProvider`.
Usage is the same as with any single-provider starter:

```java
import dev.openfeature.sdk.Client;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeatureController {

    private final Client client;

    public FeatureController(Client client) {
        this.client = client;
    }

    @GetMapping("/feature/{name}")
    public Boolean feature(@PathVariable("name") final String name) {
        return this.client.getBooleanValue(name, false);
    }
}
```
