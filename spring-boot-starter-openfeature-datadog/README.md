# spring-boot-starter-openfeature-datadog

Spring Boot starter that integrates the [Datadog](https://www.datadoghq.com/) feature-flag provider via the
[Datadog OpenFeature provider](https://docs.datadoghq.com/feature_flags/server/java/)
(`com.datadoghq:dd-openfeature`, part of the dd-trace-java project).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-datadog</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-datadog'
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

## Requirements

- The [Datadog java agent](https://docs.datadoghq.com/tracing/tracing_components/library_installation/java/)
  (`dd-java-agent`, version 1.66.0 or later) must be loaded in the JVM, e.g.
  `-javaagent:/path/to/dd-java-agent.jar`. With the default agentless delivery mode the
  provider loads flag configuration directly from Datadog using the credentials below.
- All three of `api-key`, `app-key` and `site` must be set when the starter is enabled;
  the auto-configuration fails fast otherwise.

## Configuration Properties

All properties are under the `spring.openfeature.datadog` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `api-key` | (mandatory) Datadog API key. Published as the tracer's `DD_API_KEY` setting when not already set. | — |
| `app-key` | (mandatory) Datadog application key. | — |
| `site` | (mandatory) Datadog site (e.g. `datadoghq.com`, `datadoghq.eu`). Published as `DD_SITE` when not already set. | — |
| `environment` | Environment reported to Datadog (e.g. `production`). Published as `DD_ENV` when not already set. | — |
| `init-timeout` | Timeout for the provider to receive its initial configuration. | `30s` |
| `evaluation-telemetry-opt-out` | Expose the evaluation telemetry opt-out parameter on flag evaluations. | `false` |

Values already present as `DD_API_KEY` / `DD_SITE` / `DD_ENV` environment variables or
system properties always win; the starter never overrides them.

## Example Configuration

```yaml
spring:
  openfeature:
    datadog:
      api-key: ${DD_API_KEY}
      app-key: ${DD_APP_KEY}
      site: datadoghq.com
      environment: production
      init-timeout: 10s
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

Implement `DatadogCustomizer` to adjust the assembled `DatadogProviderConfig` before the
provider is created:

```java
import org.iromu.openfeature.boot.datadog.DatadogCustomizer;
import org.iromu.openfeature.boot.datadog.DatadogProviderConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatadogConfig {

    @Bean
    public DatadogCustomizer datadogCustomizer() {
        return (DatadogProviderConfig config) -> config.setEnvironment("staging");
    }
}
```
