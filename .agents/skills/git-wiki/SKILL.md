---
name: git-wiki
description: |
   Build and maintain local code-project wikis under .wiki or language-specific wiki roots such as .wiki-cn and .wiki-fr. Use this skill whenever the user mentions "wiki", "ingest", "refresh wiki", "update wiki", "lint wiki", "check wiki", "document the codebase", "export wiki", "bundle wiki", "archive wiki", or asks a question that can be answered from wiki pages. Also use it when the user asks how something works in the project and a wiki page could capture the answer for future reference.
version: "1.6.0"
---

# Project Wiki

Maintain local wikis for a code project under `.wiki` by default, with optional language-specific roots such as `.wiki-cn`, `.wiki-fr`, or an exact user-named wiki directory such as `wiki-cn`.

Source of truth is always the current git-tracked repository files at `HEAD`, including tracked documentation in `.docs/` and `docs/`, not the wiki.
The wiki files themselves may be untracked; read and update the live selected wiki root from the working tree when present.

## Rules

* Operate only on git-tracked files and git history
* Ignore staged and unstaged uncommitted changes; ingest and query verification use committed content at `HEAD` only
* Select exactly one wiki root per request. Use `.wiki` by default; use the explicit root or language the user names, such as `.wiki-cn`, `wiki-cn`, `.wiki-fr`, "Chinese wiki", or "French wiki"
* Each wiki root is independent. Do not sync, compare, translate, merge, or copy pages between language roots unless the user explicitly asks
* Exclude all wiki roots from git-based source selection: `.wiki/**`, `.wiki-*/**`, and `wiki-*/**`
* Do not assume any wiki root is git-tracked; use the selected root's current `index.md` checkpoint when it exists
* Every wiki page must have YAML frontmatter
* The selected root's `index.md` must be updated on every ingest
* Do not ask the user before ingesting
* Export reads the live working-tree files under the selected wiki root, not the git-tracked repository at `HEAD`

## Wiki Layout

Required:

* `<wiki-root>/index.md` — human entry point and master catalog (tracks last ingest commit for that root only)

Optional (create as needed):

* `<wiki-root>/overview/` — orientation pages that explain the project, startup path, storage, and reading path
* `<wiki-root>/features/` — one page per significant product capability or implementation area
* `<wiki-root>/bug-fixes/` — one page per notable bug fix or patch
* `<wiki-root>/concepts/` — cross-cutting ideas and patterns
* `<wiki-root>/entities/` — data models, schemas, types
* `<wiki-root>/flows/` — sequences, pipelines, request lifecycles
* `<wiki-root>/risks/` — invariants, compatibility contracts, fragile behavior, and things not to break
* `<wiki-root>/reference/` — code-shaped reference pages when a source path needs focused explanation
* `<wiki-root>/notes/` — ad-hoc or open questions

### Human understanding structure

The wiki is for a human to understand the codebase, not for the repository to describe itself. Organize the front door around the reader's questions before the code's folders.

`<wiki-root>/index.md` must answer these questions, in this order:

1. What is this?
2. Get started
3. Why does it exist?
4. What happens when I run it?
5. Where is data saved?
6. What are the important moving parts?
7. What should I avoid breaking?
8. Where do I look first?

Use these questions as the primary navigation. `features/`, `entities/`, `flows/`, and `reference/` are supporting evidence, not the first mental model.

The `Get Started` section is for codebase orientation, not a duplicate README. It should give a smart newcomer the first 10 minutes:

* the main command to run
* required environment or services
* expected successful output
* the first files to read
* one safe first change
* one tempting dangerous change

When a repo already has setup instructions in `README.md` or `docs/`, link to them and summarize only what helps the reader understand the code.

### Wiki root selection

* Default wiki root: `.wiki`
* Language-specific roots: prefer `.wiki-<language-code>` when creating a new language wiki, for example `.wiki-cn` for Chinese and `.wiki-fr` for French
* If the user names an exact directory, such as `wiki-cn`, use that exact root instead of normalizing it
* Language roots do not inherit checkpoints, pages, links, or stale status from each other
* When the user asks for a language wiki by language name, write page prose in that language and keep code identifiers, file paths, and API names unchanged

## Page Conventions

### index.md frontmatter

`index.md` carries extra fields to track ingest state:

```yaml
---
title: "Project Wiki"
type: "index"
status: "active"
language: "<language-code|default>"
last_commit: "<full-sha>"
updated_at: "YYYY-MM-DD"
---
```

`last_commit` is the full git commit SHA at the time of the most recent full-repository ingest for the selected wiki root. This checkpoint lives in the selected root's current `index.md` file whether or not that root is git-tracked. On the next ingest for the same root, prefer this saved `last_commit` to find changed files with `git diff --name-status -M <last_commit> HEAD`.

If git metadata is unavailable, do not fall back to filesystem scanning. Ingest and lint are unavailable until git access works again.

### Other pages

```yaml
---
title: "Page Title"
type: "overview|feature|bug-fix|concept|entity|flow|risk|reference|note"
status: "draft|active|stale"
language: "<language-code|default>"
source_paths: []
updated_at: "YYYY-MM-DD"
---
```

Set `language` to `default` for `.wiki`, `cn` for `.wiki-cn` or `wiki-cn`, `fr` for `.wiki-fr`, and the matching language code for other language-specific roots.

Use `status: "stale"` when the source file has changed significantly since the page was last updated, or when the page describes something that no longer exists. During ingest, pages whose `source_paths` changed since `last_commit` should be updated or marked stale. Do not delete stale pages automatically — mark them and note the drift so the user can decide.

### Page naming

* Use lowercase, hyphenated filenames: `auth-middleware.md`, `user-schema.md`
* Name pages for the human idea first: `login-check.md`, `saved-data-shape.md`, `startup-flow.md`
* Use source-path names only for reference pages or when there is no clearer reader-facing name: `src/api/routes.ts` → `<wiki-root>/reference/src-api-routes.md`
* Use descriptive names for concepts and flows: `<wiki-root>/concepts/request-lifecycle.md`

### Wiki links

Use `[[page-slug]]` links between pages whenever a page references something covered on another page. The target should match the destination filename without the `.md` extension. This makes the wiki navigable and lets lint resolve links deterministically.

## Ingest

Trigger on:

* `ingest`
* `ingest [file]`
* `ingest [folder]`
* `ingest wiki-cn`
* `ingest .wiki-fr`
* `refresh wiki`
* `update wiki`
* `update Chinese wiki`

Workflow:

1. Select the wiki root from the request. Default to `.wiki`; use an explicit root or language-specific root when named
2. Read `<wiki-root>/index.md` from the working tree if it exists, even when the wiki root is untracked — note `last_commit` from frontmatter
   * Missing `<wiki-root>/index.md` or a missing `last_commit` means there is no prior ingest checkpoint for that root
3. Run `git rev-parse HEAD` to get the current HEAD commit SHA
4. Detect changed files:
   * If `last_commit` is set: `git diff --name-status -M <last_commit> HEAD` and exclude all wiki roots (`.wiki/**`, `.wiki-*/**`, `wiki-*/**`)
   * Use the saved `last_commit` even if the selected wiki root is untracked; do not treat an untracked wiki root as a fresh ingest from `HEAD`
   * Use the git status codes to distinguish added, modified, deleted, and renamed paths when updating wiki pages
   * If no prior ingest: build a broad ingest candidate set from `git ls-files` at `HEAD`, excluding all wiki roots (see Ingest Priorities below)
5. Resolve scope:
   * If a file or folder is specified, prioritize it — limit the tracked candidate set to that scope
   * For a scoped ingest, do not advance `<wiki-root>/index.md:last_commit` unless the scoped ingest also covers every file changed since `last_commit`
   * Otherwise ingest changed files first, then fill in missing coverage
6. Read relevant tracked repository files — understand their role
7. Update or create wiki pages under the selected wiki root
8. Mark pages in the selected wiki root for deleted source files as `status: "stale"` with a note in the page body, and update moved or renamed pages based on rename information from git
9. Update `<wiki-root>/index.md`:
   * Always set `updated_at` to today
   * Set `last_commit` to the current HEAD SHA only after a full ingest or after processing the complete changed-file set from the previous `last_commit`
   * If a broad ingest stops early after partial coverage, leave `last_commit` unchanged and note in `index.md` that bootstrap coverage is still partial
   * Never discard an existing saved `last_commit` merely because the selected wiki root is untracked

Ingest is automatic. Do not ask the user for confirmation.

If git commands fail, report that the wiki skill cannot ingest until git access is restored. Do not substitute directory walking.

## Export

Trigger on:

* `export wiki`
* `bundle wiki`
* `archive wiki`
* `one-page wiki`
* `prepare wiki for notebooklm`
* `send wiki to notebooklm`

Workflow:

1. Select the wiki root from the request. Default to `.wiki`; use an explicit root or language-specific root when named
2. Check whether `<wiki-root>/index.md` exists in the working tree
   * If the selected wiki root is missing or clearly incomplete, run ingest first without asking
3. Export the live selected wiki from the working tree, not from `HEAD`
4. Use the bundled zero-dependency CLI:

```bash
node skills/git-wiki/scripts/export-wiki-bundle.mjs \
  --input-dir <wiki-root>
```

5. Default output is `index-{simple-timestamp}.md` in the selected wiki root
6. If the user wants a different destination, pass `--output-file <path>`
7. If the user wants a custom label in the export header, pass `--root-name <name>`
8. The export must produce:
   * a repository/wiki header with generation date
   * a table of contents covering every bundled page
   * one section per wiki file with selected frontmatter preserved
   * explicit file start/end markers so the bundle works as an archive artifact or NotebookLM source
9. When the request is specifically about NotebookLM, export first, then hand the produced bundle file to the `notebooklm` skill as the source artifact

Export expectations:

* Prefer bundling `<wiki-root>/**/*.md`
* Preserve only useful frontmatter fields such as `title`, `type`, `status`, `language`, `tags`, `description`, `source_paths`, `updated_at`, and `last_commit`
* Normalize `[[wiki-links]]` into plain text so downstream tools do not need Obsidian semantics
* Strip Obsidian image embeds such as `![[image.png]]`
* Keep the output deterministic by sorting files by relative path
* Use a simple timestamp in the default filename, for example `.wiki-fr/index-20260510-143522.md`
* Do not treat export as a substitute for ingest; export packages the current wiki, it does not rebuild it

### Ingest Priorities

When doing a broad ingest from git-tracked files, build the human entry point first, then add supporting code pages. Work in this order and stop when you have good coverage (typically 10–20 pages for a medium project):

1. Root readme and tracked documentation under `.docs/` and `docs/`
2. Package/build/workspace config (`package.json`, `pyproject.toml`, `Cargo.toml`, etc.) to determine how the project runs
3. Entry points (`main.ts`, `index.ts`, `app.py`, `cmd/`, etc.) to explain startup and runtime behavior
4. Storage, generated outputs, config, secrets, caches, logs, and persistence boundaries → `<wiki-root>/overview/` or `<wiki-root>/entities/`
5. Core user/system workflows → `<wiki-root>/flows/`
6. Important moving parts, concepts, and subsystem boundaries → `<wiki-root>/concepts/` or `<wiki-root>/features/`
7. Contracts and fragile areas the reader should avoid breaking → `<wiki-root>/risks/`
8. API definitions, schemas, and persistence layers → `<wiki-root>/features/` or `<wiki-root>/entities/`
9. Jobs, queues, background workers → `<wiki-root>/features/`
10. UI structure (if present) → `<wiki-root>/features/`
11. Tests (summarize behavior expectations and safety nets, don't document every test file)
12. Notable bug fixes visible in git history → `<wiki-root>/bug-fixes/`

For large repos (100+ files), focus on depth over breadth: deeply document the 10 most important modules rather than shallowly touching 50.

## Query

When asked a project question:

1. Select the wiki root from the request. Default to `.wiki`; use an explicit root or language-specific root when named
2. Read `<wiki-root>/index.md` if it exists
3. Read relevant wiki pages in the selected root if they exist
4. If the selected wiki is missing, the relevant page is missing, or a page is marked stale, verify directly against git-tracked repository source before answering
5. Prefer tracked documentation in `.docs/` and `docs/` when it is relevant to the question, then confirm against code or config when needed
6. Verify against git-tracked repository source when the answer involves specific values (counts, names, configs)
7. Answer in the selected wiki's language when the user named a language root, with concise explanation and `[[wiki-link]]` citations when relevant wiki pages exist
8. If you created new understanding not yet in the selected wiki root, mention it — the user can ask you to save it

## Lint

Trigger on:

* `lint`
* `lint wiki`
* `check wiki`
* `lint .wiki-fr`
* `check Chinese wiki`

Checks:

* Contradictions between pages (two pages making conflicting claims)
* Orphan pages with no inbound `[[links]]`
* Concepts mentioned across multiple pages but lacking their own page
* Stale pages (`status: "stale"`) that may have drifted from current source
* Missing coverage for major changed modules (files changed since `last_commit` with no wiki page)
* Broken `[[wiki-links]]` where the target page doesn't exist

### Lint Output Format

```
## Wiki Lint Report — YYYY-MM-DD

### Errors
- [page] description of contradiction or broken link

### Warnings
- [page] description of potential issue

### Suggestions
- description of missing coverage or improvement

### Next Questions
- open questions worth investigating
```

Do not auto-fix unless explicitly asked.

## Writing Guidance

* Write for a smart newcomer, not a maintainer. Use layman's terms first, then add the exact code term only when it helps: "the login check" before "auth middleware", "saved data shape" before "schema", "background task" before "worker"
* Structure pages around the reader's question, then ground the answer in files. Do not lead with a directory tour unless the page is explicitly reference material
* Explain why a file matters in the product or workflow before naming implementation details
* Avoid unexplained acronyms, framework jargon, and internal shorthand. If a technical term is necessary, define it in the same sentence
* Summarize; do not reproduce large code blocks — a few lines of example code is fine, but explain patterns rather than paste implementations
* Use concrete file paths as anchors (`src/api/routes.ts:42`)
* Separate facts (directly read from source) from inferences (your interpretation)
* Cross-link related pages with `[[wiki-links]]`
* Keep pages under ~500 words; create additional pages rather than growing one page long
* Prefer useful coverage over exhaustive coverage — a good wiki is navigable, not encyclopedic

## Quality Bar

The wiki succeeds when:

* `<wiki-root>/index.md` helps a new reader navigate the project in under 2 minutes
* A newcomer can answer what the project is, why it exists, what happens when it runs, where data is saved, what matters, what not to break, and where to look first
* Answers can start from wiki pages and be verified in code
* Ingest detects changed files automatically using the saved `last_commit`
* Lint catches drift before the wiki becomes misleading
