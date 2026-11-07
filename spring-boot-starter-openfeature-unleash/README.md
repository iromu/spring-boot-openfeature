# spring-boot-starter-openfeature-unleash

Spring Boot starter that integrates the [Unleash](https://www.getunleash.io/) feature-flag provider via the
[OpenFeature Unleash provider](https://github.com/open-feature/java-sdk-contrib/tree/main/providers/unleash).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-unleash</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-unleash'
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

All properties are under the `spring.openfeature.unleash` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `unleash-api` | (mandatory) URL of the Unleash API endpoint. | — |
| `unleash-token` | Unleash API token (sent as the `Authorization` header). | — |
| `unleash-urls` | Alternative to `unleash-api` + `unleash-token`: a `UnleashURLs` object with host, app name, and token. | — |
| `app-name` | Application name reported to Unleash. | — |
| `environment` | Environment name reported to Unleash. | `default` |
| `custom-http-headers` | Additional HTTP headers sent with every request (map). | — |
| `instance-id` | Unique instance identifier. | — |
| `backup-file` | Path to a backup file (JSON) used when Unleash is unreachable. | — |
| `project-name` | Unleash project name. | — |
| `name-prefix` | Prefix prepended to flag names. | — |
| `fetch-toggles-interval` | Interval (seconds) between toggle polls. | `10` |
| `fetch-toggles-connect-timeout` | Connect timeout for toggle fetch requests. | `10s` |
| `fetch-toggles-read-timeout` | Read timeout for toggle fetch requests. | `10s` |
| `disable-polling` | Disable periodic toggle polling. | `false` |
| `send-metrics-interval` | Interval (seconds) between metrics reports. | `60` |
| `send-metrics-connect-timeout` | Connect timeout for metrics requests. | `10s` |
| `send-metrics-read-timeout` | Read timeout for metrics requests. | `10s` |
| `disable-metrics` | Disable metrics reporting. | `false` |
| `synchronous-fetch-on-initialisation` | Synchronously fetch toggles on startup before the context is ready. | `false` |

## Example Configuration

### Basic

```yaml
spring:
  openfeature:
    unleash:
      app-name: ${spring.application.name}
      environment: production
      unleash-api: https://unleash.example.com/api/
      unleash-token: 'prod:my-api-token'
```

### With backup file and custom polling

```yaml
spring:
  openfeature:
    unleash:
      app-name: my-app
      environment: production
      unleash-api: https://unleash.example.com/api/
      unleash-token: 'prod:my-api-token'
      backup-file: classpath:backup.json
      fetch-toggles-interval: 30
      synchronous-fetch-on-initialisation: true
```

### With custom HTTP headers

```yaml
spring:
  openfeature:
    unleash:
      app-name: my-app
      unleash-api: https://unleash.example.com/api/
      unleash-token: 'prod:my-api-token'
      custom-http-headers:
        X-Internal-Service: my-service
```

## Usage

Inject the auto-configured `Client` and evaluate flags:

```java
import dev.openfeature.sdk.Client;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

@GetMapping("/feature/annotated/user/{id}")
@ToggleOnFlag(key = "users-flag", attributes = "{'userId': #id}", orElse = "disabled")
public String enabled(@PathVariable("id") final String id) {
    return "Feature is ON for user " + id;
}

public String disabled(final String id) {
    return "Feature is OFF for user " + id;
}
```

## Customization

Implement `UnleashCustomizer` to tweak the Unleash client configuration:

```java
import io.getunleash.util.UnleashConfig;
import org.iromu.openfeature.boot.unleash.UnleashCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UnleashConfig {

    @Bean
    public UnleashCustomizer unleashCustomizer() {
        return (builder) -> builder
            .appName("my-app")
            .instanceId("instance-1")
            .disableMetrics();
    }
}
```
