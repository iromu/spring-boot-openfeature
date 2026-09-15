"""cross check the audit's 'behind' verdicts against maven's own versions plugin report.

the versions plugin is an independent implementation that resolves available versions
from the same repository metadata, so agreement is meaningful corroboration and any
disagreement is worth reading rather than waving away.
"""
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir, os.pardir))
TMP = os.path.join(ROOT, ".qwen", "tmp")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import step3_doc as S  # noqa

LINE = re.compile(r"^\[INFO\]\s+([\w.\-]+):([\w.\-]+)\s+\.{2,}\s+([\w.\-+]+)"
                  r"\s+->\s+([\w.\-+]+)")


def parse(path):
    out = {}
    if not os.path.isfile(path):
        return out
    for line in open(path, encoding="utf-8", errors="replace"):
        m = LINE.match(line.rstrip())
        if not m:
            continue
        g, a, old, new = m.groups()
        rec = out.setdefault((g, a), {"old": set(), "new": set()})
        rec["old"].add(old)
        rec["new"].add(new)
    return out


def main():
    dep = parse(os.path.join(TMP, "depupd.log"))
    print(f"versions plugin reports {len(dep)} dependency coordinates with an available update")

    # what the audit pins, resolved with the very helper the document uses, so the
    # two cannot drift apart over time
    coord_rows = S.read_tsv(os.path.join(TMP, "coords.tsv"), 7)
    tables = S.build_prop_tables(coord_rows)

    def resolve(value, module):
        return S.resolve_value(value, module, tables)

    pins = {}
    for r in coord_rows:
        if r[0] not in ("dependency", "plugin-tooling-dep", "plugin"):
            continue
        if r[1].startswith(S.INTERNAL) or r[3] == "-":
            continue
        pins.setdefault((r[1], r[2]), set()).add(resolve(r[3], r[5]))

    declared = set(pins)
    agree = miss = extra = 0
    for key in sorted(declared):
        g, a = key
        info = S.load_meta(S.meta_path(g, a), g, a)
        if info is None:
            continue
        top = info["latest_plain"] or info["release_tag"] or info["latest_any"]
        if top is None:
            continue
        pinned = {v for v in pins[key] if v}
        # behind == some pinned version is strictly below the repository top
        audit_says_behind = any(p != top and S.highest([p, top]) == top for p in pinned)
        plugin_says = key in dep
        if plugin_says and audit_says_behind:
            agree += 1
        elif plugin_says and not audit_says_behind:
            extra += 1
            print(f"  ONLY IN VERSIONS PLUGIN {g}:{a} pinned={sorted(pinned)} "
                  f"audit_top={top!r} plugin_new={sorted(dep[key]['new'])}")
        elif not plugin_says and audit_says_behind:
            miss += 1
            print(f"  ONLY IN AUDIT {g}:{a} pinned={sorted(pinned)} audit_top={top!r}")
    print(f"declared coordinates examined: {len(declared)}; agreement on 'behind': {agree};"
          f" only-in-audit: {miss}; only-in-plugin: {extra}")

    # version value agreement, where both name a target
    diffs = 0
    for key in sorted(set(dep) & declared):
        info = S.load_meta(S.meta_path(*key), *key)
        if info is None:
            continue
        for new in dep[key]["new"]:
            top = info["latest_any"]
            if top and S.highest([new, top]) != top:
                diffs += 1
                print(f"  PLUGIN TARGET OUTRANKS AUDIT TOP {key[0]}:{key[1]} "
                      f"plugin_new={new!r} audit_latest_any={top!r}")
    print(f"plugin targets that outrank the audit's latest_any: {diffs} (expect 0)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
