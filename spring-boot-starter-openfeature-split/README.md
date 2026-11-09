# spring-boot-starter-openfeature-split

Spring Boot starter that integrates the [Split.io](https://www.split.io/) feature-flag platform via the
[Split OpenFeature Java provider](https://github.com/splitio/split-openfeature-provider-java).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-split</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-split'
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

All properties are under the `spring.openfeature.split` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `authorization-key` | (mandatory) Split authorization key used to authenticate against the Split service. | — |
| `domain` | Optional domain (endpoint) override for the Split service; applied to both the SDK and the events endpoints. | — |

## Example Configuration

```yaml
spring:
  openfeature:
    split:
      authorization-key: ${SPLIT_AUTHORIZATION_KEY}
```

### Non-prod environment

Point the client at a different Split domain (e.g. the EU region):

```yaml
spring:
  openfeature:
    split:
      authorization-key: ${SPLIT_AUTHORIZATION_KEY}
      domain: https://api.eu.split.io
```

## Usage

Inject the auto-configured `Client` and evaluate flags:

```java
import dev.openfeature.sdk.Client;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

Or use the `@ToggleOnFlag` annotation:

```java
import org.iromu.openfeature.boot.aop.ToggleOnFlag;

@GetMapping("/feature/annotated/user/{id}")
@ToggleOnFlag(key = "users-flag", attributes = "{'userId': #id}", orElse = "disabled")
public String enabled(@PathVariable("id") final String id) {
    return "Feature is ON for user " + id;
}

public String disabled(final String id) {
    return "Feature is OFF for user " + id;
}
```

## Customization

Implement `SplitCustomizer` to tweak the underlying Split SDK client configuration:

```java
import io.split.client.SplitClientConfig;
import org.iromu.openfeature.boot.split.SplitCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SplitConfig {

    @Bean
    public SplitCustomizer splitCustomizer() {
        return (builder) -> builder
            .setBlockUntilReadyTimeout(10000)
            .featuresRefreshRate(30);
    }
}
```
