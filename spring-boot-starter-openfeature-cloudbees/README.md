# spring-boot-starter-openfeature-cloudbees

Spring Boot starter that integrates the [CloudBees OpenFeature provider](https://github.com/rollout/cloudbees-openfeature-provider-java)
(`com.cloudbees.openfeature:cloudbees-openfeature-provider`).

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-cloudbees</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-cloudbees'
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

All properties are under the `spring.openfeature.cloudbees` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `api-key` | CloudBees app key (also known as the environment ID). Required when the starter is enabled. | — |
| `endpoint` | Optional URL of the CloudBees (Rox) service endpoint. | — |

Any other option of the `RoxOptions` the `CloudbeesProvider` is built with is
configured through a `CloudbeesCustomizer`.

## Example Configuration

```yaml
spring:
  openfeature:
    cloudbees:
      api-key: your-cloudbees-app-key
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

Implement `CloudbeesCustomizer` to tweak the `RoxOptions` the `CloudbeesProvider`
is built with:

```java
import org.iromu.openfeature.boot.cloudbees.CloudbeesCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class CloudbeesConfig {

    @Bean
    public CloudbeesCustomizer cloudbeesCustomizer() {
        return (builder) -> builder.withFetchIntervalInSeconds(30L);
    }
}
```
