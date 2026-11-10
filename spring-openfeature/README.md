# spring-openfeature

Core library for the Spring Boot OpenFeature starters. It contains the provider-agnostic features that
are not tied to a specific flag backend: the declarative `@ToggleOnFlag` annotation (and its AOP aspect),
the Actuator health indicator, and the global `spring.openfeature` properties.

It is pulled in transitively by the
[base starter](../spring-boot-starter-openfeature) and every provider starter, so you usually do not
declare it directly.

## The `@ToggleOnFlag` annotation

Conditionally run a method (or every method in a class) based on a feature flag. If the flag evaluates
to `true`, the annotated method runs; if it evaluates to `false`, the fallback method named by `orElse`
is invoked instead.

| Attribute | Description | Default |
|---|---|---|
| `key` | The feature-flag key to evaluate. | — |
| `attributes` | A SpEL map used to build the evaluation context, e.g. `"{'userId': #id}"`. | `"{}"` |
| `orElse` | Name of the fallback method to run when the flag is `false`. | `""` |

```java
import org.iromu.openfeature.boot.aop.ToggleOnFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@GetMapping("/feature/annotated/user/{id}")
@ToggleOnFlag(key = "users-flag", attributes = "{'userId': #id}", orElse = "disabled")
public String enabled(@PathVariable("id") final String id) {
    return "Feature is ON for user " + id;
}

public String disabled(final String id) {
    return "Feature is OFF for user " + id;
}
```

The aspect is registered by `AspectAutoConfiguration` (Spring AOP / AspectJ must be on the classpath,
which the starters provide).

## Health indicator

`OpenFeatureHealthIndicator` (registered by `HealthAutoConfiguration`) reports the OpenFeature
provider's health through Spring Boot Actuator's `/actuator/health` endpoint.

## Global properties

All properties live under the `spring.openfeature` prefix.

| Property | Description | Default |
|---|---|---|
| `spring.openfeature.enabled` | Enable/disable the global OpenFeature auto-configuration. | `true` |

Per-provider properties are defined by each provider starter under `spring.openfeature.<provider>` —
see that starter's README.
