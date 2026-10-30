"""Check that every product implements the design system the same way.

    python3 tools/check_components.py [path-to-follow-me-therapy-app]

Reads design/components.json (the list of shared components and where each
product implements them) and design/tokens.json, and reports:

  missing     a component a product should have but doesn't (a CSS selector,
              a React export, or, given the app's path, a Compose function).
              A product without a component must say why: {"none": "..."}.
  colour      a component with tokens (component.<name> in tokens.json) taking
              a colour directly (var(--purple-tint)) instead of its component
              token (var(--chip-purple-fill)). Rules live in the tokens, so a
              change there reaches every product.
  contrast    a component's words on its fill under 7:1, or its icons and
              state outlines under 3:1 (WCAG 2.2 AAA), in any tone it's used
              in (components.json "tones").
  unlisted    a React export or app component (Fmt…) that isn't in the list.

Run by tools/check_tokens.py, so it runs on every push to preview. The app
runs the same file against its own code in CI.
Exits non-zero when something is found.
"""
import json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
T = json.loads((ROOT / "design/tokens.json").read_text())
M = json.loads((ROOT / "design/components.json").read_text())
APP = Path(sys.argv[1]) if len(sys.argv) > 1 else None
# The app repo carries a synced copy of this file and the design files; run
# there, it checks the app (and skips the website and Design System it doesn't have).
if APP is None and (ROOT / "designsystem").is_dir():
    APP = ROOT

problems = []
def report(kind, msg):
    problems.append(f"{kind}: {msg}")


# ── Colours, through aliases ──
def flat(node, path=()):
    for k, v in node.items():
        if k.startswith("$"):
            continue
        if isinstance(v, dict) and "$value" in v:
            yield path + (k,), v
        elif isinstance(v, dict):
            yield from flat(v, path + (k,))
TOK = {".".join(p): v for p, v in flat(T)}
def resolve(val):
    while isinstance(val, str) and (m := re.fullmatch(r"\{([^}]+)\}", val)):
        val = TOK[m.group(1)]["$value"]
    return val
COLOUR_NAMES = {k for k in T["color"] if not k.startswith("$")}
TONES = [k for k in T["tone"] if not k.startswith("$")]
COMPONENT_TOKENS = {k for k in T["component"] if not k.startswith("$")}

def part(comp, name, tone):
    """The hex colour of component part `name` for `tone`."""
    v = T["component"][comp][name]
    if v.get("$type") == "other":
        return resolve(T["tone"][tone][v["$value"]]["$value"])
    return resolve(v["$value"])


# ── Contrast (WCAG 2.2 AAA: 7:1 text, 3:1 icons and outlines that carry meaning) ──
def lum(h):
    h = h.lstrip("#")
    c = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    c = [x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4 for x in c]
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]
def ratio(a, b):
    a, b = lum(a), lum(b)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
TEXT, ICON = 7.0, 3.0
PAIRS = [  # (component, foreground part or colour, background part or colour, minimum)
    ("chip", "ink", "fill", TEXT), ("chip", "ink", "fill-on-ground", TEXT),
    ("tag", "ink", "fill", TEXT),
    ("tile", "icon", "fill", ICON), ("tile", "icon", "fill-on-ground", ICON),
    ("note", "#ink-soft", "fill", TEXT), ("note", "lead-in", "fill", TEXT), ("note", "icon", "#paper", ICON),
    ("head", "#ink", "fill", TEXT),
    ("card", "more", "fill", TEXT),
    ("choice", "#ink", "fill", TEXT), ("choice", "#ink", "fill-selected", TEXT), ("choice", "line-selected", "fill", ICON),
    ("button", "ink", "fill", TEXT), ("button", "ink", "fill-hover", TEXT), ("button", "outline-ink", "outline-fill", TEXT),
    ("button", "outline-ink-hover", "outline-fill-hover", TEXT), ("button", "quiet-ink", "quiet-fill", TEXT),
    ("button", "disabled-ink", "disabled-fill", TEXT),
    ("bar", "ink", "fill", TEXT), ("bar", "tab-ink", "fill", TEXT), ("bar", "tab-ink-selected", "fill", TEXT),
    ("bar", "tab-icon-selected", "fill", ICON),
    ("step", "ink", "fill", TEXT), ("step", "done-ink", "done-fill", ICON),
    ("celebrate", "star-line", "fill", ICON), ("breathe", "dot", "fill", ICON),
    ("card", "done-ink", "done-fill", ICON), ("calendar", "done-ink", "done-fill", ICON), ("calendar", "ink", "today", TEXT), ("calendar", "day-ink", "today", TEXT),
]
# Tones each component is used in (components.json "tones"); others aren't checked.
USED = {c["tokens"]: c.get("tones", TONES) for c in M["components"].values() if "tokens" in c}
def colour_of(comp, ref, tone):
    if ref.startswith("#"):
        return resolve(T["color"][ref[1:]]["$value"])
    return part(comp, ref, tone)
for comp, fg, bg, need in PAIRS:
    tones = USED.get(comp, TONES) if any(T["component"][comp].get(x, {}).get("$type") == "other" for x in (fg, bg) if not x.startswith("#")) else [None]
    for tone in tones:
        r = ratio(colour_of(comp, fg, tone), colour_of(comp, bg, tone))
        if r < need:
            report("contrast", f"{comp}{'.' + tone if tone else ''}: {fg} on {bg} is {r:.2f}:1, needs {need:g}:1")


# ── The website and Design System CSS ──
def css_rules(path):
    text = re.sub(r"/\*(?! token:).*?\*/", "", path.read_text(), flags=re.S)
    text = re.sub(r"/\* media:start[\s\S]*?/\* media:end \*/", "", text)
    for m in re.finditer(r"([^{}]+)\{([^{}]*)\}", text):
        yield m.group(1).strip(), m.group(2)
SITE_CSS = [p for p in sorted((ROOT / "css").glob("*.css")) if p.name != "tokens.css"]
DS = ROOT / "design/design-system/project/components"
HAS_SITE, HAS_DS = bool(SITE_CSS), (DS / "bundle.css").exists()
DS_CSS = [DS / "bundle.css"] if HAS_DS else []
RULES = {p: list(css_rules(p)) for p in SITE_CSS + DS_CSS}
TSX = (DS / "src/index.tsx").read_text() if HAS_DS else ""
EXPORTS = set(re.findall(r"^export (?:function|const) ([A-Z]\w*)", TSX, flags=re.M))

def sel_re(sel):
    return re.compile(re.escape(sel) + r"(?![\w-])")
def has_selector(files, sel):
    r = sel_re(sel)
    return any(r.search(s) for p in files for s, _ in RULES[p])

COLOUR_PROPS = re.compile(r"^(background(-color)?|color|border(-(top|right|bottom|left))?(-color)?|outline(-color)?|fill|stroke|box-shadow|--[\w-]+)$")
def check_colours(comp, files, selectors):
    token = M["components"][comp]["tokens"]
    for p in files:
        for sel_text, body in RULES[p]:
            if not any(sel_re(s).search(sel_text) for s in selectors):
                continue
            for d in re.finditer(r"([\w-]+)\s*:\s*([^;]+)", body):
                prop, val = d.group(1), d.group(2)
                if not COLOUR_PROPS.match(prop):
                    continue
                for name in re.findall(r"var\(--([\w-]+)", val):
                    # Neutral words (ink, ink-soft) and the focus ring are the same everywhere.
                    if name in COLOUR_NAMES and name != "focus" and not (prop == "color" and name in ("ink", "ink-soft")):
                        report("colour", f"{p.relative_to(ROOT)}: {sel_text[:60]} {{ {prop}: var(--{name}) }} → use var(--{token}-…)")
                for name in re.findall(r"/\* token:([\w-]+) \*/", val):
                    if name in COLOUR_NAMES:
                        report("colour", f"{p.relative_to(ROOT)}: {sel_text[:60]} icon tagged token:{name} → tag a {token}-… token")

def listed(x):
    return x if isinstance(x, list) else [x]

for name, c in M["components"].items():
    if "tokens" in c and c["tokens"] not in COMPONENT_TOKENS:
        report("missing", f"{name}: tokens.json has no component.{c['tokens']}")
    # Website
    w = c.get("website")
    if w is None:
        report("missing", f"{name}: no website entry (selectors, or {{\"none\": \"why\"}})")
    elif not isinstance(w, dict) and HAS_SITE:
        for s in w:
            if not has_selector(SITE_CSS, s):
                report("missing", f"{name}: website selector {s} not found in css/")
        if "tokens" in c:
            check_colours(name, SITE_CSS, w)
    # Design System
    d = c.get("designSystem")
    if d is None:
        report("missing", f"{name}: no designSystem entry")
    elif "none" not in d and HAS_DS:
        if not has_selector(DS_CSS, d["css"]):
            report("missing", f"{name}: Design System selector {d['css']} not found in bundle.css")
        for r in listed(d["react"]):
            if r not in EXPORTS:
                report("missing", f"{name}: Design System has no React export {r} (src/index.tsx)")
        if "tokens" in c:
            check_colours(name, DS_CSS, [d["css"]])
    # App
    a = c.get("app")
    if a is None:
        report("missing", f"{name}: no app entry")

LISTED_REACT = {r for c in M["components"].values() if isinstance(c.get("designSystem"), dict) and "react" in c["designSystem"] for r in listed(c["designSystem"]["react"])}
HELPERS = {"Icon", "Chips", "Group", "WriteLines", "DrawBox"}  # parts of listed components
for e in sorted(EXPORTS - LISTED_REACT - HELPERS if HAS_DS else []):
    report("unlisted", f"React export {e} isn't in design/components.json")


# ── The app (given its checkout) ──
if APP:
    KT = {p: p.read_text() for p in (APP / "designsystem/src").rglob("*.kt")}
    def fun_body(fn):
        for p, text in KT.items():
            m = re.search(rf"\bfun (?:[\w.]+\.)?{fn}\(", text)
            if m:
                nxt = re.search(r"\n(?:@Composable\n)?(?:internal |private )?(?:fun|class|data class|object|val|enum class) ", text[m.end():])
                return p, text[m.start(): m.end() + (nxt.start() if nxt else len(text))]
        return None, None
    LISTED_APP = set()
    for name, c in M["components"].items():
        a = c.get("app")
        if a is None or isinstance(a, dict):
            continue
        for fn in listed(a):
            LISTED_APP.add(fn)
            p, body = fun_body(fn)
            if body is None:
                report("missing", f"{name}: app has no Compose function {fn}")
            elif "tokens" in c:
                for col in sorted(set(re.findall(r"FmtColors\.(\w+)", body))):
                    report("colour", f"{p.relative_to(APP)}: {fn} uses FmtColors.{col} → use Fmt{c['tokens'].capitalize()}Tokens")
    APP_FUNS = {f for text in KT.values() for f in re.findall(r"@Composable\n(?:fun|internal fun) (?:\w+\.)?(Fmt\w+)\(", text)}
    APP_HELPERS = {"FmtTheme", "FmtText", "FmtIcon", "FmtGroup", "FmtKidText", "FmtScreen", "FmtOnGround", "FmtFlowScaffold", "FmtActionBarHost",
                   "FmtActionSpacer", "FmtPage", "FmtSiteHead", "FmtBand", "FmtFooter", "FmtBackButton", "FmtIconButton", "FmtStarToggle"}
    for f in sorted(APP_FUNS - LISTED_APP - APP_HELPERS):
        report("unlisted", f"app component {f} isn't in design/components.json")

for p in problems:
    print(p)
print(f"{len(problems)} design-system problem(s)" if problems else "Every product implements the design system the same way.")
sys.exit(1 if problems else 0)
