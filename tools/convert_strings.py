#!/usr/bin/env python3
"""Generate the app's Android string resources from the iOS string catalogs.

Reads the four Localizable.xcstrings catalogs of the iOS app (app, widgets, share
extension, NorthAPI) plus tools/strings-android.json (copy that only Android has),
and writes core/ui/src/main/res/values{,-es,-pt-rPT,-pt-rBR}/strings.xml.

    python3 tools/convert_strings.py [--check] [path/to/north-ios-app/khepri]

- Resource names come from tools/strings-keys.json (iOS key -> name), so a name
  never changes when its English text does. A new key gets a snake-case name
  made from its English text. Entries are never removed, so a key that drops out
  for a run (untranslated, stale) gets its old name back, and no name is reused.
- %@ becomes %N$s and %lld/%d become %N$d. A string with no arguments is literal
  (formatted="false"), as SwiftUI shows it.
- A key iOS has not translated into every locale is left out and listed: iOS
  shows English there, and Android fails the build on a missing translation.
- Lint's UnusedResources, PluralsCandidate, TypographyDashes and Typos are off in
  the generated files: the copy is fixed in the iOS catalogs, not here.
- With --check it writes nothing and exits 1 when the committed files differ
  from what it would generate.
"""

from __future__ import annotations

import json
import re
import sys
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_IOS = ROOT.parent / "north-ios-app/khepri"
CATALOGS = (
    "khepri/Resources/Localizable.xcstrings",
    "KhepriWidgets/Localizable.xcstrings",
    "KhepriShare/Localizable.xcstrings",
    "NorthKit/Sources/NorthAPI/Resources/Localizable.xcstrings",
)
KEYMAP = ROOT / "tools/strings-keys.json"
ANDROID = ROOT / "tools/strings-android.json"
RES = ROOT / "core/ui/src/main/res"
FOLDERS = {"en": "values", "es": "values-es", "pt-PT": "values-pt-rPT", "pt-BR": "values-pt-rBR"}
TRANSLATED = ("es", "pt-PT", "pt-BR")
# CLDR asks for "many" in Spanish and Portuguese (millions); iOS catalogs only carry one/other.
MANY_FROM_OTHER = {"es", "pt-PT", "pt-BR"}
QUANTITIES = ("zero", "one", "two", "few", "many", "other")
MAX_NAME = 40
# The copy is iOS's: lint's plural, dash and spelling hints are fixed in the iOS catalogs, then regenerated.
# Features use a subset of the strings, so most are unused until their screen is ported.
IGNORED_LINT = "UnusedResources,PluralsCandidate,TypographyDashes,Typos"
JAVA_KEYWORDS = set(
    "abstract assert boolean break byte case catch char class const continue default do double else enum extends "
    "false final finally float for goto if implements import instanceof int interface long native new null package "
    "private protected public return short static strictfp super switch synchronized this throw throws transient "
    "true try void volatile while".split()
)

SPEC = re.compile(r"%%|%(?:(\d+)\$)?(@|lld|ld|llu|lu|d|i|u|\.\d+f|f)")
KIND = {"@": "s", "f": "f"}


@dataclass
class Resource:
    """One string (text) or plural (quantities) in one locale."""

    text: str | None = None
    quantities: dict[str, str] | None = None
    formatted: bool = True
    translatable: bool = True


@dataclass
class Result:
    locales: dict[str, dict[str, Resource]]
    keymap: dict[str, str]
    untranslated: list[str] = field(default_factory=list)
    rejected: list[tuple[str, str]] = field(default_factory=list)
    stale: int = 0


class FormatError(ValueError):
    pass


def android_format(value: str) -> tuple[str, set[tuple[int, str]]]:
    """%@ -> %N$s, %lld -> %N$d, numbering unnumbered arguments in order. Returns the text and its arguments."""
    args: set[tuple[int, str]] = set()
    position = 0

    def replace(match: re.Match) -> str:
        nonlocal position
        if match.group(0) == "%%":
            return "%%"
        conversion = match.group(2)
        kind = KIND.get(conversion, conversion if conversion.endswith("f") else "d")
        if match.group(1):
            index = int(match.group(1))
        else:
            position += 1
            index = position
        args.add((index, kind))
        return f"%{index}${kind}"

    text = SPEC.sub(replace, value)
    if "%" in SPEC.sub("", value):
        raise FormatError(f"stray % in {value!r}")
    return text, args


def has_arguments(value: str) -> bool:
    return any(match.group(0) != "%%" for match in SPEC.finditer(value))


def values_of(localization: dict) -> str | dict[str, str] | None:
    if "stringUnit" in localization:
        return localization["stringUnit"].get("value")
    plural = localization.get("variations", {}).get("plural")
    if plural:
        return {q: v["stringUnit"]["value"] for q, v in plural.items() if "stringUnit" in v}
    return None


def complete(item: dict) -> bool:
    """Translated into every locale, or marked not to translate."""
    return item.get("shouldTranslate") is False or all(loc in item.get("localizations", {}) for loc in TRANSLATED)


def rank(item: dict) -> tuple[bool, bool]:
    """Which copy of a key shared between catalogs to use: a live one first, then a translated one."""
    return item.get("extractionState") != "stale", complete(item)


# In a string without arguments a % is literal, and must read as one: followed by a space, punctuation or the end.
LITERAL_PERCENT = re.compile(r"%(?=[\s.,;:!?)\]}'\"]|$)")


def make_name(text: str, taken: set[str]) -> str:
    ascii_text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode()
    ascii_text = SPEC.sub(" x ", ascii_text).replace("'", "").replace("\u2019", "")
    words = [w for w in re.split(r"[^a-z0-9]+", ascii_text.lower()) if w]
    name = ""
    for word in words:
        candidate = f"{name}_{word}" if name else word
        if len(candidate) > MAX_NAME:
            break
        name = candidate
    if not name:
        name = words[0][:MAX_NAME] if words else "empty"
    if name[0].isdigit():
        name = f"text_{name}"
    if name in JAVA_KEYWORDS:
        name = f"{name}_text"
    unique, n = name, 2
    while unique in taken:
        unique, n = f"{name}_{n}", n + 1
    return unique


def literal_text(text: str) -> str:
    if LITERAL_PERCENT.sub("", text).count("%"):
        raise FormatError(f"unsupported % in {text!r}")
    return text


def resource(value, literal: bool) -> tuple[Resource, set]:
    if isinstance(value, dict):
        if "other" not in value:
            raise FormatError("plural without other")
        quantities, found = {}, set()
        for quantity, text in value.items():
            converted, args = (literal_text(text), set()) if literal else android_format(text)
            quantities[quantity] = converted
            found |= args
        return Resource(quantities=quantities, formatted=not literal), found
    if not value:
        raise FormatError("empty value")
    if literal:
        args = {(0, m.group(0)) for m in SPEC.finditer(value) if m.group(0) != "%%"}
        return Resource(text=literal_text(value), formatted="%" not in value), args
    text, args = android_format(value)
    return Resource(text=text), args


def build(catalogs: list[dict], keymap: dict[str, str], android: dict[str, dict[str, str]]) -> Result:
    result = Result(locales={loc: {} for loc in FOLDERS}, keymap={})
    clash = sorted(set(android) & set(keymap.values()))
    if clash:
        raise ValueError(f"tools/strings-android.json reuses names the key map gives iOS keys: {clash}")
    taken = set(android) | set(keymap.values())
    for name, texts in android.items():
        if set(texts) == {"en"}:
            result.locales["en"][name] = Resource(text=texts["en"], formatted="%" not in texts["en"], translatable=False)
            continue
        missing = [loc for loc in FOLDERS if not texts.get(loc)]
        tokens = {loc: sorted(re.findall(r"%\d+\$[sdf]", texts.get(loc, ""))) for loc in FOLDERS}
        if missing or len({tuple(t) for t in tokens.values()}) > 1:
            raise ValueError(f"tools/strings-android.json {name}: missing {missing} or arguments differ {tokens}")
        for loc in FOLDERS:
            formatted = has_arguments(texts["en"]) or "%" not in texts["en"]
            result.locales[loc][name] = Resource(text=texts[loc], formatted=formatted)
    chosen: dict[str, dict] = {}
    for catalog in catalogs:
        for key, item in catalog["strings"].items():
            if key not in chosen or rank(item) > rank(chosen[key]):
                chosen[key] = item
    for key, item in chosen.items():
        if not key.strip():
            continue
        if item.get("extractionState") == "stale":
            result.stale += 1
            continue
        localizations = item.get("localizations", {})
        translatable = item.get("shouldTranslate") is not False
        english = values_of(localizations["en"]) if "en" in localizations else None
        english = english or key
        if not complete(item):
            result.untranslated.append(key)
            continue
        plural_en = english if isinstance(english, dict) else None
        sample = (plural_en.get("other") or next(iter(plural_en.values()))) if plural_en else english
        literal = not has_arguments(sample)
        try:
            converted = {}
            en_resource, en_args = resource(english, literal)
            converted["en"] = en_resource
            for loc in TRANSLATED if translatable else ():
                value = values_of(localizations[loc])
                if value is None:
                    raise FormatError(f"{loc} has no value")
                if isinstance(value, dict) != isinstance(english, dict):
                    raise FormatError(f"{loc} is {'a plural' if isinstance(value, dict) else 'a string'}, English is not")
                res, args = resource(value, literal)
                if args != en_args:
                    raise FormatError(f"{loc} arguments {sorted(args)} differ from English {sorted(en_args)}")
                converted[loc] = res
        except FormatError as error:
            result.rejected.append((key, str(error)))
            continue
        name = keymap.get(key)
        if name is None or name in result.keymap.values():
            name = make_name(sample if isinstance(sample, str) else key, taken)
        taken.add(name)
        result.keymap[key] = name
        for loc, res in converted.items():
            res.translatable = translatable
            if res.quantities is not None and loc in MANY_FROM_OTHER and "many" not in res.quantities:
                res.quantities["many"] = res.quantities["other"]
            result.locales[loc][name] = res
    result.keymap = {**keymap, **result.keymap}
    return result


def escape(text: str) -> str:
    text = text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")
    text = text.replace("'", "\\'").replace('"', '\\"')
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    if text[:1] in ("@", "?"):
        text = "\\" + text
    if text and not text.strip(" "):
        return "\\u0020" * len(text)
    if text != text.strip(" "):
        stripped = text.strip(" ")
        lead = len(text) - len(text.lstrip(" "))
        trail = len(text) - len(text.rstrip(" "))
        text = "\\u0020" * lead + stripped + "\\u0020" * trail
    return text


def render(resources: dict[str, Resource]) -> str:
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!-- Generated by tools/convert_strings.py from the iOS string catalogs. Do not edit. -->",
        f'<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="{IGNORED_LINT}">',
    ]
    for name in sorted(resources):
        res = resources[name]
        attrs = f' name="{name}"'
        if not res.translatable:
            attrs += ' translatable="false"'
        if not res.formatted:
            attrs += ' formatted="false"'
        if res.quantities is None:
            lines.append(f"    <string{attrs}>{escape(res.text)}</string>")
        else:
            lines.append(f"    <plurals{attrs}>")
            for quantity in QUANTITIES:
                if quantity in res.quantities:
                    lines.append(f'        <item quantity="{quantity}">{escape(res.quantities[quantity])}</item>')
            lines.append("    </plurals>")
    lines.append("</resources>")
    return "\n".join(lines) + "\n"


def outputs(result: Result) -> dict[Path, str]:
    files = {RES / folder / "strings.xml": render(result.locales[loc]) for loc, folder in FOLDERS.items()}
    files[KEYMAP] = json.dumps(dict(sorted(result.keymap.items())), ensure_ascii=False, indent=2) + "\n"
    return files


def main() -> int:
    args = [a for a in sys.argv[1:] if a != "--check"]
    check = "--check" in sys.argv[1:]
    ios = Path(args[0]) if args else DEFAULT_IOS
    catalogs = [json.loads((ios / path).read_text(encoding="utf-8")) for path in CATALOGS]
    keymap = json.loads(KEYMAP.read_text(encoding="utf-8")) if KEYMAP.exists() else {}
    android = json.loads(ANDROID.read_text(encoding="utf-8")) if ANDROID.exists() else {}
    result = build(catalogs, keymap, android)
    for key, reason in result.rejected:
        print(f"rejected {key!r}: {reason}", file=sys.stderr)
    if result.untranslated:
        print(f"left out, not translated on iOS ({len(result.untranslated)}):", file=sys.stderr)
        for key in result.untranslated:
            print(f"  {key!r}", file=sys.stderr)
    files = outputs(result)
    if check:
        stale = [path for path, text in files.items() if not path.exists() or path.read_text(encoding="utf-8") != text]
        for path in stale:
            print(f"out of date: {path.relative_to(ROOT)}", file=sys.stderr)
        return 1 if stale or result.rejected else 0
    if result.rejected:
        print("nothing written: fix the rejected keys in the iOS catalogs first", file=sys.stderr)
        return 1
    for path, text in files.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
    counts = {loc: len(res) for loc, res in result.locales.items()}
    print(f"wrote {counts} ({result.stale} stale keys skipped)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
