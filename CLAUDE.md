# Working on the Follow Me Therapy app

Kotlin Multiplatform + Compose Multiplatform app for Simone's practice (see
README.md). Sam maintains it; the website lives in Samwisee/follow-me-therapy.

- The design comes from the website repo (design/tokens.json, components.json,
  icons.json). Never edit `FmtTokens.kt`, `FmtIcons.kt` or the copies in design/:
  change them in the website repo, then `tools/sync_design.sh ../follow-me-therapy`
  (CI and the hourly Design sync workflow do this too).
- Shared components (design/components.json) take their colours from their
  component tokens (`FmtChipTokens.fill(tone)` …), never `FmtColors` directly;
  `tools/check_components.py` and `tools/check_literals.py` must pass.
- No hard-coded colours, sizes or spacing in UI code: tokens, `FmtTheme.metrics`, `FmtTheme.type`.
- Components live in `designsystem/` and mirror the website's design-system components
  (same names and rules). A new shared component goes in components.json first,
  then into every product (the website, the Design System and here).
- Voice: the app speaks as Simone ("I"). She is not NDIS-registered: never imply
  NDIA-managed plans can pay. Chevrons (›), not arrows.
- Keep data on the device. No analytics SDKs or accounts until there's a privacy
  plan (child development information is health information).
- The cloud workspace can't reach Maven: builds run in GitHub Actions. Read results
  with `gh api repos/Samwisee/follow-me-therapy-app/actions/runs`.
- Commit author: `git -c user.name=Claude -c user.email=noreply@anthropic.com`.
