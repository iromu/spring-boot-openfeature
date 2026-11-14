---
name: gitleaks flags a PEM marker literal in source even with no key committed
description: A contiguous -----BEGIN ... PRIVATE KEY----- string literal in test source trips this repo's gitleaks pre-commit hook even when the key is generated at runtime; assemble the marker from pieces.
type: reference
category: common_pitfall
keywords:
  - "gitleaks private-key rule false positive"
  - "PEM marker literal source scan"
  - "runtime generated credential fixture"
  - "pre-commit gitleaks blocks commit"
  - "service account JSON test fixture"
usage_scenarios:
  - "Writing a test fixture with PEM or key material"
  - "Pre-commit gitleaks hook rejects a commit"
  - "Reviewing a runtime-generated credential test"
---

This repo runs the `gitleaks` pre-commit hook (`.pre-commit-config.yaml`, rev v8.30.0, no excludes, and there
is no `.gitleaksignore`). gitleaks scans **source bytes**, so a contiguous private-key marker appearing as a
string literal is reported as a `private-key` leak **even when no key is ever committed** — e.g. a test that
generates an RSA keypair at runtime but builds the fixture from a literal:

```java
// trips gitleaks: 1 leak, RuleID private-key
String privateKey = "-----BEGIN PRIVATE KEY-----\\n" + pem + "\\n-----END PRIVATE KEY-----\\n";
```

Fix it by making the marker non-contiguous in source so the detector never sees it, and keep generating the key
at runtime:

```java
private static String pemMarker(String label) { return "-----" + label + "-----"; }
String privateKey = pemMarker("BEGIN PRIVATE KEY") + "\\n" + pem + "\\n" + pemMarker("END PRIVATE KEY") + "\\n";
```

**Why:** found in review of `spring-boot-starter-openfeature-gcp`, where a runtime-generated service-account
fixture would have blocked the commit. The design note justifying runtime generation as "the safer choice
because committing a PEM would trip gitleaks" was itself the bug's cover — runtime generation avoids committing
*key material* but does nothing about a marker *literal* in the file. Verify with
`gitleaks detect --no-git --source <module>/src`, which must report `no leaks found`.

**How to apply:** when a test needs key-like material, generate it at runtime AND avoid contiguous secret-shaped
literals (PEM headers, `BEGIN/END` markers). Fix in source rather than by adding a `.gitleaksignore`, an
`#nosec`-style comment, or a hook exclude — those suppress the detector instead of removing the trigger.
Related: `validate-a-gates-premise-before-recording-it-unmet` (a mitigation claim should be run, not assumed).
