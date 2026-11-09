# spring-boot-starter-openfeature-confidence

Spring Boot starter that integrates the [Confidence OpenFeature provider](https://github.com/spotify/confidence-sdk-java)
(`com.spotify.confidence:openfeature-provider`).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-confidence</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-confidence'
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

All properties are under the `spring.openfeature.confidence` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `api-token` | Confidence API token (client secret) used to authenticate against the Confidence service. Required when the starter is enabled. | — |
| `host` | Optional Confidence flag-resolver endpoint override. When unset the provider's default endpoint is used. | — |
| `port` | Optional Confidence flag-resolver endpoint port, used together with `host`. | `443` |

Any other `Confidence.Builder` option (resolver/event-sender deadlines, telemetry) is
configured through a `ConfidenceCustomizer`, which is the Confidence SDK's own extension
point.

## Example Configuration

```yaml
spring:
  openfeature:
    confidence:
      api-token: your-confidence-api-token
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

Implement `ConfidenceCustomizer` to tweak the Confidence client configuration:

```java
import org.iromu.openfeature.boot.confidence.ConfidenceCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class ConfidenceConfig {

    @Bean
    public ConfidenceCustomizer confidenceCustomizer() {
        // Wait longer for flag resolution before giving up:
        return (builder) -> builder.resolveDeadlineMs(30000);
    }
}
```
