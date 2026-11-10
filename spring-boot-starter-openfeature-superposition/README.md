# spring-boot-starter-openfeature-superposition

Spring Boot starter that integrates the [Superposition OpenFeature provider](https://github.com/juspay/superposition)
(`io.juspay.superposition:openfeature-provider`).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-superposition</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-superposition'
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

All properties are under the `spring.openfeature.superposition` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `api-key` | Superposition API token for server-to-server access. Required when the starter is enabled. | — |
| `org-id` | Superposition organization ID. Required when the starter is enabled. | — |
| `workspace-id` | Superposition workspace ID. Required when the starter is enabled. | — |
| `endpoint` | Superposition API endpoint. | `https://api.superposition.dev` |
| `refresh-timeout-millis` | Configuration refresh timeout in milliseconds. | `10000` |
| `refresh-interval-millis` | Configuration refresh interval in milliseconds. | `5000` |

Any other `SuperpositionProviderOptions` option (for example, a fallback config or the
experimentation options) is configured through a `SuperpositionCustomizer`, which wraps the
provider's own options builder.

## Example Configuration

```yaml
spring:
  openfeature:
    superposition:
      api-key: your-superposition-api-token
      org-id: your-superposition-org-id
      workspace-id: your-superposition-workspace-id
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

Implement `SuperpositionCustomizer` to tweak the provider options:

```java
import io.juspay.superposition.openfeature.options.RefreshStrategy;
import org.iromu.openfeature.boot.superposition.SuperpositionCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class SuperpositionConfig {

    @Bean
    public SuperpositionCustomizer superpositionCustomizer() {
        // Refresh the configuration every 30 seconds with a 15-second timeout:
        return (builder) -> builder.refreshStrategy(RefreshStrategy.Polling.of(15000, 30000));
    }
}
```
