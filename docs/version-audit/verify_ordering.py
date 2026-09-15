"""independent verification of the version ordering used by step3.

the check deliberately uses a different, brute force method so it does not share the
assumptions of step3's comparator:
  * plain ordering is re-derived with fixed width zero padded numeric tuples
  * maximality of latest_any is checked pairwise against every listed version
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir, os.pardir))
META = os.path.join(ROOT, ".qwen", "tmp", "meta")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import step3_doc as S  # noqa: E402  the module under test


def plain_versions(versions):
    return [v for v in versions if re.fullmatch(r"\d+(\.\d+)*", v)]


def naive_max_plain(versions):
    pool = plain_versions(versions)
    if not pool:
        return None
    width = max(len(v.split(".")) for v in pool)
    keyed = [(tuple(int(x) for x in v.split(".")) + (0,) * (width - len(v.split("."))), v)
             for v in pool]
    return max(keyed)[1]


def all_versions(path):
    root = ET.parse(path).getroot()
    ver = root.find("versioning")
    if ver is None:
        return []
    out = [v.text.strip() for v in ver.findall("versions/version")
           if v.text and v.text.strip()]
    return [v for v in out if v != "SNAPSHOT" and not v.endswith("-SNAPSHOT")]


bad_plain = bad_max = bad_shape = 0
checked = 0
for name in sorted(os.listdir(META)):
    if not name.endswith(".xml"):
        continue
    path = os.path.join(META, name)
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    g, a = root.findtext("groupId"), root.findtext("artifactId")
    versions = all_versions(path)
    if not versions:
        continue
    checked += 1
    info = S.load_meta(path, g, a)
    if info is None:
        print("REJECTED by content check:", name, g, a)
        bad_shape += 1
        continue
    # 1. plain column must agree with an independently computed maximum
    expect = naive_max_plain(versions)
    if info["latest_plain"] != expect:
        bad_plain += 1
        print(f"PLAIN MISMATCH {g}:{a} step3={info['latest_plain']!r} "
              f"independent={expect!r}")
    # 2. latest_any must be maximal: no listed version may outrank it
    top = info["latest_any"]
    for v in versions:
        if S.highest([top, v]) != top:
            bad_max += 1
            print(f"NOT MAXIMAL {g}:{a} top={top!r} beaten by {v!r}")
            break
    # 3. sanity: latest_any must not sit below latest_plain
    if info["latest_plain"] and info["latest_any"]:
        if S.highest([info["latest_any"], info["latest_plain"]]) != info["latest_any"]:
            bad_shape += 1
            print(f"INVERTED {g}:{a} plain={info['latest_plain']} any={info['latest_any']}")

print(f"checked {checked} metadata files; plain mismatches={bad_plain} "
      f"non maximal={bad_max} shape problems={bad_shape}")

# a few hand-checkable cases, printed for the record
CASES = [("org.slf4j", "slf4j-api"), ("org.projectlombok", "lombok"),
         ("org.apache.maven.plugins", "maven-checkstyle-plugin"),
         ("dev.openfeature", "sdk")]
for g, a in CASES:
    path = S.meta_path(g, a)
    info = S.load_meta(path, g, a)
    if info:
        vs = all_versions(path)
        print(f"  {g}:{a} n={len(vs)} release={info['release_tag']!r} "
              f"plain={info['latest_plain']!r} any={info['latest_any']!r} "
              f"independent_plain={naive_max_plain(vs)!r}")
