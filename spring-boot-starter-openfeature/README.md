# spring-boot-starter-openfeature

Base Spring Boot starter for [OpenFeature](https://openfeature.dev/). It wires up the
provider-agnostic pieces — the auto-configured `Client` bean, the OpenFeature API, the
`@ToggleOnFlag` annotation, a health indicator, and (when Spring Security is present) the
security evaluation-context hook. Pair it with a [provider starter](#pair-it-with-a-provider-starter)
to connect a feature-flag backend.

## Adding the Dependency

**Maven**

```xml
<dependency>
    <groupId>org.iromu.openfeature</groupId>
    <artifactId>spring-boot-starter-openfeature</artifactId>
</dependency>
```

**Gradle**

```groovy
implementation 'org.iromu.openfeature:spring-boot-starter-openfeature'
```

This starter pulls in [`spring-boot-openfeature-autoconfigure`](../spring-boot-openfeature-autoconfigure)
and imports the
[`spring-boot-openfeature-dependencies`](../spring-boot-openfeature-dependencies) BOM, so the
OpenFeature SDK and provider versions are managed for you.

## Pair it with a provider starter

On its own this starter only provides the generic wiring. You also need a feature-flag backend:
add the starter for your provider (for example
[`spring-boot-starter-openfeature-unleash`](../spring-boot-starter-openfeature-unleash)) to register
a `FeatureProvider` bean, which in turn activates the `Client`.

See the [root README](../README.md) for the full list of provider starters.

## What you get

- A ready-made `dev.openfeature.sdk.Client` bean, created once a `FeatureProvider` is present.
- The OpenFeature API with event logging (`OpenFeatureAPIAutoConfiguration`).
- Automatic `userId`/`authorities` evaluation context when Spring Security is on the classpath.
- A health indicator and the `@ToggleOnFlag` annotation (see
  [`spring-openfeature`](../spring-openfeature)).
