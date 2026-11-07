---
name: An absent env var does not trigger @DisabledIfEnvironmentVariable
description: JUnit's @DisabledIfEnvironmentVariable(matches="") leaves a test enabled when the variable is unset, so a real-backend test gated that way fails on credential-less CI; use @EnabledIfEnvironmentVariable(matches=".+").
type: reference
category: testing_standard
keywords:
  - "DisabledIfEnvironmentVariable absent variable"
  - "EnabledIfEnvironmentVariable integration gate"
  - "credential-free CI test skip"
  - "GCP_PROJECT_ID gate"
  - "self-skipping integration test"
usage_scenarios:
  - "Writing a real-backend test that must self-skip"
  - "Reviewing an env-gated integration tier"
  - "Debugging a gated test failing on CI"
---

To make a real-backend test self-skip when its gating environment variable is **absent**, gate it with
`@EnabledIfEnvironmentVariable(named = "X", matches = ".+")`, not
`@DisabledIfEnvironmentVariable(named = "X", matches = "")`.

**Why:** JUnit evaluates `@DisabledIfEnvironmentVariable` only when the named variable *exists* — an unset
variable does not match `""`, so the test stays **enabled** and runs, then fails on a host with no
credentials. Upstream `open-feature/java-sdk-contrib` uses the `@DisabledIfEnvironmentVariable(matches = "")`
form for its GCP integration tests, so copying that gate verbatim into a starter module would break the
credential-free CI guarantee the `add-gcp-starter` spec pins (scenario 6.1: a real-backend test "SHALL
self-skip, rather than fail, when the gating project-identifier environment variable is absent").
`matches = ".+"` covers both absent and empty.

**How to apply:** When a module needs an integration tier that must never fail a credential-less build, use
`@Tag("integration")` + `@EnabledIfEnvironmentVariable(named = ..., matches = ".+")`, and verify the run
reports `Skipped: 1` attributable to *that class only* while the contract tier reports `Skipped: 0`. Keep the
distinction between "this skip is by design" and "a skipping gate is no gate" — see
`validate-a-gates-premise-before-recording-it-unmet` and the `add-gcp-starter` design.md D4. Applies to the
sibling `add-ofrep-starter` / `add-optimizely-starter` changes if they add real-backend tiers.
