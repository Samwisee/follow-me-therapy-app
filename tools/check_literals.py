"""Check that the app's UI code takes its design from the tokens.

    python3 tools/check_literals.py

Reports, in designsystem/ and composeApp/ (generated token and icon files
excepted):
  colour   Color(0x…), Color.Black/White/Red… → FmtColors, Fmt…Tokens
  size     a number in dp or sp (0.dp is fine) → FmtSpace, FmtSize, FmtBorder, Fmt…Tokens, FmtTheme.metrics
Exits non-zero when something is found.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SKIP = {"FmtTokens.kt", "FmtIcons.kt", "FmtLetters.kt"}
COLOUR = re.compile(r"\bColor\(0x[0-9A-Fa-f]+\)|\bColor\.(Black|White|Red|Green|Blue|Gray|Grey|Yellow|Cyan|Magenta|DarkGray|LightGray)\b")
SIZE = re.compile(r"(?<![\w.])(\d+(?:\.\d+)?f?)\.(dp|sp)\b")

problems = 0
for f in sorted(list((ROOT / "designsystem/src").rglob("*.kt")) + list((ROOT / "composeApp/src").rglob("*.kt"))):
    if f.name in SKIP:
        continue
    for n, line in enumerate(f.read_text().split("\n"), 1):
        code = re.sub(r"//.*$", "", line)
        code = re.sub(r'"(?:[^"\\]|\\.)*"', '""', code)
        for m in COLOUR.finditer(code):
            problems += 1
            print(f"{f.relative_to(ROOT)}:{n}: colour {m.group(0)} → FmtColors or a component token")
        for m in SIZE.finditer(code):
            if float(m.group(1).rstrip("f")) == 0:
                continue
            problems += 1
            print(f"{f.relative_to(ROOT)}:{n}: size {m.group(0)} → a space, size or component token")
print(f"{problems} hand-written value(s)" if problems else "The app reads everything from the design tokens.")
sys.exit(1 if problems else 0)
