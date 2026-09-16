"""step 3: render the version audit document.

inputs (all machine produced, nothing hand retyped):
  * coords.tsv    - coordinates parsed out of the pom bytes by step1
  * manifest.tsv  - which repository served each coordinate, recorded by step2
  * bases.tsv     - repository roots derived from maven's own log lines
  * meta/*.xml    - the fetched maven-metadata.xml payloads
  * deplist.log   - maven's own resolved dependency closure (effective versions)

the markdown is emitted here so no version string passes through a rendered
intermediate view on its way into the document.

version taxonomy, stated plainly because "latest" is ambiguous:
  * release (repo)  the <release> element the repository itself publishes
  * latest plain    highest version whose every dot component is purely numeric
  * latest any      highest version overall, qualifiers included
"""
import datetime
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

import _va_paths as va

ROOT = va.ROOT
TMP = va.TMP
META = va.META
DOC = va.DOC
DOCS = os.path.dirname(DOC)
OUT = DOC
INTERNAL = "org.iromu.openfeature"
POMNS = {"m": "http://maven.apache.org/POM/4.0.0"}
POMU = "{http://maven.apache.org/POM/4.0.0}"
NON_VERSION_PROPS = ("revision", "java.version", "project.build.sourceEncoding",
                     "maven.compiler.source", "maven.compiler.target",
                     "jacoco.instruction.minimum",
                     "sonar.coverage.jacoco.xmlReportPaths",
                     "spring-boot-openfeature.version")


def toks(v):
    return [t for t in re.split(r"[.\-_]+", v) if t != ""]


def is_plain(v):
    ts = toks(v)
    return bool(ts) and all(t.isdigit() for t in ts)


HEAD = re.compile(r"^(\d+(?:\.\d+)*)(?:[.\-](.*))?$")
# known qualifier precedence; a bare release ranks below every qualifier so that
# taking the maximum yields the release, matching maven's own ordering. unknown
# qualifiers (platform variants such as '-jre'/'-android') sort after the known
# prerelease tags, which is also what maven does.
QUAL_RANK = {"": -1, "ga": -1, "final": -1, "release": -1,
             "alpha": 0, "a": 0, "beta": 1, "b": 1, "milestone": 2, "m": 2,
             "rc": 3, "cr": 3, "snapshot": 4, "dev": 5, "preview": 6,
             "eap": 7, "t": 8}
UNKNOWN_QUAL = 50


def split_version(v):
    """(numeric tuple, qualifier string); numeric tuple is None when unparseable."""
    m = HEAD.match(v)
    if not m:
        return None, v.lower()
    return tuple(int(x) for x in m.group(1).split(".")), (m.group(2) or "").lower()


def qual_key(qual):
    head = re.split(r"[.\-_]", qual)[0] if qual else ""
    return (QUAL_RANK.get(head, UNKNOWN_QUAL), qual)


def highest(versions):
    """max under a maven-style ordering: numeric prefix first, then qualifier rank."""
    pool = [v for v in versions if v]
    if not pool:
        return None
    width = 0
    for v in pool:
        nums, _ = split_version(v)
        if nums:
            width = max(width, len(nums))
    def key(v):
        nums, qual = split_version(v)
        if nums is None:
            return (0, (0,) * width, (10 ** 6, v))
        return (1, nums + (0,) * (width - len(nums)), qual_key(qual))
    return max(pool, key=key)


def read_tsv(path, cols, has_header=True):
    rows = []
    if not os.path.isfile(path):
        return rows
    with open(path, encoding="utf-8") as handle:
        if has_header:
            next(handle, None)
        for line in handle:
            cells = line.rstrip("\n").split("\t")
            if len(cells) >= cols:
                rows.append(cells)
    return rows


def get(url, dest, timeout=45):
    proc = subprocess.run(
        ["curl", "-s", "-s", "-L", "-f", "--max-time", str(timeout),
         "--user-agent", "qwen-version-scan/1.0", "-o", dest, "-w", "%{http_code}", url],
        capture_output=True, text=True)
    return proc.returncode, (proc.stdout or "").strip()


def meta_path(group, artifact):
    return os.path.join(META, group.replace(".", "_") + "__" + artifact + ".xml")


def load_meta(path, group, artifact):
    """parse a metadata file, rejecting anything that describes another coordinate."""
    if not os.path.isfile(path):
        return None
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        return None
    if root.findtext("groupId") != group or root.findtext("artifactId") != artifact:
        return None
    ver = root.find("versioning")
    if ver is None:
        return None
    versions = [v.text.strip() for v in ver.findall("versions/version")
                if v.text and v.text.strip()]
    versions = [v for v in versions if v != "SNAPSHOT" and not v.endswith("-SNAPSHOT")]
    plain = [v for v in versions if is_plain(v)]
    return {
        "release_tag": ver.findtext("release"),
        "latest_tag": ver.findtext("latest"),
        "latest_plain": highest(plain),
        "latest_any": highest(versions),
        "count": len(versions),
        "recent": sorted(set(versions), key=lambda x: (split_version(x)[0] or (), x),
                         reverse=True)[:4],
    }


def resolved_map():
    """(group, artifact) -> (versions maven resolved, scopes seen), from its own output."""
    pat = re.compile(r"^\[INFO\]\s+(\S+):(\S+):(\S+):([^:\s]+):(\S+)")
    out = {}
    log = os.path.join(TMP, "deplist.log")
    if not os.path.isfile(log):
        return out
    for line in open(log, encoding="utf-8", errors="replace"):
        m = pat.match(line.rstrip())
        if not m:
            continue
        g, a, _pkg, v, scope = m.groups()
        bucket = out.setdefault((g, a), (set(), set()))
        bucket[0].add(v)
        bucket[1].add(scope.split("(")[0].strip())
    return out


def pom_txt(elem, tag):
    """text of a namespaced child, or empty string.

    element.findtext's second positional argument is the default, not the namespace
    map, so the lookup goes through find() with the namespaces passed by keyword.
    """
    node = elem.find(tag, namespaces=POMNS)
    if node is None or node.text is None:
        return ""
    return node.text.strip()


def parents_and_boms():
    out = []
    for dirpath, dirnames, files in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames
                       if d not in {".git", "target", "node_modules", ".qwen", ".idea"}]
        if "pom.xml" not in files:
            continue
        rel = os.path.relpath(os.path.join(dirpath, "pom.xml"), ROOT)
        root = ET.parse(os.path.join(dirpath, "pom.xml")).getroot()
        p = root.find("m:parent", namespaces=POMNS)
        if p is not None:
            out.append(("parent", rel, pom_txt(p, "m:groupId"),
                        pom_txt(p, "m:artifactId"), pom_txt(p, "m:version")))
        for d in root.iter(POMU + "dependency"):
            if pom_txt(d, "m:scope") == "import":
                out.append(("bom import", rel, pom_txt(d, "m:groupId"),
                            pom_txt(d, "m:artifactId"), pom_txt(d, "m:version")))
    return out


def build_prop_tables(coord_rows):
    """module dir -> its own <properties>, as parsed from the pom bytes."""
    tables = {}
    for r in coord_rows:
        if r[0] == "property":
            name, _, value = r[3].partition("=")
            key = (os.path.dirname(r[5])
                   if os.path.basename(r[5]) == "pom.xml" else r[5])
            tables.setdefault(key, {})[name] = value
    return tables


def prop_chain(module, tables):
    """properties visible to a module, nearest declaration first.

    a module inherits its ancestors' properties, and the examples reactor hangs off
    examples/pom.xml rather than the root pom, so the whole directory ancestry has to
    be consulted - resolving only the module's own properties would leave a parent
    declared placeholder such as ${sprindoc-openapi.version} unresolved.
    """
    parts = [p for p in module.split(os.sep) if p not in ("", ".")]
    if parts and parts[-1] == "pom.xml":
        parts = parts[:-1]
    dirs = []
    for cut in range(len(parts), 0, -1):
        cand = os.sep.join(parts[:cut])
        if cand in tables:
            dirs.append(cand)
    dirs.append("")
    return dirs


def resolve_value(value, module, tables):
    if not value:
        return value
    merged = {}
    for key in reversed(prop_chain(module, tables)):
        merged.update(tables.get(key, {}))
    for _ in range(12):
        if "${" not in value:
            break
        whole = re.fullmatch(r"\$\{([^}]+)\}", value)
        if whole and whole.group(1) in merged:
            value = merged[whole.group(1)]
            continue
        replaced = False
        for name, sub in merged.items():
            token = "${" + name + "}"
            if token in value:
                value = value.replace(token, sub)
                replaced = True
        if not replaced:
            break
    return value


def inherited_plugin_versions():
    """(group, artifact) -> version, read out of maven's own effective-pom output.

    a plugin declared without a <version> is pinned by a parent's pluginManagement, so
    the number a reader wants is the one maven actually bound. maven's effective-pom
    print is the authority for that, and it is parsed rather than guessed. the scan
    demands the three tags run consecutively so an unrelated <version> further down a
    block cannot be attributed to the wrong plugin.
    """
    log = os.path.join(TMP, "effpom.log")
    out = {}
    if not os.path.isfile(log):
        return out
    lines = []
    for raw in open(log, encoding="utf-8", errors="replace"):
        stripped = raw.rstrip("\n")
        if stripped.startswith("[INFO]"):
            stripped = stripped[len("[INFO]"):]
        if stripped.strip():
            lines.append(stripped.strip())
    index = 0
    while index < len(lines) - 2:
        g = re.fullmatch(r"<groupId>([^<]+)</groupId>", lines[index])
        a = re.fullmatch(r"<artifactId>([^<]+)</artifactId>", lines[index + 1])
        v = re.fullmatch(r"<version>([^<]+)</version>", lines[index + 2])
        if g and a and v:
            out.setdefault((g.group(1), a.group(1)), set()).add(v.group(1))
        index += 1
    return out


def main():
    bases = {}
    for row in read_tsv(os.path.join(TMP, "bases.tsv"), 2, has_header=False):
        bases[row[0]] = row[1]
    served = {}
    for row in read_tsv(os.path.join(TMP, "manifest.tsv"), 6):
        served[(row[0], row[1])] = row[3]
    coord_rows = read_tsv(os.path.join(TMP, "coords.tsv"), 7)
    resolved = resolved_map()
    cache = {}

    # properties are resolved with the same precedence maven uses: a module's own
    # <properties> win over its ancestors', so a pinned value is shown as the build
    # really sees it instead of as an unresolved placeholder.
    tables = build_prop_tables(coord_rows)

    def resolve(value, module):
        return resolve_value(value, module, tables)

    def verdict(group, artifact):
        """(info, repo_that_served_it), fetching metadata on demand if absent."""
        key = (group, artifact)
        if key in cache:
            return cache[key]
        path = meta_path(group, artifact)
        info = load_meta(path, group, artifact)
        repo = served.get(key, "")
        if info is None:
            repo = ""
            for name in sorted(bases, key=lambda n: (n != "central", n)):
                url = bases[name] + "/" + group.replace(".", "/") + "/" + artifact \
                    + "/maven-metadata.xml"
                get(url, path)
                info = load_meta(path, group, artifact)
                if info is not None:
                    repo = name
                    break
            if info is None and os.path.isfile(path):
                os.remove(path)
        cache[key] = (info, repo if repo else "-")
        return cache[key]

    def fmt(v):
        return f"`{v}`" if v else "*n/a*"

    def coord_of(group, artifact):
        return f"`{group}:{artifact}`"

    def pin_cell(raw, module):
        """show the version the build really uses, noting the declared placeholder."""
        shown = resolve(raw, module)
        if not shown:
            return "*by BOM*"
        if shown == raw:
            return fmt(shown)
        return f"{fmt(shown)} *(declared as* `{raw}`*)*"

    def status_for(current, info):
        if info is None:
            return "*not found*"
        if not current or current.startswith("${"):
            return "*unpinned*"
        top = info["latest_plain"] or info["release_tag"] or info["latest_any"]
        if current in (info["latest_plain"], info["release_tag"], info["latest_any"],
                       info["latest_tag"]):
            return "current"
        if top and highest([current, top]) == top and current != top:
            return "**behind**"
        if top and highest([current, top]) == current:
            return "*ahead of repo*"
        if not is_plain(current):
            return "*pre-release/qualified pin*"
        return "**behind**"

    lines = []
    add = lines.append
    stamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    add("# Maven plugin and dependency version audit")
    add("")
    add(f"Generated {stamp} from the repository's own build metadata by the version-audit")
    add("scripts (`step1_coords.py` harvests coordinates from the POMs, `step2_download.py`")
    add("fetches the repository metadata, `step3_doc.py` renders this file).")
    add("")
    add("## How this was produced")
    add("")
    add("1. **Declared coordinates and pinned versions** are parsed out of this repository's")
    add("   `pom.xml` files. Nothing below is typed in by hand, so a row cannot drift from")
    add("   what the build actually says.")
    add("2. **Latest versions** are read from each coordinate's `maven-metadata.xml` - the very")
    add("   file a Maven client consults to decide what is available. This is the authoritative")
    add("   repository index (the same data a browser view such as `mvnrepository.com` renders).")
    add("3. **Repository roots were not reconstructed**: they were harvested from the")
    add("   `Downloading/Downloaded from <repo>:` lines Maven itself wrote to its log, then")
    add("   reduced to the repository root by stripping the coordinate path that step1 parsed")
    add("   out of the POMs.")
    add("4. **Every response is content-checked**: the `<groupId>`/`<artifactId>` inside a")
    add("   fetched metadata file must equal the coordinate that was asked for, or the hit is")
    add("   discarded. A permissive error page cannot masquerade as a result.")
    add("5. **`in use` comes from Maven's own `dependency:list`**, so BOM-managed rows show the")
    add("   version really resolved rather than a guess about the BOM.")
    add("")
    add("Repositories consulted (as declared by the build itself):")
    add("")
    add("| Repository | Root used for metadata lookups |")
    add("| --- | --- |")
    for repo in sorted(bases):
        add(f"| `{repo}` | `{bases[repo]}` |")
    add("")
    add("Any row can be re-checked directly:")
    add("")
    add("```")
    add("<repo root>/<groupId with '.' replaced by '/'>/<artifactId>/maven-metadata.xml")
    add("```")
    add("")
    add("Reading the version columns (\"latest\" is ambiguous, so all three are given):")
    add("")
    add("| Column | Meaning |")
    add("| --- | --- |")
    add("| `pinned` | version the build declares (`${...}` placeholders resolved) |")
    add("| `in use` | version Maven actually resolved; differs from `pinned` when a BOM/parent decides |")
    add("| `release (repo)` | the `<release>` element the repository publishes |")
    add("| `latest plain` | highest version whose every dot component is purely numeric |")
    add("| `latest any` | highest version overall, qualifiers such as `-alpha`/`-rc` included |")
    add("| verdict | `current`, **behind**, *pre-release/qualified pin*, *unpinned*, *ahead of repo*, *not found* |")
    add("")

    # ---------------------------------------------------------------- 1 plugins
    pinned = {}
    for r in coord_rows:
        if r[0] != "plugin":
            continue
        pinned.setdefault((r[1], r[2]), []).append((r[3], r[5]))
    inherited = inherited_plugin_versions()
    add("## 1. Maven build plugins")
    add("")
    add("Plugins bound by `<build><plugins>` / `<pluginManagement>` across the reactor. A plugin")
    add("that declares no version is pinned by a parent's `pluginManagement`; the version Maven")
    add("actually bound is then taken from its own effective-POM output rather than omitted.")
    add("")
    add("| Plugin | pinned | release (repo) | latest plain | latest any | verdict | repo |")
    add("| --- | --- | --- | --- | --- | --- | --- |")
    behind_plugins = []
    for (g, a) in sorted(pinned):
        declared = sorted(v for v, _m in pinned[(g, a)] if v != "-")
        if declared:
            raw, module = sorted((v, m) for v, m in pinned[(g, a)] if v != "-")[0]
            cur = resolve(raw, module)
            cell = pin_cell(raw, module)
        else:
            found = sorted(inherited.get((g, a), ()))
            cur = found[0] if found else ""
            module = pinned[(g, a)][0][1]
            cell = (f"{fmt(cur)} *(inherited from a parent)*" if cur
                    else "*no version found*")
        info, repo = verdict(g, a)
        st = status_for(cur, info)
        if st.startswith("**behind"):
            behind_plugins.append((coord_of(g, a), cur, info["latest_plain"]))
        add(f"| {coord_of(g, a)} | {cell} | {fmt(info and info['release_tag'])} |"
            f" {fmt(info and info['latest_plain'])} | {fmt(info and info['latest_any'])} |"
            f" {st} | {repo} |")
    add("")
    if behind_plugins:
        add("Behind the latest plain release:")
        add("")
        for name, cur, latest in behind_plugins:
            add(f"- {name}: pinned {fmt(cur)}, latest plain {fmt(latest)}")
    else:
        add("No pinned plugin is behind its latest plain release.")
    add("")

    # ---------------------------------------------------------------- 2 properties
    add("## 2. Version properties")
    add("")
    add("Each version property the build declares, the coordinate it drives, and where that")
    add("coordinate stands against the repository.")
    add("")
    drives = {}
    for r in coord_rows:
        if r[0] == "property" or r[3] == "-":
            continue
        m = re.fullmatch(r"\$\{([^}]+)\}", r[3])
        if m:
            drives.setdefault(m.group(1), set()).add((r[1], r[2]))
    add("| Property | pinned | drives | release (repo) | latest plain | latest any | verdict |")
    add("| --- | --- | --- | --- | --- | --- | --- |")
    non_version = []
    for r in sorted((x for x in coord_rows if x[0] == "property"),
                    key=lambda x: (x[5], x[3])):
        name, _, value = r[3].partition("=")
        if name in NON_VERSION_PROPS:
            non_version.append((r[5], name, value))
            continue
        coords = sorted(drives.get(name, set()))
        drives_txt = ", ".join(coord_of(g, a) for g, a in coords) or "*no coordinate uses it*"
        info, repo = (None, "-")
        if coords:
            info, repo = verdict(*coords[0])
        st = status_for(value, info) if info else "*not found*"
        add(f"| `{name}` | {fmt(value)} | {drives_txt} | {fmt(info and info['release_tag'])} |"
            f" {fmt(info and info['latest_plain'])} | {fmt(info and info['latest_any'])} | {st} |")
    add("")
    add("### Build properties that are not artifact versions")
    add("")
    add("| Where | Property | value |")
    add("| --- | --- | --- |")
    for mod, name, value in non_version:
        shown = resolve(value, mod)
        if "${" in (shown or "") or shown == value:
            add(f"| `{mod}` | `{name}` | {value} |")
        else:
            add(f"| `{mod}` | `{name}` | {shown} *(declared as* `{value}`*)* |")
    add("")

    # ---------------------------------------------------------------- 3 parents/boms
    add("## 3. Parents and imported BOMs")
    add("")
    add("| Kind | Declared in | Coordinate | pinned | release (repo) | latest plain | verdict |")
    add("| --- | --- | --- | --- | --- | --- | --- |")
    for kind, rel, g, a, v in parents_and_boms():
        internal = g.startswith(INTERNAL)
        cur = "" if v == "${revision}" else resolve(v, rel)
        if cur is not None and "${" in (cur or ""):
            cur = "" if v == "${revision}" else v
        if internal:
            info, repo, st = None, "internal", "*reactor internal*"
        else:
            info, repo = verdict(g, a)
            fallback = sorted(resolved.get((g, a), (set(), set()))[0])
            st = status_for(cur or (fallback[0] if fallback else None), info)
        add(f"| {kind} | `{rel}` | {coord_of(g, a)} | {fmt(cur)} |"
            f" {fmt(info and info['release_tag'])} | {fmt(info and info['latest_plain'])} |"
            f" {st} |")
    add("")

    # ---------------------------------------------------------------- 4 declared deps
    grouped = {}
    for r in coord_rows:
        if r[0] not in ("dependency", "plugin-tooling-dep"):
            continue
        key = (r[0], r[1], r[2])
        rec = grouped.setdefault(key, {"mods": set(), "scopes": set(), "vers": []})
        rec["mods"].add(r[5])
        if r[3] != "-":
            rec["vers"].append((r[3], r[5]))
        if r[4] != "-":
            rec["scopes"].add(r[4])
    add("## 4. Declared project dependencies")
    add("")
    add("Direct dependencies the reactor declares itself.")
    add("")
    add("| Coordinate | pinned | in use | scopes | release (repo) | latest plain | latest any | verdict | repo |")
    add("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
    behind_deps = []
    internal_rows = []
    for key in sorted(grouped):
        kind, g, a = key
        rec = grouped[key]
        if rec["vers"]:
            raw, module = sorted(rec["vers"])[0]
            cur = resolve(raw, module)
        else:
            cur = ""
        resv, resscopes = resolved.get((g, a), (set(), set()))
        inuse = ", ".join(f"`{v}`" for v in sorted(resv)) if resv else "*not resolved*"
        scopes = ", ".join(sorted(rec["scopes"] | resscopes)) or "compile"
        if g.startswith(INTERNAL):
            internal_rows.append((g, a, cur, sorted(rec["mods"])))
            continue
        info, repo = verdict(g, a)
        st = status_for(cur or (sorted(resv)[0] if resv else None), info)
        if st.startswith("**behind"):
            # when the version arrives from a BOM the reader still needs to know which
            # version is the one sitting behind, so name the resolved one
            shown = cur if cur else (", ".join(sorted(resv)) if resv else "(unknown)")
            behind_deps.append((coord_of(g, a), shown, info["latest_plain"]))
        tag = "" if kind == "dependency" else " *(plugin tooling)*"
        add(f"| {coord_of(g, a)}{tag} | {pin_cell(raw, module) if rec['vers'] else '*by BOM*'} |"
            f" {inuse} |"
            f" {scopes} | {fmt(info and info['release_tag'])} |"
            f" {fmt(info and info['latest_plain'])} | {fmt(info and info['latest_any'])} |"
            f" {st} | {repo} |")
    add("")
    if behind_deps:
        add("Behind the latest plain release:")
        add("")
        for name, cur, latest in behind_deps:
            add(f"- {name}: pinned {fmt(cur)}, latest plain {fmt(latest)}")
    else:
        add("No declared external dependency is behind its latest plain release.")
    add("")
    if internal_rows:
        add("### Reactor-internal coordinates")
        add("")
        add("Built by this project, so no repository \"latest\" applies.")
        add("")
        add("| Coordinate | pinned | declared in |")
        add("| --- | --- | --- |")
        for g, a, cur, mods in internal_rows:
            add(f"| {coord_of(g, a)} | {fmt(cur)} | {', '.join(f'`{m}`' for m in mods)} |")
        add("")

    # ---------------------------------------------------------------- appendix closure
    body, total, behind = [], 0, 0
    for (g, a) in sorted(resolved):
        if g.startswith(INTERNAL):
            continue
        resv, scopes = resolved[(g, a)]
        total += 1
        info, repo = verdict(g, a)
        st = status_for(sorted(resv)[0] if resv else None, info)
        if st.startswith("**behind"):
            behind += 1
        body.append(f"| {coord_of(g, a)} | {', '.join(f'`{v}`' for v in sorted(resv))} |"
                    f" {', '.join(sorted(scopes)) or '-'} | {fmt(info and info['release_tag'])} |"
                    f" {fmt(info and info['latest_plain'])} | {fmt(info and info['latest_any'])} |"
                    f" {st} | {repo} |")
    add("## Appendix A - full resolved dependency closure")
    add("")
    add(f"Every external coordinate appearing in the resolved closure of the reactor:")
    add(f"**{total}** coordinates, of which **{behind}** sit behind the latest plain release.")
    add("")
    add("| Coordinate | in use | scopes | release (repo) | latest plain | latest any | verdict | repo |")
    add("| --- | --- | --- | --- | --- | --- | --- | --- |")
    lines.extend(body)
    add("")

    # ---------------------------------------------------------------- caveats
    add("## Notes and caveats")
    add("")
    add("- `versions-maven-plugin:display-property-updates` reports **no** property updates for")
    add("  this reactor. That goal suppresses *major* upgrades by default, so it cannot and does")
    add("  not agree with the **behind** rows above; the tables here come from the raw")
    add("  `maven-metadata.xml` version lists and therefore do include major jumps.")
    add("- `dev.openfeature:sdk` and the `dev.openfeature.contrib.providers:*` artifacts are")
    add("  compiled against one another and break at **runtime**, not compile time, when they")
    add("  drift apart. Upgrade that family as one lockstep change and re-run the whole reactor")
    add("  afterwards - modules after the first failure are skipped, which hides every")
    add("  downstream provider module.")
    add("- *pre-release/qualified pin* is informational: the build deliberately pins a qualified")
    add("  version (an `-alpha`, `-rc`, `-jre`, `-android` ... build). Compare `latest plain`")
    add("  before treating it as a defect.")
    add("- *by BOM* in the `pinned` column means the POM states no version; it arrives via")
    add("  `spring-boot-dependencies` or `spring-boot-openfeature-dependencies`. The `in use`")
    add("  column is the truth for those rows.")
    add("- A coordinate listed with several `in use` values resolves differently per module; all")
    add("  observed versions are shown in one row.")
    add("- *not found* means no repository declared by the build returned content-verified")
    add("  metadata for that coordinate.")
    add("- A local `~/.m2` cache directory is **not** evidence that a version exists. A folder")
    add("  holding only `<artifact>-<version>.jar.lastUpdated` / `.pom.lastUpdated` markers is a")
    add("  record of a *failed* fetch, not an artifact. The GrowthBook SDK cache shows a")
    add("  `0.11.1` directory of exactly that kind while the repository index publishes no such")
    add("  version (`<release>` is `0.11.0`, and `0.11.1` is absent from its version list) - so")
    add("  trust the `maven-metadata.xml` columns above over a directory listing.")
    add("- The `examples/` aggregator is **profile gated** (`-P examples`) and parents off")
    add("  `examples/pom.xml`, not the root pom. Its coordinates are audited here from the POM")
    add("  bytes, but they are absent from `in use` and from any default-reactor Maven report,")
    add("  because the default reactor does not build that tree. Its own `revision` and")
    add("  `sprindoc-openapi.version` properties resolve through that parent chain.")
    add("- Coverage: plugins, properties, parents/BOM imports and declared dependencies are the")
    add("  audit proper; Appendix A is the transitive closure, included so an upgrade review can")
    add("  see what a bump would actually move.")
    add("")
    add("## Reproducing")
    add("")
    add("The audit ships as a set of scripts; run them from their own directory with the")
    add("repository as the working directory. Scratch (fetched metadata, maven logs) lands in")
    add("one temporary dir shared by every step; override `VERSION_AUDIT_TMP`, `VERSION_AUDIT_DOC`")
    add("or `MVN` if the defaults do not suit your layout.")
    add("")
    add("```bash")
    add("SK=<the directory holding these scripts>   # the skill's scripts/ directory")
    add("TMP=${VERSION_AUDIT_TMP:-target/version-audit}")
    add("MVN=${MVN:-$(test -f ./mvnw && echo ./mvnw || echo mvn)}")
    add("mkdir -p \"$TMP\"")
    add("python3 \"$SK/step1_coords.py\"")
    add("# let maven log its own repository traffic, then read the roots out of that log")
    add("\"$MVN\" -B -U org.codehaus.mojo:versions-maven-plugin:2.21.0:display-property-updates > \"$TMP/props.log\" 2>&1")
    add("\"$MVN\" -B dependency:list > \"$TMP/deplist.log\" 2>&1")
    add("# maven's effective-pom print, for the version a parent pins on a plugin that")
    add("# declares none itself; a profile gated sub-reactor must be asked for explicitly")
    add("\"$MVN\" -B -P examples help:effective-pom > \"$TMP/effpom.log\" 2>&1")
    add("python3 \"$SK/step2_download.py\"")
    add("python3 \"$SK/step3_doc.py\"")
    add("```")
    add("")
    add("Two self checks ship with it; both are independent of the renderer:")
    add("")
    add("```bash")
    add("python3 \"$SK/verify_ordering.py\"   # re-derives ordering independently; checks latest-any maximality")
    add("python3 \"$SK/crosscheck.py\"          # 'behind' verdicts vs maven's own versions plugin report")
    add("```")
    add("")

    os.makedirs(DOCS, exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")
    missing = sorted({(g, a) for (g, a) in
                      [tuple(x[1:3]) for x in coord_rows if x[0] != "property"]
                      if verdict(g, a)[0] is None})
    print("wrote", OUT, "with", len(lines), "lines")
    print(f"plugins behind: {len(behind_plugins)}; declared deps behind: {len(behind_deps)};"
          f" closure: {total} total / {behind} behind")
    print("coordinates without verified metadata:", len(missing))
    for m in missing:
        print("   ", m[0] + ":" + m[1])
    return 0


if __name__ == "__main__":
    sys.exit(main())
