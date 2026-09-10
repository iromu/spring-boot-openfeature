---
name: Disabling a single spring-javaformat Checkstyle rule
description: SpringNullabilityCheck is new in spring-javaformat 0.0.48 and has no config surface; disable one rule via maven-checkstyle-plugin's violationIgnore parameter and prove it is surgical with a canary violation.
type: feedback
---

When a newly-added spring-javaformat Checkstyle rule fails the build, disable **that rule only** via the
`maven-checkstyle-plugin` `<violationIgnore>` parameter. Do not revert the formatter bump, and do not hunt
for a suppressions file.

```xml
<configuration>
  <configLocation>io/spring/javaformat/checkstyle/checkstyle.xml</configLocation>
  <includeTestSourceDirectory>false</includeTestSourceDirectory>
  <violationIgnore>SpringNullability</violationIgnore>
</configuration>
```

**Why:** the rule itself is unreachable by configuration. `SpringNullabilityCheck` arrived in **0.0.48**
(release notes: "Add Spring Nullability Checkstyle Rule", #465) - `javap` over the two jars counts 0
`SpringNullabilityCheck` classes at 0.0.47 and 2 at 0.0.48. It has **zero public setters**, so Checkstyle's
`ConfigurationUtil` (which injects public fields only) can never configure it; its banned-import set is a
`private final Set unwantedNullabilityImports`. The `nullability.bannedImport` /
`nullability.annotationLocation` strings in `check/messages.properties` are **message templates, not
knobs**. `SpringChecks` exposes only `headerType` / `headerFile` / `headerCopyrightPattern` /
`projectRootPackage` / `avoidStaticImportExcludes`, so there is no suppression property either, and the
jar's `spring-checkstyle-suppressions.xml` is **dead weight** - `SpringChecks` builds its two filters via
`new SuppressFilterElement(...)` and never loads that file.

**Ruled out - do not re-test these:**

- `<suppressionsFiles>` is not a parameter of `maven-checkstyle-plugin:3.6.0:check` - the build warns
  `Parameter 'suppressionsFiles' is unknown`. Read the real parameter list from
  `META-INF/maven/plugin.xml` inside the plugin jar; it lists `violationIgnore` and `suppressionsLocation`.
- Pointing `configLocation` at the repo's root `checkstyle.xml` is **not** a no-op. That file is a bare
  `Checker -> SpringChecks` copy, and `SpringChecks` loads `spring-checkstyle.xml` internally via
  `SpringConfigurationLoader`, so both routes work - but the local one is unverified drift. Keep the
  bundled classpath resource.
- `-Dcheckstyle.version=...` is silently ignored: the engine is hard-pinned in the plugin's own
  `<dependencies>` block (`com.puppycrawl.tools:checkstyle:10.21.4`, while both ruleset poms declare
  `9.3`). Only editing that pin changes the engine, and aligning it to 9.3 reproduces the violation
  identically - the pin drift is **not** the cause.
- The JSpecify migration is genuinely blocked, so suppression is the right call rather than a shortcut:
  `SpringImportOrderCheck` rejects all 15 admissible positions for
  `import org.jspecify.annotations.Nullable;` (`import.separation` /
  `import.groups.separated.internally`).

**How to apply:** After wiring `violationIgnore`, prove the suppression is surgical rather than a silent
blanket disable - inject a deliberate violation into the same file (e.g. `if (b == true)` to trip
`SimplifyBooleanExpression`), confirm it is still reported, then remove it. Then re-verify with the
**full** reactor: a single module failure SKIPS every module after it, so a run that excludes the failing
module is not evidence of a green build.
