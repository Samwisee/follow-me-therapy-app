# Working on the Follow Me Therapy app

Kotlin Multiplatform + Compose Multiplatform app for Simone's practice (see
README.md). Sam maintains it; the website lives in Samwisee/follow-me-therapy.

- The design tokens come from the website repo. Never edit `FmtTokens.kt`:
  sync `design/tokens.json` (`tools/sync_tokens.sh`) and run `python3 tools/gen_tokens.py`.
- No hard-coded colours, sizes or spacing in UI code: tokens, `FmtTheme.metrics`, `FmtTheme.type`.
- Components live in `designsystem/` and mirror the website's design-system components
  (same names and rules). New activity UI goes there first, then the app uses it.
- Voice: the app speaks as Simone ("I"). She is not NDIS-registered: never imply
  NDIA-managed plans can pay. Chevrons (›), not arrows.
- Keep data on the device. No analytics SDKs or accounts until there's a privacy
  plan (child development information is health information).
- The cloud workspace can't reach Maven: builds run in GitHub Actions. Read results
  with `gh api repos/Samwisee/follow-me-therapy-app/actions/runs`.
- Commit author: `git -c user.name=Claude -c user.email=noreply@anthropic.com`.
