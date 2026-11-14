---
name: Validate a gate's premise before recording it as NOT SATISFIABLE
description: Before writing "gate unmet / NOT SATISFIABLE" into a tasks.md outcome, prove the artifacts and endpoints the gate names actually exist; "not executable as written" and "unmet" are different findings with different remedies
type: feedback
---

When an OpenSpec task gates a promotion on running some named test or reaching some named
endpoint, verify the gate names things that **exist** before recording it as unmet. Check the
upstream repository's own source tree, not merely the published artifact, and check this repo's
own config surface for the fields the gate says are "required".

**Why:** `upgrade-dependencies-2026-09` task 5.2 gated the Unleash client 12.3.0 promotion on
re-running `UnleashConfigAndAccessProviderTest` / `AbstractUnleash2ProviderTest` against a real
endpoint with `adminBaseUrl` + `apiToken`, and recorded `Outcome: NOT SATISFIABLE`. The conclusion
survived, the reasoning did not: those two classes exist **nowhere** — 0 matches by filename and by
content across the 29 test sources of the upstream client repo's own tree, no `-tests`/
`-test-fixtures` classifier is published — and `UnleashProperties` has no `adminBaseUrl`/`apiToken`
fields at all. The prior note had checked only the shipped jar (where the absence is real) and never
the source tree, so an *unexecutable* gate was written down as an *unmet* one. That distinction
matters operationally: an unmet gate means "wait for evidence", so the pin sat blocked, while an
unexecutable gate means "the gate is void — replace it", which is what actually closed it (a real
endpoint test this repo owns).

**How to apply:** when asked to close a gate, first ask whether the gate is executable at all, and
say which of the two it is. Two traps that made a correct-looking test look like a client regression
when it was an authoring defect, both silent-failure shaped: seeding an Unleash admin token via
`INIT_ADMIN_API_TOKENS` is all-or-nothing (`api-token-service.initApiTokens` returns early once
`api_tokens` is non-empty), so tokens must be seeded on a fresh database; and the admin create
endpoint ignores the `environments` block it is handed, so a flag stays `enabled=false` until
`POST .../features/{name}/environments/{env}/on` targets the environment the client token is scoped
to. Also: a test that only passes proves nothing until you have seen it fail for the right reason —
and "the server logged no request" is not evidence of no request; put a logging proxy on the wire
before concluding anything about a client's traffic.
