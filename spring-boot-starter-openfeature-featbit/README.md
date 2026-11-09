# spring-boot-starter-openfeature-featbit

Spring Boot starter that integrates the [FeatBit OpenFeature provider](https://github.com/featbit/featbit-openfeature-provider-java-server)
(`co.featbit:featbit-openfeature-provider-java-server`).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-featbit</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-featbit'
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

All properties are under the `spring.openfeature.featbit` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `token` | FeatBit app token (environment secret) for the target environment. Required when the starter is enabled. | — |
| `streaming-url` | FeatBit streaming (WebSocket) endpoint for the environment. Required when the starter is enabled. | — |
| `event-url` | FeatBit event (analytics) endpoint for the environment. Required when the starter is enabled. | — |
| `offline` | Run the FeatBit client in offline mode (no connection to FeatBit is attempted). | `false` |
| `disable-events` | Disable FeatBit event delivery (analytics tracking) entirely. | `false` |

Each FeatBit environment has its own `streaming-url` and `event-url`, obtained from the FeatBit
dashboard. Any other `FBConfig` option is configured through a `FeatBitCustomizer`, which is the
FeatBit SDK's own extension point.

## Example Configuration

```yaml
spring:
  openfeature:
    featbit:
      token: your-featbit-app-token
      streaming-url: wss://your-environment.streaming.featbit.co
      event-url: https://your-environment.api.featbit.co
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

Implement `FeatBitCustomizer` to tweak the FeatBit client configuration:

```java
import org.iromu.openfeature.boot.featbit.FeatBitCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class FeatBitConfig {

    @Bean
    public FeatBitCustomizer featBitCustomizer() {
        // Wait longer for the initial flag fetch before the client reports ready:
        return (builder) -> builder.startWaitTime(Duration.ofSeconds(30));
    }
}
```
