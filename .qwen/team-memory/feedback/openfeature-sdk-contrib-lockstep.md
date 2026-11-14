---
name: OpenFeature SDK and contrib provider versions must move in lockstep
description: Bumping dev.openfeature:sdk alone breaks every pinned contrib provider; bump the SDK together with all dev.openfeature.contrib.providers artifacts.
type: feedback
---

Never bump `dev.openfeature:sdk` in `pom.xml` without also bumping every
`dev.openfeature.contrib.providers:*` artifact pinned beside it. They are
compiled against one another and fail at *runtime*, not compile time.

**Why:** Measured on the 2026-09-09 Dependabot sweep. `sdk 1.15.1 -> 1.22.1`
applied alone produced two failures that no compile step predicted:

- `go-feature-flag 0.4.3` -> `NoSuchMethodError: void
  dev.openfeature.sdk.EventProvider.emitProviderReady(ProviderEventDetails)`
  (the signature changed in the SDK, so the old provider cannot link).
- `multiprovider 0.0.3` -> NPE on `AtomicReference… "<local4>.attachment" is null`
  inside `ClientAutoConfiguration`'s `multiClient` factory: the newer `Client`
  reads a field the older provider never initialises.

The failures are asymmetric and non-obvious, so all four combinations of
{old,new} SDK x {old,new} provider were tested. Only "both old" and "both new"
are coherent; the two mixed states are each broken in a different way.

**How to apply:** When a Dependabot PR bumps only the SDK, check whether
matching contrib provider releases exist and take them in the same change, or
hold the SDK bump. After any SDK bump, run the *whole* reactor — modules after
the first failure are SKIPPED, so a single early failure silently hides every
downstream provider module. Also note `go-feature-flag` 1.2.x does blocking
flag I/O while its provider bean is constructed, so
`GoFeatureFlagAutoConfigurationTest`'s handler-less `MockWebServer` parks context
startup forever instead of failing.
