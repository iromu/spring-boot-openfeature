# spring-boot-starter-openfeature-configdirector

Spring Boot starter that integrates the
[ConfigDirector OpenFeature provider](https://docs.configdirector.com/sdks/openfeature/java)
(`com.configdirector:configdirector-openfeature-server-provider`).

ConfigDirector is a remote configuration and feature-flag service: the provider
connects to the ConfigDirector endpoint using a server SDK key and keeps the
flags up to date in the background.

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-configdirector</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-configdirector'
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

All properties are under the `spring.openfeature.configdirector` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `api-key` | Server SDK key for the ConfigDirector environment. Required when the starter is enabled. | — |
| `url` | Endpoint the provider connects to. | ConfigDirector default endpoint |

Any other option (connection mode, polling interval, timeouts, metadata,
telemetry) is configured through a `ConfigDirectorCustomizer`.

## Example Configuration

```yaml
spring:
  openfeature:
    configdirector:
      api-key: ${CONFIGDIRECTOR_API_KEY}
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

Implement `ConfigDirectorCustomizer` to tweak the `ClientOptions` the provider
is built with:

```java
import com.configdirector.ClientOptions;
import org.iromu.openfeature.boot.configdirector.ConfigDirectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigDirectorConfig {

    @Bean
    public ConfigDirectorCustomizer configDirectorCustomizer() {
        return (ClientOptions options) -> options.metadata("my-app", "1.0.0");
    }
}
```
