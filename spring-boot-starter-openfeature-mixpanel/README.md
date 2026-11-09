# spring-boot-starter-openfeature-mixpanel

Spring Boot starter that integrates the [Mixpanel OpenFeature provider](https://github.com/mixpanel/mixpanel-java)
(`com.mixpanel:mixpanel-java-openfeature`).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-mixpanel</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-mixpanel'
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

All properties are under the `spring.openfeature.mixpanel` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `project-token` | Mixpanel project token for the target project. Required when the starter is enabled. | — |
| `api-host` | Mixpanel API host for your region (e.g. `api.mixpanel.com`, `api-eu.mixpanel.com`). | SDK default (`api.mixpanel.com`) |
| `request-timeout-seconds` | HTTP request timeout in seconds for flag evaluations. | SDK default (`10`) |

Without a `project-token` startup fails with a clear message naming the missing property —
the starter validates eagerly, which is intentional. Any other `RemoteFlagsConfig` option
(e.g. the exposure executor) is configured through a `MixpanelCustomizer`, which is the
Mixpanel SDK's own extension point.

## Example Configuration

```yaml
spring:
  openfeature:
    mixpanel:
      project-token: your-mixpanel-project-token
      api-host: api-eu.mixpanel.com
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

Implement `MixpanelCustomizer` to tweak the Mixpanel feature flags configuration:

```java
import org.iromu.openfeature.boot.mixpanel.MixpanelCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class MixpanelConfig {

    @Bean
    public MixpanelCustomizer mixpanelCustomizer() {
        // Wait longer for flag evaluation HTTP requests to complete:
        return (builder) -> builder.requestTimeoutSeconds(30);
    }
}
```
