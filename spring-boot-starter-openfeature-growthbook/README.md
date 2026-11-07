# spring-boot-starter-openfeature-growthbook

Spring Boot starter that integrates the [GrowthBook](https://growthbook.io/) feature-flag provider via the
[GrowthBook OpenFeature provider](https://github.com/growthbook/growthbook-openfeature-provider-java).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-growthbook</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-growthbook'
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

All properties are under the `spring.openfeature.growthbook` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `api-host` | (mandatory) GrowthBook API host URL. | `https://cdn.growthbook.io` |
| `client-key` | (mandatory) GrowthBook client key. | — |
| `decryption-key` | (optional) Decryption key to decrypt encrypted features. | — |
| `refresh-strategy` | Feature refresh strategy: `STALE_WHILE_REVALIDATE`, `SWR`, `REFRESH`. | `STALE_WHILE_REVALIDATE` |
| `cache-mode` | Cache mode: `AUTO`, `MEMORY_ONLY`, `DISK_ONLY`. | `AUTO` |
| `cache-directory` | Directory to store the local cache. | `java.io.tmpdir` |
| `is-qa-mode` | QA mode: disables random assignment, only uses explicitly forced variations. | `false` |
| `is-cache-disabled` | Whether the local cache is disabled. | `false` |
| `global-forced-variations-map` | Force specific experiments to always assign a specific variation (key: experiment key, value: variation index). | — |

## Example Configuration

```yaml
spring:
  openfeature:
    growthbook:
      api-host: https://cdn.growthbook.io
      client-key: your-client-key
      decryption-key: your-decryption-key
      cache-mode: AUTO
```

### QA mode with forced variations

```yaml
spring:
  openfeature:
    growthbook:
      api-host: https://cdn.growthbook.io
      client-key: your-client-key
      is-qa-mode: true
      global-forced-variations-map:
        experiment-key: 0
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

Implement `GrowthBookCustomizer` to tweak the provider configuration:

```java
import org.iromu.openfeature.boot.growthbook.GrowthBookCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class GrowthBookConfig {

    @Bean
    public GrowthBookCustomizer growthBookCustomizer() {
        return (builder) -> builder.setApiHost("https://custom.growthbook.io");
    }
}
```
