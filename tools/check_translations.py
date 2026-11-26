#!/usr/bin/env python3
"""Fail when a string resource is missing from, or extra in, any locale of any module.

Every `src/main/res` folder in the repo is checked: each name in `values/` (strings,
plurals, string arrays; not translatable="false") must be in values-es, values-pt-rPT
and values-pt-rBR, and those must hold nothing `values/` does not. Android Lint's
MissingTranslation covers the first half per module; this gives one readable list.

    python3 tools/check_translations.py
"""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LOCALES = ("values-es", "values-pt-rPT", "values-pt-rBR")
KINDS = ("string", "plurals", "string-array")


def names(folder: Path) -> tuple[set[str], set[str]]:
    """Translatable and untranslatable resource names in one values folder."""
    translatable, fixed = set(), set()
    for path in sorted(folder.glob("*.xml")):
        for element in ET.parse(path).getroot():
            if element.tag in KINDS:
                target = fixed if element.get("translatable") == "false" else translatable
                target.add(element.get("name"))
    return translatable, fixed


def problems(root: Path) -> list[str]:
    found = []
    for res in sorted(root.glob("**/src/main/res")):
        if "build" in res.relative_to(root).parts or not (res / "values").is_dir():
            continue
        where = res.relative_to(root).as_posix()
        default, fixed = names(res / "values")
        for locale in LOCALES:
            folder = res / locale
            present = names(folder)[0] if folder.is_dir() else set()
            missing = sorted(default - present)
            extra = sorted(present - default - fixed)
            if missing:
                found.append(f"{where} {locale}: missing {', '.join(missing)}")
            if extra:
                found.append(f"{where} {locale}: not in values: {', '.join(extra)}")
    return found


def main() -> int:
    found = problems(ROOT)
    for line in found:
        print(line, file=sys.stderr)
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main())
