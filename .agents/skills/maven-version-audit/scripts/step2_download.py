"""step 2 (multi repo): fetch maven-metadata.xml for every external coordinate.

no url is hand retyped:
  * repository base urls are harvested from maven's own "from <repo>: <url>" log lines
    in props.log, then reduced to the repository root by stripping the coordinate path
    ("<group with slashes>/<artifact>/maven-metadata.xml") whose group/artifact are the
    very strings parsed out of the poms by step1. the result is re-fetched to prove it
    really serves a <metadata body.
  * every coordinate is then tried against every declared repository, so artifacts that
    live only on a secondary repo (the growthbook pair on jitpack) still resolve.
"""
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

import _va_paths as va

ROOT = va.ROOT
TMP = va.TMP
META = va.META
COORDS = va.COORDS
MANIFEST = va.MANIFEST
BASES = va.BASES
INTERNAL = "org.iromu.openfeature"
TAIL = "/maven-metadata.xml"


def get(url, dest, timeout=45):
    proc = subprocess.run(
        ["curl", "-s", "-s", "-L", "-f", "--max-time", str(timeout),
         "--user-agent", "qwen-version-scan/1.0", "-o", dest, "-w", "%{http_code}", url],
        capture_output=True, text=True)
    return proc.returncode, (proc.stdout or "").strip()


def is_metadata(path):
    if not os.path.isfile(path):
        return False
    with open(path, "rb") as handle:
        return b"<metadata" in handle.read(400)


def pom_coordinates():
    """(group, artifact) pairs exactly as parsed from the pom bytes by step1."""
    pairs = set()
    with open(COORDS, encoding="utf-8") as handle:
        next(handle)
        for line in handle:
            cells = line.rstrip("\n").split("\t")
            if len(cells) < 7:
                continue
            kind, group, artifact = cells[0], cells[1], cells[2]
            if kind == "property" or group in ("", "-") or artifact in ("", "-"):
                continue
            pairs.add((group, artifact))
    return pairs


def logged_repo_urls():
    log = os.path.join(TMP, "props.log")
    with open(log, encoding="utf-8", errors="replace") as handle:
        data = handle.read()
    out = []
    for repo, url in re.findall(r"from ([\w.\-]+):\s+(https://\S+/maven-metadata\.xml)", data):
        if (repo, url) not in out:
            out.append((repo, url))
    return out


def derive_bases(all_pairs):
    """repo -> repository root, obtained by stripping the parsed coordinate path
    from a url that maven itself logged for that repo.

    reachability is deliberately not proven here: a repo may legitimately 404 the
    particular artifact maven happened to log while still serving others (jitpack is
    a github build service, so central-only artifacts 404 there). a repo therefore
    earns its place by actually returning content-verified metadata during the sweep.
    """
    bases = {}
    for repo, url in logged_repo_urls():
        for group, artifact in all_pairs:
            suffix = "/" + group.replace(".", "/") + "/" + artifact + TAIL
            if url.endswith(suffix):
                base = url[: -len(suffix)]
                bases.setdefault(repo, base)
                print(f"  repo {repo!r}: root = {base!r}  "
                      f"(stripped coordinate path of {group}:{artifact})")
                break
        else:
            print(f"  !! repo {repo!r}: no logged url matched a pom coordinate")
    return bases


def content_matches(path, group, artifact):
    """the bytes returned must describe the coordinate we asked for."""
    if not is_metadata(path):
        return False
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        return False
    got_group = root.findtext("groupId")
    got_artifact = root.findtext("artifactId")
    return got_group == group and got_artifact == artifact


def main():
    os.makedirs(META, exist_ok=True)
    all_pairs = pom_coordinates()
    print("== deriving repository roots from maven's own log lines ==")
    bases = derive_bases(all_pairs)
    if not bases:
        print("FAILED: could not prove a single repository root")
        return 1
    with open(BASES, "w", encoding="utf-8") as handle:
        for repo in sorted(bases):
            handle.write(repo + "\t" + bases[repo] + "\n")

    order = [r for r in ("central",) if r in bases] + sorted(set(bases) - set(["central"]))
    external = sorted(p for p in all_pairs if not p[0].startswith(INTERNAL))
    print(f"== fetching {len(external)} external coordinates over repos {order} ==")
    ok = 0
    hits_by_repo = {}
    rows = []
    for group, artifact in external:
        gpath = group.replace(".", "/")
        dest = os.path.join(META, gpath.replace("/", "_") + "__" + artifact + ".xml")
        hit, code, note = "-", "", "missing"
        for repo in order:
            url = bases[repo] + "/" + gpath + "/" + artifact + TAIL
            code, status = get(url, dest)
            if code == 0 and content_matches(dest, group, artifact):
                hit, note = repo, "ok"
                ok += 1
                hits_by_repo[repo] = hits_by_repo.get(repo, 0) + 1
                break
            if code == 0:
                note = "wrong-content"
        if hit == "-":
            if os.path.isfile(dest):
                os.remove(dest)
        rows.append((group, artifact, str(code), hit, os.path.basename(dest), note))
        print(f"  {'ok ' if note == 'ok' else '!! '}{group}:{artifact} [{hit}] {code}")
    with open(MANIFEST, "w", encoding="utf-8") as handle:
        handle.write("group\tartifact\thttp\trepo\tfile\tnote\n")
        for row in rows:
            handle.write("\t".join(row) + "\n")
    print(f"resolved {ok}/{len(external)}")
    for repo in order:
        print(f"  {repo}: served {hits_by_repo.get(repo, 0)} coordinates")
    return 0 if ok == len(external) else 2


if __name__ == "__main__":
    sys.exit(main())
