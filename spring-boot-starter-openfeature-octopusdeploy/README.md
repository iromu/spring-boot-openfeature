# spring-boot-starter-openfeature-octopusdeploy

Spring Boot starter that integrates the [Octopus Deploy OpenFeature provider](https://github.com/OctopusDeploy/openfeature-provider-java)
(`com.octopus.openfeature:octopus-openfeature-provider`).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-octopusdeploy</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-octopusdeploy'
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

All properties are under the `spring.openfeature.octopusdeploy` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `client-identifier` | Octopus Deploy client identifier (API key) for the target environment. Required when the starter is enabled. | — |
| `product-name` | Product name identifying the calling application. Required when the starter is enabled. | — |
| `product-version` | Product version identifying the calling application build. Optional. | — |
| `cache-duration` | How often the provider re-checks for flag changes. | `1m` |

## Example Configuration

```yaml
spring:
  openfeature:
    octopusdeploy:
      client-identifier: your-octopus-client-identifier
      product-name: my-application
      product-version: 1.0.0
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

Implement `OctopusDeployCustomizer` to tweak the Octopus Deploy provider configuration:

```java
import org.iromu.openfeature.boot.octopusdeploy.OctopusDeployCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class OctopusDeployConfig {

    @Bean
    public OctopusDeployCustomizer octopusDeployCustomizer() {
        // Re-check for flag changes every 5 minutes:
        return (config) -> config.setCacheDuration(Duration.ofMinutes(5));
    }
}
```
