"""step 1: dump every maven coordinate declared by this project into a tab file.

coordinates are read out of the pom bytes; nothing here is retyped.
columns: kind  group  artifact  version  scope  module  via
"""
import os
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir, os.pardir))
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
U = "{http://maven.apache.org/POM/4.0.0}"
OUT = os.path.join(ROOT, ".qwen", "tmp", "coords.tsv")


def grab(elem, tag):
    node = elem.find(tag, NS)
    if node is None or node.text is None:
        return ""
    return node.text.strip()


def poms():
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames
                       if d not in {".git", "target", "node_modules", ".qwen", ".idea"}]
        if "pom.xml" in filenames:
            yield os.path.join(dirpath, "pom.xml")


def rows():
    out = []
    for path in sorted(poms()):
        rel = os.path.relpath(path, ROOT)
        root = ET.parse(path).getroot()
        props = root.find("m:properties", NS)
        if props is not None:
            for child in props:
                out.append(("property", "", "", child.tag.split("}")[-1] + "="
                            + (child.text or "").strip(), "", rel, ""))
        # elementtree gives no parent links, so build one. whether a <dependency> sits
        # under a <plugin> is what tells a build tooling dependency apart from a project
        # one; without it a plugin's nested <dependencies> is reported as both.
        parents = {child: elem for elem in root.iter() for child in elem}

        def ancestors(elem):
            """tags of every ancestor, all the way to the root."""
            seen = []
            cur = parents.get(elem)
            while cur is not None:
                seen.append(cur.tag)
                cur = parents.get(cur)
            return seen

        def nested_plugin_below(entry, upper):
            """true when a <plugin> sits strictly between entry and its `upper` owner."""
            cur = parents.get(entry)
            while cur is not None and cur is not upper:
                if cur.tag == U + "plugin":
                    return True
                cur = parents.get(cur)
            return False

        for container_tag, entry_tag, kind in (
            ("dependencies", "dependency", "dependency"),
            ("plugins", "plugin", "plugin"),
        ):
            for container in root.iter(U + container_tag):
                for entry in container.iter(U + entry_tag):
                    gid = grab(entry, "m:groupId")
                    aid = grab(entry, "m:artifactId")
                    ver = grab(entry, "m:version")
                    scope = grab(entry, "m:scope")
                    if not gid or not aid:
                        continue
                    if entry_tag == "dependency" and U + "plugin" in ancestors(entry):
                        continue  # reported below as tooling, with its plugin named
                    out.append((kind, gid, aid, ver, scope, rel, ""))

        for plugin in root.iter(U + "plugin"):
            pg = grab(plugin, "m:groupId")
            pa = grab(plugin, "m:artifactId")
            if not pg or not pa:
                continue
            for entry in plugin.iter(U + "dependency"):
                if nested_plugin_below(entry, plugin):
                    continue
                gid = grab(entry, "m:groupId")
                aid = grab(entry, "m:artifactId")
                ver = grab(entry, "m:version")
                if gid and aid:
                    out.append(("plugin-tooling-dep", gid, aid, ver, "", rel,
                               pg + ":" + pa))
    return out


def main():
    with open(OUT, "w", encoding="utf-8") as handle:
        handle.write("kind\tgroup\tartifact\tversion\tscope\tmodule\tvia\n")
        for row in rows():
            handle.write("\t".join(cell if cell else "-" for cell in row) + "\n")
    print("wrote", OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main())
