# spring-boot-starter-openfeature-kameleoon

Spring Boot starter that integrates the [Kameleoon OpenFeature provider](https://github.com/Kameleoon/openfeature-java)
(`com.kameleoon:kameleoon-openfeature-java`).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-kameleoon</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-kameleoon'
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

All properties are under the `spring.openfeature.kameleoon` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `site-code` | Kameleoon site code for the target site. Required when the starter is enabled. | — |
| `client-id` | Kameleoon client ID for server-to-server access. Required when the starter is enabled. | — |
| `client-secret` | Kameleoon client secret for server-to-server access. Required when the starter is enabled. | — |
| `environment` | Kameleoon environment name. | — |
| `top-level-domain` | Top-level domain for the Kameleoon endpoints, recommended when using subdomains. | — |
| `refresh-interval` | Configuration refresh interval in minutes. | Kameleoon default |
| `session-duration` | Visitor session duration in minutes. | Kameleoon default |
| `default-timeout` | Default HTTP timeout in milliseconds. | Kameleoon default |
| `tracking-interval` | Conversion tracking interval in milliseconds. | Kameleoon default |

Any other `KameleoonClientConfig` option (for example, a proxy host) is configured through a
`KameleoonCustomizer`, which is the Kameleoon client's own extension point.

## Example Configuration

```yaml
spring:
  openfeature:
    kameleoon:
      site-code: your-kameleoon-site-code
      client-id: your-kameleoon-client-id
      client-secret: your-kameleoon-client-secret
      environment: production
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

Implement `KameleoonCustomizer` to tweak the Kameleoon client configuration:

```java
import org.iromu.openfeature.boot.kameleoon.KameleoonCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class KameleoonConfig {

    @Bean
    public KameleoonCustomizer kameleoonCustomizer() {
        // Route the Kameleoon requests through a proxy:
        return (builder) -> builder.proxyHost(new HttpHost("proxy.example.org", 8080));
    }
}
```
