# spring-boot-starter-openfeature-featureflip

Spring Boot starter that integrates the [Featureflip](https://featureflip.io/) feature-flag
provider via the [Featureflip OpenFeature provider](https://featureflip.io/docs/integrations/openfeature/)
(`io.featureflip:featureflip-openfeature`).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-featureflip</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-featureflip'
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

- `api-key` must be set when the starter is enabled; the auto-configuration fails fast
  otherwise. The target environment is encoded in the per-environment SDK key itself.

## Configuration Properties

All properties are under the `spring.openfeature.featureflip` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `api-key` | (mandatory) Featureflip SDK key for the target environment. | — |
| `base-url` | Optional Featureflip endpoint override (self-hosted deployments). | — |

Data-source tuning (streaming, polling, timeouts) is configured through a
`FeatureflipCustomizer`, or by supplying your own `FeatureProvider` bean, which makes the
starter back off entirely.

## Example Configuration

```yaml
spring:
  openfeature:
    featureflip:
      api-key: ${FEATUREFLIP_API_KEY}
      base-url: https://your.featureflip.endpoint
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

Implement `FeatureflipCustomizer` to adjust the assembled `FeatureflipProviderConfig`
before the provider is created:

```java
import org.iromu.openfeature.boot.featureflip.FeatureflipCustomizer;
import org.iromu.openfeature.boot.featureflip.FeatureflipProviderConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeatureflipConfig {

    @Bean
    public FeatureflipCustomizer featureflipCustomizer() {
        return (FeatureflipProviderConfig config) -> config.setBaseUrl("https://staging.featureflip.endpoint");
    }
}
```
