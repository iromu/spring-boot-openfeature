"""hardened XML reading for the version-audit steps.

POMs and maven-metadata.xml files arrive from whichever repository mirror answers,
so they are parsed as untrusted input. defusedxml refuses DTDs, entity
declarations, and external entities outright, which closes both the
entity-expansion and the quadratic-expansion variants, and this module is the
single place that decision is made.

There is deliberately no stdlib fallback: a missing dependency fails loudly at
import rather than silently downgrading every step's parser. Declare it in
requirements.txt alongside the scripts that import this module.
"""
#  Copyright 2025-2026 the original author or authors.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

from defusedxml.ElementTree import ParseError as _DefusedParseError, fromstring
from defusedxml.common import DefusedXmlException


class ParseError(ValueError):
    """Raised for malformed XML and for XML that was refused as unsafe.

    A single error type keeps the callers simple: a document that cannot be read
    safely and a document that is not well-formed are both "not usable here",
    which is how every call site treats them.
    """


def root(path):
    """Return the document's root element, refusing DTDs and entity declarations."""
    with open(path, "rb") as handle:
        raw = handle.read()
    try:
        return fromstring(raw, forbid_dtd=True, forbid_entities=True, forbid_external=True)
    except (_DefusedParseError, DefusedXmlException) as exc:
        raise ParseError(f"{path}: {exc}") from exc


__all__ = ["ParseError", "root"]
