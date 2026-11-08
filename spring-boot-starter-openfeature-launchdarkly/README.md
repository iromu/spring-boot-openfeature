# spring-boot-starter-openfeature-launchdarkly

Spring Boot starter that integrates the [LaunchDarkly OpenFeature provider](https://github.com/launchdarkly/openfeature-java-server)
(`com.launchdarkly:launchdarkly-openfeature-serverprovider`).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-launchdarkly</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-launchdarkly'
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

All properties are under the `spring.openfeature.launchdarkly` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `service-key` | LaunchDarkly server-side SDK key for the target environment. Required when the starter is enabled — the environment is encoded in the key itself. | — |
| `offline` | Run the LaunchDarkly client in offline mode (no connection to LaunchDarkly is attempted). | `false` |

The data source (streaming, polling, or a local flag file) and any other
`LDConfig` option are configured through a `LaunchDarklyCustomizer`, which is the
LaunchDarkly SDK's own extension point.

## Example Configuration

```yaml
spring:
  openfeature:
    launchdarkly:
      service-key: sk-your-project-1-your-key
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

Implement `LaunchDarklyCustomizer` to tweak the LaunchDarkly client configuration:

```java
import com.launchdarkly.sdk.server.Components;
import org.iromu.openfeature.boot.launchdarkly.LaunchDarklyCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class LaunchDarklyConfig {

    @Bean
    public LaunchDarklyCustomizer launchDarklyCustomizer() {
        // Use polling instead of the default streaming data source:
        return (builder) -> builder.dataSource(Components.pollingDataSource().pollInterval(Duration.ofSeconds(30)));
    }
}
```
