# spring-boot-starter-openfeature-devcycle

Spring Boot starter that integrates the [DevCycle OpenFeature provider](https://docs.devcycle.com/deploy/start-here/developer-guides/integrations/open-feature)
(`com.devcycle:java-server-sdk`, which embeds `com.devcycle.sdk.server.openfeature.DevCycleProvider`).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-devcycle</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-devcycle'
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

All properties are under the `spring.openfeature.devcycle` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `server-side-key` | DevCycle server-side SDK key for the target project (must start with `dvc_server`). Required when the starter is enabled. | — |
| `base-url-override` | Base URL override for the DevCycle bucketing API (defaults to the production API). | — |

Any other client option (custom logger, REST options, eval hooks, edge DB) is
configured through a `DevCycleCustomizer`, which wraps the DevCycle SDK's own
`DevCycleCloudOptions.Builder`.

## Example Configuration

```yaml
spring:
  openfeature:
    devcycle:
      server-side-key: dvc_server_your_project_key
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

Implement `DevCycleCustomizer` to tweak the DevCycle client configuration:

```java
import com.devcycle.sdk.server.common.logging.IDevCycleLogger;
import org.iromu.openfeature.boot.devcycle.DevCycleCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class DevCycleConfig {

    @Bean
    public DevCycleCustomizer devCycleCustomizer() {
        // Install a custom logger for DevCycle SDK diagnostics:
        return (builder) -> builder.customLogger(new MyDevCycleLogger());
    }
}
```
