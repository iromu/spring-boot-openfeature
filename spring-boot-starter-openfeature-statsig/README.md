# spring-boot-starter-openfeature-statsig

Spring Boot starter that integrates the [Statsig](https://statsig.com/) feature-flag provider via the
[OpenFeature Statsig provider](https://github.com/open-feature/java-sdk-contrib/tree/main/providers/statsig).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-statsig</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-statsig'
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

All properties are under the `spring.openfeature.statsig` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `sdk-key` | (mandatory) Statsig SDK key (server-side key). | — |
| `local-mode` | Enable local mode (server-side SDK with local evaluation). | `false` |

## Example Configuration

### Remote mode

```yaml
spring:
  openfeature:
    statsig:
      sdk-key: your-server-side-sdk-key
```

### Local mode

```yaml
spring:
  openfeature:
    statsig:
      sdk-key: your-server-side-sdk-key
      local-mode: true
```

## Usage

Inject the auto-configured `Client` and evaluate flags:

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

Or use the `@ToggleOnFlag` annotation:

```java
import org.iromu.openfeature.boot.aop.ToggleOnFlag;

@GetMapping("/feature/annotated")
@ToggleOnFlag(key = "my-flag", orElse = "disabled")
public String enabled() {
    return "Feature is ON";
}

public String disabled() {
    return "Feature is OFF";
}
```

## Customization

Implement `StatsigCustomizer` to tweak the provider configuration:

```java
import org.iromu.openfeature.boot.statsig.StatsigCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class StatsigConfig {

    @Bean
    public StatsigCustomizer statsigCustomizer() {
        return (config) -> config.setServerSide(true);
    }
}
```
