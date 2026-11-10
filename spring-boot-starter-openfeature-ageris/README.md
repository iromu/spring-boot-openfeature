# spring-boot-starter-openfeature-ageris

Spring Boot starter that integrates the [Ageris OpenFeature provider](https://github.com/lichtsinn/ageris#using-ageris-through-openfeature)
(`io.github.lichtsinn:ageris-openfeature`).

Ageris is a file-based platform: feature flags and JSON configs are read from
configuration files in a local directory and refreshed on a schedule. Boolean
flag lookups are answered from the feature-flag files; text, number and object
lookups from the JSON config files.

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-ageris</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-ageris'
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

All properties are under the `spring.openfeature.ageris` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `application` | Name of the application, as the configuration file fetcher and the Ageris managers use it. Required when the starter is enabled. | — |
| `path` | Base directory of the Ageris configuration files; the fetcher reads them from `<path>/<application>/`. Required when the starter is enabled. | — |
| `feature-flag-files` | Names of the feature-flag configuration files (e.g. `featureflags.json`). At least one entry is required when the starter is enabled. | — |
| `json-config-files` | Names of the JSON configuration files (e.g. `configs.json`). Optional — text, number and object lookups are answered from these files when present. | empty |
| `targeting-key-type` | Context type that the targeting key of an OpenFeature caller is put under. | `user` |
| `refresh-interval` | Interval between two refreshes of the configuration files. | `10s` |
| `first-load-timeout` | How long provider initialization waits for the first attempt to load the configuration files. | `30s` |

Any other `Ageris` option (policy bindings, metrics, skip behaviour) is
configured through an `AgerisCustomizer`, which runs before the managers are
built.

## Example Configuration

```yaml
spring:
  openfeature:
    ageris:
      application: my-app
      path: /etc/my-app
      feature-flag-files: featureflags.json
      json-config-files: configs.json
```

The fetcher reads the files from `<path>/<application>/`, so this example
expects `/etc/my-app/my-app/featureflags.json` and
`/etc/my-app/my-app/configs.json`.

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

Implement `AgerisCustomizer` to tweak the Ageris instance the provider is
built from:

```java
import io.github.lichtsinn.ageris.core.Ageris;
import org.iromu.openfeature.boot.ageris.AgerisCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgerisConfig {

    @Bean
    public AgerisCustomizer agerisCustomizer(PricingService pricingService) {
        // Bind a policy value the configuration files cannot express:
        return (Ageris ageris) -> ageris.bind(PricingService.class, pricingService);
    }
}
```
