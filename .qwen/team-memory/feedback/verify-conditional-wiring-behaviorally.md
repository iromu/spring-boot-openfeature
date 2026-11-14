---
name: A silent no-op looks like success — verify conditional bean and AOP wiring behaviorally
description: An @ConditionalOnBean auto-configured aspect can register nothing and log nothing, so grepping logs proves nothing; prove wiring by toggling the thing it gates and observing the effect
type: feedback
category: common_pitfall
keywords:
  - "@ConditionalOnBean silently unmet"
  - "aspect never registered"
  - "auto-configuration ordering alphabetical"
  - "verify AOP wiring behaviorally"
  - "silent no-op looks like success"
usage_scenarios:
  - "A declarative annotation appears to do nothing"
  - "Confirming an auto-configured bean exists"
  - "Debugging an aspect that never fires"
---

When a feature is delivered by a conditionally-registered bean (an aspect, an interceptor, an
auto-configuration), **absence of log output is not evidence of anything** — a bean that never
registered produces no lines, and neither does one that registered but was never invoked. Grep for
the class name and get `0` matches, then conclude nothing.

Prove it **behaviorally**: drive the exact input that the feature must change, and assert the
observable difference. If a flag gates a method, toggle the flag and require the output to flip. If
the output never changes, the feature is inert, whatever the logs say.

The classic mechanism in Spring Boot: `@ConditionalOnBean(X.class)` on a bean registered by an
`@AutoConfiguration` that does **not** declare `@AutoConfigureAfter` the configuration that produces
`X`. Auto-configuration sorting falls back to alphabetical class-name order, so the consumer is
evaluated first, the condition is silently false, and the bean is never created. No error, no
warning, no log line.

**Why:** found while building `examples/gcp-simple`, where `@ToggleOnFlag` never invoked its
`orElse` fallback. The aspect logs nothing, so the only thing that exposed it was calling the gated
endpoint with the flag on and off. The existing `unleash-simple` example was inert in exactly the
same way, which is how it survived unnoticed: nobody had ever asserted the flip.

**How to apply:** for any conditionally-wired feature, (1) run the toggle-and-observe test, not a log
grep; (2) run the same test against a pre-existing example or module as a control before blaming
your own new code — a bug that reproduces in the sibling is not yours; (3) when the condition is
`@ConditionalOnBean`, check the producing configuration is named in an `@AutoConfigureAfter`; (4) if
a forced bean definition (declare it yourself in a plain `@Configuration`) makes the feature work,
the condition, not the classpath, is at fault. Related:
`offline-build-pom-only-cache-entries` (rule out your own flags before declaring something absent).
