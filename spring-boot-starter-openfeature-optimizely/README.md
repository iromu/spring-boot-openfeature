# spring-boot-starter-openfeature-optimizely

Spring Boot starter that integrates the [Optimizely](https://www.optimizely.com/) feature-flag provider via the
[Optimizely OpenFeature provider](https://github.com/open-feature/java-sdk-contrib/tree/main/providers/optimizely).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-optimizely</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-optimizely'
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

All properties are under the `spring.openfeature.optimizely` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `datafile` | Path to a local Optimizely datafile (classpath or filesystem). | — |
| `sdk-key` | Optimizely SDK key for remote config retrieval. | — |
| `datafile-auth-token` | Auth token for fetching the datafile from Optimizely's CDN. | — |
| `config-url` | Custom URL to fetch the Optimizely datafile from. | — |
| `poll-interval` | Polling interval in milliseconds for remote config updates. | — |

## Example Configuration

### With a local datafile

```yaml
spring:
  openfeature:
    optimizely:
      datafile: classpath:optimizely-datafile.json
```

### Remote config (SDK key)

```yaml
spring:
  openfeature:
    optimizely:
      sdk-key: your-optimizely-sdk-key
      datafile-auth-token: your-auth-token
      poll-interval: 60000
```

### Custom datafile URL

```yaml
spring:
  openfeature:
    optimizely:
      config-url: https://my-cdn.example.com/optimizely-datafile.json
      poll-interval: 30000
```

## Usage

Inject the auto-configured `Client` and evaluate flags:

```java
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ImmutableContext;
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

    @GetMapping("/feature/user/{id}")
    public Boolean featureForUser(@PathVariable("id") final String id) {
        return this.client.getBooleanValue("my-flag", false,
                new ImmutableContext(Map.of("userId", id)));
    }
}
```

Or use the `@ToggleOnFlag` annotation:

```java
import org.iromu.openfeature.boot.aop.ToggleOnFlag;

@GetMapping("/feature/annotated/user/{id}")
@ToggleOnFlag(key = "my-flag", attributes = "{'userId': #id}", orElse = "disabled")
public String enabled(@PathVariable("id") final String id) {
    return "Feature is ON for user " + id;
}

public String disabled(final String id) {
    return "Feature is OFF for user " + id;
}
```

## Customization

Implement `OptimizelyCustomizer` to tweak the provider configuration:

```java
import org.iromu.openfeature.boot.optimizely.OptimizelyCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class OptimizelyConfig {

    @Bean
    public OptimizelyCustomizer optimizelyCustomizer() {
        return (builder) -> builder.setEventDispatcher(new com.optimizely.ab.event.NoopEventDispatcher());
    }
}
```
