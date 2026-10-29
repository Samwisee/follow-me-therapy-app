#!/usr/bin/env bash
# Take the design from the website repo (the single source) and regenerate.
#   tools/sync_design.sh                 download from GitHub (branch $DESIGN_REF, default preview)
#   tools/sync_design.sh ../follow-me-therapy   copy from a local checkout
# Then checks the app against it. CI runs this on every build, and the
# "Design sync" workflow runs it hourly and commits what changed.
set -euo pipefail
FILES="design/tokens.json design/components.json design/icons.json tools/check_components.py"
# The app icons are the website's full-bleed Home Screen icons (img/app-icon-*.png).
ICONS="180 192 512"
RES=composeApp/src/wasmJsMain/resources/icons
mkdir -p "$RES"
if [ $# -ge 1 ]; then
  for f in $FILES; do cp "$1/$f" "$f"; done
  for s in $ICONS; do cp "$1/img/app-icon-$s.png" "$RES/icon-$s.png"; done
else
  BASE="https://raw.githubusercontent.com/Samwisee/follow-me-therapy/${DESIGN_REF:-preview}"
  for f in $FILES; do curl -fsSL "$BASE/$f" -o "$f"; done
  for s in $ICONS; do curl -fsSL "$BASE/img/app-icon-$s.png" -o "$RES/icon-$s.png"; done
fi
python3 tools/gen_tokens.py
python3 tools/gen_icons.py
python3 tools/check_components.py
python3 tools/check_literals.py
