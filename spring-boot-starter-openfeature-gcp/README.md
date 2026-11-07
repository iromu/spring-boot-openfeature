# spring-boot-starter-openfeature-gcp

Spring Boot starter that integrates the [Google Cloud feature-flag provider](https://github.com/open-feature/java-sdk-contrib/tree/main/providers/gcp)
via either [Secret Manager](https://cloud.google.com/secret-manager) or [Parameter Manager](https://cloud.google.com/parameter-manager).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-gcp</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-gcp'
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

All properties are under the `spring.openfeature.gcp` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `type` | Backend type: `secretmanager` or `parametermanager`. | `secretmanager` |
| `project-id` | GCP project ID. | — |
| `service-account-json` | Path to a service-account JSON file. When unset, falls back to Application Default Credentials. | — |
| `version` | Secret/parameter version to read (`latest` resolves the most recent). | — |
| `location-id` | Location of the secret/parameter (e.g. `global`). | — |
| `cache-expiry` | How long to cache a fetched value before re-fetching. | — |
| `cache-max-size` | Maximum number of cached entries. | — |
| `name-prefix` | Prefix prepended to the flag key when looking up the secret/parameter. | — |

## Example Configuration

### Secret Manager

```yaml
spring:
  openfeature:
    gcp:
      type: secretmanager
      project-id: my-gcp-project
      location-id: global
      version: latest
      cache-expiry: 5m
      cache-max-size: 100
      name-prefix: flags/
```

### Parameter Manager

```yaml
spring:
  openfeature:
    gcp:
      type: parametermanager
      project-id: my-gcp-project
      location-id: global
      cache-expiry: 10m
```

### With a service-account JSON file

```yaml
spring:
  openfeature:
    gcp:
      project-id: my-gcp-project
      service-account-json: classpath:service-account.json
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

Implement `GcpCustomizer` to tweak the provider configuration:

```java
import org.iromu.openfeature.boot.gcp.GcpCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class GcpConfig {

    @Bean
    public GcpCustomizer gcpCustomizer() {
        return (options) -> options.setCacheExpiry(Duration.ofMinutes(15));
    }
}
```
