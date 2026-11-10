# spring-boot-openfeature-autoconfigure

Generic auto-configuration for [OpenFeature](https://openfeature.dev/). It is normally consumed
indirectly through the [base starter](../spring-boot-starter-openfeature) or a
[provider starter](../README.md), but can be added directly if you only want the OpenFeature wiring.

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-openfeature-autoconfigure</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-openfeature-autoconfigure'
```

## What it configures

The auto-configurations are registered via
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

| Auto-configuration | Purpose |
|---|---|
| `ClientAutoConfiguration` | Registers the `dev.openfeature.sdk.Client` bean. Activated only when a `FeatureProvider` bean is present, and it backs off if a `multiProvider` bean exists. |
| `OpenFeatureAPIAutoConfiguration` | Exposes the OpenFeature SDK's global `OpenFeatureAPI` and installs event logging. |
| `SecurityAutoConfiguration` | When Spring Security is on the classpath, adds a hook that injects `userId`/`authorities` into every flag evaluation. |

## Extension points

Define beans of the following types to customize the wiring:

- `ClientCustomizer` — tweak the auto-configured `Client` (e.g. set the domain, add hooks).
- `OpenFeatureAPICustomizer` — tweak the global `OpenFeatureAPI` before the `Client` is built.

```java
import dev.openfeature.sdk.Client;
import org.iromu.openfeature.boot.autoconfigure.ClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenFeatureConfig {

    @Bean
    public ClientCustomizer clientCustomizer() {
        return (client) -> client.addHandler(/* ... */);
    }
}
```

## Relationship to provider starters

A provider starter (e.g. `spring-boot-starter-openfeature-unleash`) registers a `FeatureProvider`
bean. `ClientAutoConfiguration` is then activated by that bean and builds the `Client`. If you register
your own `FeatureProvider`, the same wiring applies.
