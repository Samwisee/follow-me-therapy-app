#!/usr/bin/env bash
# Take the design from the website repo (the single source) and regenerate.
#   tools/sync_design.sh                 download from GitHub (branch $DESIGN_REF, default preview)
#   tools/sync_design.sh ../follow-me-therapy   copy from a local checkout
# Then checks the app against it. CI runs this on every build, and the
# "Design sync" workflow runs it hourly and commits what changed.
set -euo pipefail
FILES="design/tokens.json design/components.json design/icons.json tools/check_components.py"
if [ $# -ge 1 ]; then
  for f in $FILES; do cp "$1/$f" "$f"; done
else
  BASE="https://raw.githubusercontent.com/Samwisee/follow-me-therapy/${DESIGN_REF:-preview}"
  for f in $FILES; do curl -fsSL "$BASE/$f" -o "$f"; done
fi
python3 tools/gen_tokens.py
python3 tools/gen_icons.py
python3 tools/check_components.py
python3 tools/check_literals.py
