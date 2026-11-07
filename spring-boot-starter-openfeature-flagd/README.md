# spring-boot-starter-openfeature-flagd

Spring Boot starter that integrates the [flagd](https://flagd.dev/) feature-flag provider via the
[OpenFeature flagd provider](https://github.com/open-feature/java-sdk-contrib/tree/main/providers/flagd).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-flagd</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-flagd'
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

All properties are under the `spring.openfeature.flagd` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `resolver-type` | Flag resolution mode: `RPC` (connects to a flagd server) or `IN_PROCESS` (local sync stream). | `RPC` |
| `deadline` | Connection deadline in milliseconds. For RPC: the deadline to connect to flagd. For in-process: the deadline for sync stream termination. | `500` |
| `offline-flag-source-path` | File source of flags for offline mode. Setting this enables offline mode of the in-process provider. | — |

## Example Configuration

### RPC mode (connecting to a flagd server)

```yaml
spring:
  openfeature:
    flagd:
      resolver-type: RPC
      deadline: 5000
```

The flagd server address is read from the `FLAGD_HOST` and `FLAGD_PORT` environment variables (or the
`dev.openfeature.flagd.host` / `dev.openfeature.flagd.port` system properties).

### In-process mode

```yaml
spring:
  openfeature:
    flagd:
      resolver-type: IN_PROCESS
      deadline: 5000
```

### Offline mode

```yaml
spring:
  openfeature:
    flagd:
      resolver-type: IN_PROCESS
      offline-flag-source-path: classpath:flags.yaml
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

Implement `FlagdCustomizer` to tweak the provider configuration:

```java
import dev.openfeature.contrib.providers.flagd.Config;
import org.iromu.openfeature.boot.flagd.FlagdCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class FlagdConfig {

    @Bean
    public FlagdCustomizer flagdCustomizer() {
        return (config) -> config.setHost("flagd-server")
            .setPort(8013);
    }
}
```
