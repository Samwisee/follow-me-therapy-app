#!/usr/bin/env bash
# Copy the design tokens from the website repo (the single source) and regenerate.
#   tools/sync_tokens.sh [path-to-follow-me-therapy-checkout]
set -euo pipefail
SITE="${1:-../follow-me-therapy}"
cp "$SITE/design/tokens.json" design/tokens.json
python3 tools/gen_tokens.py
