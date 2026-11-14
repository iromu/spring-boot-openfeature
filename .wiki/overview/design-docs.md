---
title: "Design Documents"
type: "overview"
status: "active"
language: "default"
source_paths:
  - "docs/architecture.md"
  - "docs/provider-starter-contract.md"
  - "docs/quality-and-testing.md"
  - "docs/dependency-management.md"
  - "docs/backlog.md"
updated_at: "2026-11-14"
---

# Design Documents — `docs/` is the design home

`docs/` holds the project's design documents. It replaced the OpenSpec planning
workflow on 2026-11-14 and is now the place to read (and update) a decision.
Code stays authoritative wherever a document and the code disagree.

| Document | Answers |
| --- | --- |
| `docs/architecture.md` | Consumer contract, module map, the provider → `Client` wiring flow, extension points, `@ToggleOnFlag`, health and security, the examples reactor |
| `docs/provider-starter-contract.md` | The shared contract every provider starter honors, per-provider deltas, the add-a-starter checklist, and the spec-to-code divergences found while consolidating |
| `docs/quality-and-testing.md` | Testing tiers, the behavioral contract, the JaCoCo coverage-gate mechanism and per-module floors, CI posture and its two known gaps |
| `docs/dependency-management.md` | Where versions live, the BOM, the OpenFeature SDK ↔ provider lockstep rule, upgrade policy, blocked pins, how to regenerate the audit |
| `docs/backlog.md` | The three items that were never implemented: the blocked Featurevisor and Hyphen AI starters and the Unleash real-endpoint gate |
| `docs/dependency-versions.md` | Generated audit of every plugin and dependency pin against its latest release — see [[reference/build-and-modules]] |

## What OpenSpec was, and what is still around

The repo used to keep a change-proposal tree under `openspec/` (18 archived and
3 open changes, plus 20 per-capability spec files). Those trees were deleted on
2026-11-14: they had drifted from the code (mandatory properties that do not
exist, stale version enumerations) and nothing enforced them. Their durable
content is the five documents above.

Deliberately kept, because starting a fresh OpenSpec change is still a valid way
to pick up a backlog item:

- `openspec/config.yaml`
- the OpenSpec CLI install in `setup.sh`
- the `openspec-*` skills in `skills-lock.json`

## Two things to remember

- **Update the document with the decision.** A design change that only lands in
  code is how `docs/` becomes as misleading as the old specs were.
- **The divergences section is load-bearing.** `docs/provider-starter-contract.md`
  lists where the retired spec text disagreed with the code (Octopus Deploy and
  FeatBit required properties, OFREP and Optimizely missing the credential-free
  and fail-fast rules). Read it before trusting any quoted-from-memory spec.

Related: [[features/provider-starters]] for the starter pattern as implemented,
[[risks/fragile-behavior]] for the invariants that break silently.
