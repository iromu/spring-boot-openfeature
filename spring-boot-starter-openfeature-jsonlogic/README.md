# spring-boot-starter-openfeature-jsonlogic

Spring Boot starter that integrates the [OpenFeature JsonLogic provider](https://github.com/open-feature/java-sdk-contrib/tree/main/providers/jsonlogic).
Flags are evaluated locally using [JSONLogic](https://jsonlogic.com/) rules loaded from a JSON file — no external
service required.

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature-jsonlogic</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature-jsonlogic'
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

All properties are under the `spring.openfeature.json-logic` prefix.

| Property | Description | Default |
|---|---|---|
| `enabled` | Enable/disable the auto-configuration. | `true` |
| `filename` | Path to the JSON file containing flag definitions (classpath or filesystem). | — |

## Example Configuration

```yaml
spring:
  openfeature:
    json-logic:
      filename: classpath:flags.json
```

### Flag file format

The JSON file maps flag keys to JSONLogic rules:

```json
{
  "my-feature": {
    "and": [
      { "==": ["{{context.userId}}", "123"] },
      { "var": "enabled" }
    ]
  },
  "another-flag": true
}
```

## Usage

Inject the auto-configured `Client` and evaluate flags:

```java
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ImmutableContext;
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

    @GetMapping("/feature/user/{id}")
    public Boolean featureForUser(@PathVariable("id") final String id) {
        return this.client.getBooleanValue("my-feature", false,
                new ImmutableContext(Map.of("userId", id)));
    }
}
```

Or use the `@ToggleOnFlag` annotation:

```java
import org.iromu.openfeature.boot.aop.ToggleOnFlag;

@GetMapping("/feature/annotated/user/{id}")
@ToggleOnFlag(key = "my-feature", attributes = "{'userId': #id}", orElse = "disabled")
public String enabled(@PathVariable("id") final String id) {
    return "Feature is ON for user " + id;
}

public String disabled(final String id) {
    return "Feature is OFF for user " + id;
}
```
