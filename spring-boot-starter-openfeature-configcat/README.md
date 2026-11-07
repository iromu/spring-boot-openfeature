# spring-boot-starter-openfeature-configcat

Spring Boot starter that integrates the [ConfigCat](https://configcat.com/) feature-flag provider via the
[OpenFeature ConfigCat provider](https://github.com/open-feature/java-sdk-contrib/tree/main/providers/configcat).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-configcat</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-configcat'
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

All properties are under the `spring.openfeature.config-cat` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `sdk-key` | ConfigCat SDK key (server-side key). | — |
| `filename` | Path to a local override file (classpath or filesystem). | — |

## Example Configuration

```yaml
spring:
  openfeature:
    config-cat:
      sdk-key: your-server-side-sdk-key
```

To use a local override file (e.g. for testing without the ConfigCat SDK):

```yaml
spring:
  openfeature:
    config-cat:
      filename: classpath:flags.json
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

Implement `ConfigCatCustomizer` to tweak the provider configuration:

```java
import dev.openfeature.contrib.providers.configcat.ConfigCatProviderConfig;
import org.iromu.openfeature.boot.configcat.ConfigCatCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class ConfigCatConfig {

    @Bean
    public ConfigCatCustomizer configCatCustomizer() {
        return (options) -> options.pollingMode(
            ConfigCatProviderConfig.PollingMode.AutoPoll, Duration.ofSeconds(120));
    }
}
```
