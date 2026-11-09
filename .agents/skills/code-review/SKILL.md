---
name: code-review
description: >
  Run an iterative review–critique–fix loop over real git changes — uncommitted work, a
  feature branch's changes versus the default branch, or any commit range, PR, or file set
  the user names. Use when the user asks to review code changes, review what's uncommitted,
  review a branch before merging, run a code review, critique and fix a diff, or polish a set
  of changes until the build and tests are green. Spawns read-only reviewer and critic
  subagents plus surgical fixer subagents, and applies fixes until consensus is reached.
license: MIT
metadata:
  author: "Iván Rodríguez Murillo <wantez@gmail.com>"
  version: 2.0.0
  category: development
  tags: [code-review, git, diff, subagents, quality, pre-merge]
---

# Code Review

Run an iterative review–critique–implement loop over **actual git changes**: spawn subagents from the self-contained role briefs below, apply fixes where issues are found, and repeat until the consensus check passes.

The skill does NOT depend on any pre-installed agent definitions. The role briefs ARE the prompts: each subagent receives its role, rules, and output format from this skill.

**Scope is git, not a spec.** The unit under review is a set of changes in the working tree or branch history — resolved in the Target Selection step. There is no proposal/design/tasks artifact; the diff and the changed files are the source of truth.

## When to Use

- Reviewing uncommitted changes before committing them
- Reviewing a feature branch's changes before merging or opening a PR
- Running a full review–fix cycle on a diff until the build and tests are green
- Critiquing and fixing a specific commit range, PR, or file set the user names
- A pre-merge quality gate on a set of changes

## Target Selection

Resolve **what to review** before anything else. Priority order:

1. **User-provided changes** — if the user pastes the changes directly (a diff, a patch, a PR body, or the changed files as text), review that text as the change set and skip the git commands below. This covers "review this PR / this patch" when the changes arrive as text rather than a repo to inspect.
2. **User-named target** — if the user named a range, commit, PR, "all uncommitted", "the branch", or a file set, use exactly that.
3. **Auto-detect** (no target given):
   - `CUR=$(git rev-parse --abbrev-ref HEAD)`
   - `DEF=$(git symbolic-ref --quiet --short refs/remotes/origin/HEAD | sed 's@^origin/@@')` — fall back to `main`, then `master`, if unset.
   - If `CUR == DEF` (or `CUR` is `HEAD`, i.e. detached): target = **UNCOMMITTED**.
   - Otherwise: target = **BRANCH** (the current branch's changes vs `DEF`).

Resolve the target into a **base** (the commit to diff against) and the change set:

**UNCOMMITTED** — base is `HEAD`:
```bash
git status --porcelain          # staged + unstaged + untracked
git diff HEAD --stat            # summary
git diff HEAD                   # full diff, staged + unstaged
git status --porcelain | grep '^??'   # untracked files (not in the diff — read them directly)
```

**BRANCH** — base is the merge-base with the default branch, so the diff includes any uncommitted work on top:
```bash
BASE=$(git merge-base "$DEF" HEAD)
git log --oneline "$DEF"..HEAD        # the branch's commits
git diff --name-status "$BASE"        # per-file A/M/D/R list
git diff --stat "$BASE"              # summary
git diff "$BASE"                     # full diff (base → working tree)
```

**USER-NAMED** — resolve the range into a base (and optional head) and run `git diff <base> [<head>]`; a PR number maps to its base/head via `gh pr view <n> --json headRefName,baseRefName`.

Announce the resolved target and how to override it, e.g. `Reviewing: branch feature/auth vs main (3 commits, 12 files)`. If the resolved change set is empty, stop and report "nothing to review".

## Spawning Roles

For each role, spawn a general-purpose subagent (Agent tool, `subagent_type: general-purpose`) with:
1. The role brief, verbatim, from the Role Briefs section.
2. The review context block (built in the Review Loop, step 2).
3. Any role-specific evidence the steps name for that role (for the critic: the reviewer's findings, verbatim).

Do not assume any named agent type (reviewer/critic/implementer/etc.) exists. If subagent spawning is unavailable, the executor runs each brief itself, one at a time, in sequence — honoring the same read-only/write boundaries.

## Review Loop

1. **Select the target** — per Target Selection.

2. **Build the review context block.** Identify the project's build and test commands from the repo's manifests and existing patterns (`package.json`, `pom.xml`, `pyproject.toml`, `Makefile`, etc.); the user may override them. If no build system or test suite exists for the changed files, record `Build: none` / `Tests: none` and proceed — do not invent commands. Otherwise **run the build and test commands yourself before spawning any role** and record what you observe — read-only roles consume these results instead of running the commands (the build rewrites generated output such as `dist/`, which sits outside a read-only role). Assemble:

   ```
   Target: <UNCOMMITTED | BRANCH <name> vs <default> | <user-named range>>
   Base: <commit-ish (HEAD for uncommitted, merge-base for branch)>
   Files changed:
     <name-status list: M path, A path, D path>
   Commits in scope:
     <git log oneline, or "(uncommitted work only)">
   Build: <build command>
   Tests: <test command>
   Build result: <observed outcome, plus the failure output if any>
   Test result: <observed outcome, plus the failure output if any>
   ```

3. **Review** — spawn a subagent with the REVIEWER brief. Read-only role.

4. **Critique** — spawn a subagent with the CRITIC brief, passing the reviewer's findings verbatim. Read-only role.

5. **Implement fixes** — for each confirmed must-fix issue, spawn a subagent with the IMPLEMENTER brief (bugs, missing behavior, failing tests) or the SIMPLIFIER brief (complexity, dead code). One issue per subagent. Run fixers sequentially with disjoint file scopes. If the user asked for review-only, skip this step and go straight to the report.

6. **Re-verify** — repeat steps 3–5 until the reviewer reports `CLEAN` and the critic reports `PASS` (no blocking findings). `WARNING` and `SUGGESTION` findings never keep the loop open: collect them for the final report. Run at most 3 cycles; when a cycle applies no fix, or repeats the previous cycle's blocking findings, stop and report `NOT REACHED` with the outstanding evidence.

7. **Consensus check** — the executor determines consensus directly from evidence (no subagent). Consensus holds only when ALL of:
   - Reviewer verdict `CLEAN`
   - Critic verdict `PASS` (no blocking findings)
   - The build completes with no errors
   - All tests for the changed files pass

   If any condition fails, feed the failing evidence back into step 5 and repeat the loop.

8. **Report**

   ```markdown
   ## Code Review: <target>

   | Cycle | Reviewer | Critic | Fixes applied |
   |-------|----------|--------|---------------|
   | 1     | ...      | ...    | ...           |

   **Consensus:** ✅ REACHED / ❌ NOT REACHED (outstanding items)
   **Build:** <result>
   **Tests:** <pass/fail summary>
   **Verdict:** Clean / Not ready — <reasons>
   ```

## Role Briefs

Each brief is a complete, self-contained prompt. Pass it verbatim together with the review context block.

### REVIEWER (read-only)

You are a code reviewer auditing a set of git changes. READ-ONLY: do not modify, create, or delete any file; do not run mutating commands (no git commit, no edits, no installs, no build — the build rewrites generated output such as `dist/`).

Given the review context block, do:
1. Read the full diff and open every changed file in its full context (not just the hunks).
2. Hunt for correctness bugs: logic errors, off-by-one, wrong operator, broken control flow, unhandled null/empty/boundary/concurrency cases.
3. Check security: secrets, injection (SQL/command/HTML), unsafe deserialization, missing authorization, path traversal.
4. Check for regressions against surrounding code: does the change break callers, invariants, or documented behavior?
5. Check test coverage: does each changed behavior have a test that exercises it? A new or changed behavior with no test is a must-fix.
6. Consume the recorded Build result and Test result from the context block; report the failures they carry with their output. If either result is missing or predates the latest fix, raise it as a must-fix.

Report every finding in exactly this format:
- `MUST-FIX | file:line | issue | evidence (quoted code or test output)`
- `SUGGESTION | file:line | issue | why`

End with one line: `Verdict: CLEAN` or `Verdict: ISSUES (N must-fix, M suggestions)`.

### CRITIC (read-only)

You are a critic challenging a review's findings and hunting for what it missed. READ-ONLY: do not modify any file.

Given the review context block plus the reviewer's findings, do:
1. Challenge each reviewer finding: is it real? Verify with the diff, test output, or quoted code. Mark each finding `CONFIRMED` or `REJECTED`, with evidence.
2. Find what the reviewer missed: edge cases (null, empty, boundary, concurrency), logic gaps, silent failures, regressions, and scope creep beyond the stated target.
3. Flag over-engineering: abstractions, config, or complexity the change does not need (YAGNI).
4. Classify every finding: `BLOCKING` (correctness bug, data loss, security, failing test) | `WARNING` | `SUGGESTION`. Offer a concrete alternative for each, not just criticism.

Report `PASS` when there are no blocking findings, whatever the warning and suggestion count; `WARNING` when a non-blocking finding deserves the user's attention; `BLOCKING` only when a blocking finding stands.

End with one line: `Verdict: PASS` | `Verdict: WARNING` | `Verdict: BLOCKING (N blocking, M warnings, K suggestions)`.

### IMPLEMENTER (writes code)

You are implementing a fix for one confirmed issue. SURGICAL CHANGES ONLY: fix the stated issue and nothing else — no refactoring, no adjacent fixes, no new features, no new abstractions.

Given the review context block plus one issue (file, line, description, evidence), do:
1. Read the surrounding code and existing tests.
2. If the issue is missing or wrong behavior: write or update the failing test first, then the minimal fix (red → green).
3. Run the test command from the context block; tests must pass before you finish. If a test was already failing before your change, prove it (show the pre-change run) and say so.
4. Stay inside the files named in the issue, plus the test file that directly covers them: writing the regression test your step 2 calls for is inside your scope. If the fix requires touching any other file, stop and report why instead of proceeding.

Report: files changed; one line per change; the test command and its result output.

### SIMPLIFIER (writes code)

You are simplifying code flagged as over-complex. BEHAVIOR-PRESERVING REFACTOR ONLY: no new features, no behavior changes, no new abstractions.

Given the review context block plus the target files and the flagged complexity, do:
1. Before removing or renaming anything, check usages (grep) and git history. Exported symbols and public APIs are contracts: do not rename or remove them.
2. In small safe steps: remove dead code, flatten nesting, consolidate duplication, improve naming.
3. Run the test command from the context block after each step; on failure, revert that step.

Report: files changed; lines removed/changed; the test command and its result output.

## Guardrails

- **Read-only roles never write** — only the IMPLEMENTER and SIMPLIFIER modify code, one issue at a time, sequentially, with disjoint file scopes. Enforce this with tool capabilities, not instruction alone: spawn the REVIEWER and CRITIC without file-editing or install tools, keep their shell to read-only inspection such as `git diff`, and keep the build command in the executor.
- **Evidence over assertions** — every finding and every fix cites `file:line`, test output, or build output.
- **Distinguish pre-existing failures on proof** — a failure is excused from consensus only when the same failure is reproduced on the pre-change baseline (a clean checkout of the base commit, or the executor's run recorded before any fix), with that reproduction quoted in the report. Without that proof it blocks.
- **Consensus is the only exit** — one clean reviewer pass is not enough; all four consensus conditions must hold.
- **The loop is bounded** — at most 3 cycles, and a cycle that applies no fix or repeats the previous cycle's blocking findings ends it with `NOT REACHED` plus the outstanding evidence. A bounded stop is reported as such, never dressed up as consensus.
- **Do not rewrite history** — the loop edits the working tree and may add tests, but it never commits, rebases, or amends; committing is the user's call.

## When NOT to Use This Skill

| Situation | Better Alternative |
|-----------|-------------------|
| Just viewing a diff | `git diff` is enough — no review loop needed |
| Security-focused audit of a codebase | Use the `security-audit` skill |
| Formatting/lint-only pass | Run the project's linter/formatter directly |
| Reviewing a remote PR's CI status | Use the platform's PR tooling (e.g. `gh pr view`) |
| A one-file, obvious change | A quick manual read is faster than spawning the loop |

## Examples

**"Review my changes before I commit."** On `main` with a dirty tree → Target Selection resolves to UNCOMMITTED; the loop reviews the `git diff HEAD`, and the fixers patch the working tree.

**"Review this branch before I merge."** On `feature/auth` → BRANCH mode; base is `git merge-base main HEAD`, so the diff covers every commit on the branch plus any uncommitted work, and the commit list is `main..HEAD`.

**"Review the last 3 commits."** User-named target → `git diff HEAD~3`; the loop scopes its review to that range only.

**"Just review, don't fix anything."** Step 5 is skipped; the skill reports findings and stops at the reviewer + critic pass.

## Pitfalls

- ⚠️ `git diff HEAD` does not include untracked files — list them via `git status --porcelain | grep '^??'` and read them directly or they are silently unreviewed.
- ⚠️ On a branch, `git diff` (no args) shows only the last commit; always diff from the merge-base to capture the whole branch.
- ⚠️ Build/test commands are project-specific — detect them from manifests, never assume `npm test` or `mvn test`.
- ❌ Don't let a reviewer claim "no tests" without checking for test files that cover the changed behavior.
- ⚠️ A fixer that touches files outside its issue scope breaks the disjoint-scope guarantee — stop it and report, don't let it proceed.
- ❌ Don't dress up a bounded stop (3 cycles, no progress) as consensus.

## Verification

- [ ] The target was resolved and announced (uncommitted / branch / user-named)
- [ ] Build and test commands were run by the executor before any role spawned
- [ ] Every finding cites `file:line`, test output, or build output
- [ ] Read-only roles made no file changes
- [ ] Consensus required all four conditions, not just a clean review
- [ ] The report shows the cycle table, build, tests, and verdict
