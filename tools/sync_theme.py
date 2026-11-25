#!/usr/bin/env python3
"""Generate KhepriColors.kt from the web app's theme tokens.

Reads the same source as the iOS script (K/scripts/sync-theme.py): every colour
is OKLCH in north-web-app/src/lib/styles/app.css, under :root (light) and .dark.
Each token becomes a Display P3 colour, the same values the iOS colour sets
hold, so both apps draw the same colour. Tokens iOS pins keep the iOS value.

    python3 tools/sync_theme.py [--check] [path/to/app.css] [path/to/Theme.xcassets]

With --check it writes nothing and exits 1 when KhepriColors.kt differs from what
it would generate (a hand edit, or a web theme change not yet synced).
"""

import json
import math
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_CSS = ROOT.parent / "north-web-app/src/lib/styles/app.css"
DEFAULT_IOS = ROOT.parent / "north-ios-app/khepri/NorthKit/Sources/NorthKit/Resources/Theme.xcassets"
OUT = ROOT / "core/designsystem/src/main/kotlin/com/fernandocorreia/khepri/core/designsystem/theme/KhepriColors.kt"
PINNED = {"north-body-idle", "north-heat-low", "north-body-shell"}

TOKENS = {
    "background": "Background",
    "foreground": "Foreground",
    "card": "Card",
    "card-foreground": "CardForeground",
    "primary": "Primary",
    "primary-foreground": "PrimaryForeground",
    "secondary": "Secondary",
    "secondary-foreground": "SecondaryForeground",
    "muted": "Muted",
    "muted-foreground": "MutedForeground",
    "destructive": "Destructive",
    "border": "Border",
    "input": "Input",
    "ring": "Ring",
    "north-signal": "Signal",
    "north-agent": "Agent",
    "north-ember": "Ember",
    "north-surface": "Surface",
    "north-hairline": "Hairline",
    "north-sport-run": "SportRun",
    "north-sport-ride": "SportRide",
    "north-sport-swim": "SportSwim",
    "north-sport-walk": "SportWalk",
    "north-sport-strength": "SportStrength",
    "north-sport-other": "SportOther",
    "north-day-water": "DayWater",
    "north-day-food": "DayFood",
    "north-day-protein": "DayProtein",
    "north-day-carb": "DayCarb",
    "north-day-fat": "DayFat",
    "north-day-move": "DayMove",
    "north-day-exercise": "DayExercise",
    "north-day-stand": "DayStand",
    "north-day-sleep": "DaySleep",
    "north-day-caffeine": "DayCaffeine",
    "north-day-sun": "DaySun",
    "north-day-screen": "DayScreen",
    "north-day-now": "DayNow",
    "north-body-idle": "BodyIdle",
    "north-body-shell": "BodyShell",
    "north-body-core": "BodyCore",
    "north-heat-low": "HeatLow",
    "north-body-base": "BodyBase",
    "north-sore-low": "SoreLow",
    "north-sore-high": "SoreHigh",
}

OKLCH = re.compile(r"oklch\(\s*([\d.]+)\s+([\d.]+)\s+([\d.]+)\s*(?:/\s*([\d.]+)%)?\s*\)")
VAR = re.compile(r"var\(--([\w-]+)\)")


def block(css: str, selector: str) -> dict[str, str]:
    match = re.search(re.escape(selector) + r"\s*\{(.*?)\n\}", css, re.S)
    if not match:
        sys.exit(f"selector {selector!r} not found")
    body = re.sub(r"/\*.*?\*/", "", match.group(1), flags=re.S)
    return dict(re.findall(r"--([\w-]+):\s*([^;]+);", body))


def resolve(name: str, tokens: dict[str, str]) -> str:
    value = tokens[name]
    ref = VAR.fullmatch(value.strip())
    return resolve(ref.group(1), tokens) if ref else value


def oklch_to_p3(value: str) -> tuple[float, float, float, float]:
    m = OKLCH.fullmatch(value.strip())
    if not m:
        sys.exit(f"not an oklch() value: {value}")
    l, c, h = float(m.group(1)), float(m.group(2)), math.radians(float(m.group(3)))
    alpha = float(m.group(4)) / 100 if m.group(4) else 1.0
    a, b = c * math.cos(h), c * math.sin(h)
    l_ = (l + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m_ = (l - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s_ = (l - 0.0894841775 * a - 1.2914855480 * b) ** 3
    x = 1.2270138511 * l_ - 0.5577999807 * m_ + 0.2812561490 * s_
    y = -0.0405801784 * l_ + 1.1122568696 * m_ - 0.0716766787 * s_
    z = -0.0763812845 * l_ - 0.4214819784 * m_ + 1.5861632204 * s_
    r = 2.4934969119 * x - 0.9313836179 * y - 0.4027107845 * z
    g = -0.8294889696 * x + 1.7626640603 * y + 0.0236246858 * z
    bl = 0.0358458302 * x - 0.0761723893 * y + 0.9568845240 * z

    def encode(v: float) -> float:
        v = min(max(v, 0.0), 1.0)
        return 12.92 * v if v <= 0.0031308 else 1.055 * v ** (1 / 2.4) - 0.055

    return encode(r), encode(g), encode(bl), alpha


def ios_colour(xcassets: Path, name: str) -> tuple[tuple, tuple]:
    contents = json.loads((xcassets / f"{name}.colorset" / "Contents.json").read_text())
    pairs = []
    for entry in contents["colors"]:
        comp = entry["color"]["components"]
        pairs.append(tuple(float(comp[k]) for k in ("red", "green", "blue", "alpha")))
    return pairs[0], pairs[1]


def kotlin_value(rgba: tuple) -> str:
    r, g, b, a = rgba
    args = f"{r:.4f}f, {g:.4f}f, {b:.4f}f"
    return f"p3({args})" if a >= 1 else f"p3({args}, alpha = {a:.3f}f)"


def property_name(name: str) -> str:
    return name[0].lower() + name[1:]


def main() -> None:
    args = [a for a in sys.argv[1:] if a != "--check"]
    check = "--check" in sys.argv[1:]
    css_path = Path(args[0]) if len(args) > 0 else DEFAULT_CSS
    ios_path = Path(args[1]) if len(args) > 1 else DEFAULT_IOS
    css = css_path.read_text()
    light, dark = block(css, ":root"), block(css, ".dark")

    values = {}
    for token, name in TOKENS.items():
        if token in light and token in dark and token not in PINNED:
            values[name] = (oklch_to_p3(resolve(token, light)), oklch_to_p3(resolve(token, dark)))
        else:
            reason = "pinned" if token in PINNED else "no longer in the web CSS"
            print(f"kept {name} from iOS: --{token} is {reason}", file=sys.stderr)
            values[name] = ios_colour(ios_path, name)

    lines = [
        "@file:Suppress(\"MagicNumber\", \"LongParameterList\")",
        "",
        "package com.fernandocorreia.khepri.core.designsystem.theme",
        "",
        "import androidx.compose.runtime.Immutable",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.ui.graphics.colorspace.ColorSpaces",
        "",
        "/**",
        " * The app's colour tokens in Display P3, generated by `tools/sync_theme.py` from the web theme. Do not edit by hand;",
        " * change the web CSS and run the script.",
        " */",
        "@Immutable",
        "public data class KhepriColors(",
    ]
    lines += [f"    val {property_name(n)}: Color," for n in values]
    lines += [") {", "    public companion object {"]
    for label, index in (("Light", 0), ("Dark", 1)):
        lines.append(f"        public val {label}: KhepriColors =")
        lines.append("            KhepriColors(")
        lines += [f"                {property_name(n)} = {kotlin_value(v[index])}," for n, v in values.items()]
        lines.append("            )")
        if label == "Light":
            lines.append("")
    lines += [
        "    }",
        "}",
        "",
        "private fun p3(red: Float, green: Float, blue: Float, alpha: Float = 1f): Color =",
        "    Color(red, green, blue, alpha, ColorSpaces.DisplayP3)",
        "",
    ]
    generated = "\n".join(lines)
    if check:
        current = OUT.read_text() if OUT.exists() else ""
        if current != generated:
            sys.exit(f"{OUT.relative_to(ROOT)} is out of date: run tools/sync_theme.py")
        print("KhepriColors.kt is up to date")
        return
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(generated)
    print(f"wrote {len(values)} tokens from {css_path.name} to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
