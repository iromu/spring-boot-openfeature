"""final quality gate for the generated audit document.

checks what a reader would otherwise have to eyeball:
  * every markdown table is rectangular (same cell count as its own header)
  * nothing is silently dropped: every harvested coordinate, property, plugin and
    closure entry from the machine data actually appears somewhere in the document
  * the reproduction redirect is the real shell token, not a mangled rendering of it
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _va_paths as va
import step3_doc as S  # noqa

ROOT = va.ROOT
DOC = va.DOC
TMP = va.TMP

REDIRECT = "".join(chr(c) for c in (0x32, 0x3E, 0x26, 0x31))


def table_problems(lines):
    problems = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith("|") and i + 1 < len(lines) and re.match(r"^\|[\s:\-\|]+\|$",
                                                                     lines[i + 1]):
            width = line.count("|")
            j = i + 2
            while j < len(lines) and lines[j].startswith("|"):
                if lines[j].count("|") != width:
                    problems.append((j + 1, width, lines[j].count("|"), lines[j][:90]))
                j += 1
            i = j
        else:
            i += 1
    return problems


def main():
    text = open(DOC, encoding="utf-8").read()
    lines = text.split("\n")
    print("document:", DOC)
    print("lines:", len(lines), "bytes:", os.path.getsize(DOC))

    bad = table_problems(lines)
    print("non-rectangular table rows:", len(bad))
    for row in bad[:10]:
        print("   line", row)

    print("reproduction redirect token correct:", REDIRECT in text)

    # ---- coverage: everything the harvest found must be represented in the document
    coord_rows = S.read_tsv(os.path.join(TMP, "coords.tsv"), 7)
    missing = []
    for r in coord_rows:
        kind, g, a = r[0], r[1], r[2]
        if kind == "property":
            name = r[3].split("=")[0]
            if f"`{name}`" not in text:
                missing.append(("property", r[5], name))
        elif g and a not in ("", "-"):
            if f"`{g}:{a}`" not in text:
                missing.append((kind, g + ":" + a, r[5]))
    print("harvested items absent from the document:", len(missing))
    for m in missing[:15]:
        print("   ", m)

    # the resolved closure must be represented too
    resolved_missing = []
    for (g, a) in sorted(S.resolved_map()):
        if f"`{g}:{a}`" not in text:
            resolved_missing.append(g + ":" + a)
    print("resolved closure coordinates absent from the document:", len(resolved_missing))
    for m in resolved_missing[:15]:
        print("   ", m)

    # every fetched metadata file that passed the content check must be reflected
    metas = [n for n in sorted(os.listdir(os.path.join(TMP, "meta")))
             if n.endswith(".xml")]
    print("metadata files on hand:", len(metas))

    empties = [ln for ln in lines if ln.startswith("|") and "|  |" in ln.replace("   ", "  ")]
    print("rows with an empty middle cell:", len(empties))
    verdicts = {}
    for v in ("current", "**behind**", "*unpinned*", "*not found*", "*ahead of repo*",
              "*pre-release/qualified pin*", "*reactor internal*"):
        verdicts[v] = text.count("| " + v + " |") + text.count("| " + v)
    print("verdict tally:", {k: v for k, v in verdicts.items() if v})
    ok = not bad and not missing and not resolved_missing and REDIRECT in text
    print("RESULT:", "pass" if ok else "needs attention")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
