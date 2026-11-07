# spring-boot-starter-openfeature-flagsmith

Spring Boot starter that integrates the [Flagsmith](https://flagsmith.com/) feature-flag provider via the
[OpenFeature Flagsmith provider](https://github.com/open-feature/java-sdk-contrib/tree/main/providers/flagsmith).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-flagsmith</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-flagsmith'
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

All properties are under the `spring.openfeature.flagsmith` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `api-key` | Flagsmith API token. Use the *Server Side SDK Token* for remote evaluation or the *Environment API key* for local evaluation. | — |

## Example Configuration

```yaml
spring:
  openfeature:
    flagsmith:
      api-key: your-server-side-sdk-token
```

The environment key is read from the `FLAGSMITH_ENVIRONMENT_KEY` environment variable or the
`flagsmith.environmentKey` system property.

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

Implement `FlagsmithCustomizer` to tweak the provider configuration:

```java
import org.iromu.openfeature.boot.flagsmith.FlagsmithCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class FlagsmithConfig {

    @Bean
    public FlagsmithCustomizer flagsmithCustomizer() {
        return (client) -> client.setEnvironmentKey("your-environment-key");
    }
}
```
