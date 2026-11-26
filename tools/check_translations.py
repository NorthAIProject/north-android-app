#!/usr/bin/env python3
"""Fail when a string resource is missing from, extra in, or different in kind in any locale of any module.

Every `src/<source set>/res` folder in the repo is checked: each name in `values/`
(strings, plurals, string arrays; not translatable="false") must be in values-es,
values-pt-rPT and values-pt-rBR as the same kind, not empty, and every plural must
have `other`. The locales must hold nothing `values/` does not. Android Lint's
MissingTranslation covers part of this per module; this gives one readable list.

    python3 tools/check_translations.py
"""

from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LOCALES = ("values-es", "values-pt-rPT", "values-pt-rBR")
KINDS = ("string", "plurals", "string-array")


def resources(folder: Path) -> tuple[dict[str, str], set[str], list[str]]:
    """Translatable names with their kind, untranslatable names, and empty or incomplete resources."""
    kinds, fixed, broken = {}, set(), []
    for path in sorted(folder.glob("*.xml")):
        for element in ET.parse(path).getroot():
            if element.tag not in KINDS:
                continue
            name = element.get("name")
            if element.get("translatable") == "false":
                fixed.add(name)
                continue
            kinds[name] = element.tag
            if element.tag == "string" and not "".join(element.itertext()).strip():
                broken.append(f"{name} is empty")
            if element.tag == "plurals" and "other" not in {item.get("quantity") for item in element}:
                broken.append(f"{name} has no other")
    return kinds, fixed, broken


def problems(root: Path) -> list[str]:
    found = []
    for res in sorted(root.glob("**/src/*/res")):
        if "build" in res.relative_to(root).parts or not (res / "values").is_dir():
            continue
        where = res.relative_to(root).as_posix()
        default, fixed, broken = resources(res / "values")
        found += [f"{where} values: {line}" for line in broken]
        for locale in LOCALES:
            folder = res / locale
            present, _, broken = resources(folder) if folder.is_dir() else ({}, set(), [])
            missing = sorted(set(default) - set(present))
            extra = sorted(set(present) - set(default) - fixed)
            kind = sorted(n for n in set(default) & set(present) if default[n] != present[n])
            if missing:
                found.append(f"{where} {locale}: missing {', '.join(missing)}")
            if extra:
                found.append(f"{where} {locale}: not in values: {', '.join(extra)}")
            if kind:
                found.append(f"{where} {locale}: different kind: {', '.join(kind)}")
            found += [f"{where} {locale}: {line}" for line in broken]
    return found


def main() -> int:
    found = problems(ROOT)
    for line in found:
        print(line, file=sys.stderr)
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main())
