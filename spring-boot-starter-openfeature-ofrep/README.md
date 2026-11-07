# spring-boot-starter-openfeature-ofrep

Spring Boot starter that integrates the [OpenFeature Remote Evaluation Protocol (OFREP) provider](https://github.com/open-feature/java-sdk-contrib/tree/main/providers/ofrep).
Flags are evaluated by an external OFREP-compatible server — any backend that speaks the OFREP protocol.

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-ofrep</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-ofrep'
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

All properties are under the `spring.openfeature.ofrep` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `base-url` | (mandatory) Base URL of the OFREP endpoint. Must be a valid `http` or `https` URL. | — |
| `connect-timeout` | Timeout for establishing the connection to the OFREP endpoint. | — |
| `request-timeout` | Timeout for a single evaluation request. | — |
| `headers` | Custom headers sent with every OFREP request (map of header name to list of values). | — |

## Example Configuration

```yaml
spring:
  openfeature:
    ofrep:
      base-url: https://ofrep.example.com/v1/evaluate
      connect-timeout: 5s
      request-timeout: 3s
      headers:
        Authorization:
          - Bearer your-api-token
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

Implement `OfrepCustomizer` to tweak the provider configuration:

```java
import org.iromu.openfeature.boot.ofrep.OfrepCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class OfrepConfig {

    @Bean
    public OfrepCustomizer ofrepCustomizer() {
        return (options) -> options.setConnectTimeout(Duration.ofSeconds(10));
    }
}
```
