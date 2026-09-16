"""shared, location-independent paths for the version-audit pipeline.

every path here is overridable by the environment so nothing is welded to a
particular checkout layout, build tool install, or agent harness:

  VERSION_AUDIT_ROOT  repository root (default: nearest ancestor holding pom.xml + .git)
  VERSION_AUDIT_TMP   scratch dir for fetched metadata + maven logs
                      (default: <root>/target/version-audit - ignored by maven builds
                       and skipped by the POM walk, so it never pollutes the tree)
  VERSION_AUDIT_DOC   the audit document to (re)write
                      (default: <root>/docs/dependency-versions.md)
  MVN                 maven command (default: ./mvnw when present, else mvn)

import this module instead of rederiving any of the above in an individual step.
"""
import os


def _repo_root(start):
    d = os.path.abspath(start)
    while True:
        if os.path.isfile(os.path.join(d, "pom.xml")) and os.path.isdir(os.path.join(d, ".git")):
            return d
        parent = os.path.dirname(d)
        if parent == d:
            return os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                                os.pardir, os.pardir, os.pardir, os.pardir))
        d = parent


HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.environ.get("VERSION_AUDIT_ROOT") or _repo_root(HERE)
TMP = os.environ.get("VERSION_AUDIT_TMP") or os.path.join(ROOT, "target", "version-audit")
DOC = os.environ.get("VERSION_AUDIT_DOC") or os.path.join(ROOT, "docs", "dependency-versions.md")

META = os.path.join(TMP, "meta")
COORDS = os.path.join(TMP, "coords.tsv")
MANIFEST = os.path.join(TMP, "manifest.tsv")
BASES = os.path.join(TMP, "bases.tsv")
PROPS_LOG = os.path.join(TMP, "props.log")
DEPLIST_LOG = os.path.join(TMP, "deplist.log")
EFFPOM_LOG = os.path.join(TMP, "effpom.log")
DEPUPD_LOG = os.path.join(TMP, "depupd.log")


def mvn():
    cmd = os.environ.get("MVN")
    if cmd:
        return cmd
    return "./mvnw" if os.path.isfile(os.path.join(ROOT, "mvnw")) else "mvn"


os.makedirs(TMP, exist_ok=True)
os.makedirs(META, exist_ok=True)
