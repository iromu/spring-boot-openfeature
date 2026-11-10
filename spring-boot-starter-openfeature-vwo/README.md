# spring-boot-starter-openfeature-vwo

Spring Boot starter that integrates the [VWO OpenFeature provider](https://github.com/wingify/vwo-openfeature-provider-java)
(`com.vwo.sdk:vwo-openfeature-provider-java`).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-vwo</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-vwo'
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

All properties are under the `spring.openfeature.vwo` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `account-id` | VWO account ID for the target account. Required when the starter is enabled; must be numeric. | — |
| `api-key` | VWO API key (sdk key) used to initialize the VWO client. Required when the starter is enabled. | — |

Any other option of the options map the `VWOProvider` is built from is configured through a
`VwoCustomizer`.

## Example Configuration

```yaml
spring:
  openfeature:
    vwo:
      account-id: your-vwo-account-id
      api-key: your-vwo-api-key
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

Implement `VwoCustomizer` to tweak the options map the `VWOProvider` is built from:

```java
import org.iromu.openfeature.boot.vwo.VwoCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class VwoConfig {

    @Bean
    public VwoCustomizer vwoCustomizer() {
        return (options) -> options.put("custom-option", "value");
    }
}
```
