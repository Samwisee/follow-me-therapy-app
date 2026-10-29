# Follow Me Therapy app

Simone's home activities (the same programs as the printed worksheets) as an
app, built with Kotlin Multiplatform and Compose Multiplatform on the Follow Me
Therapy design system. Web (Kotlin/Wasm) first; Android and iOS targets slot in
later without changing the shared code.

| Path | What's there |
|---|---|
| `design/tokens.json` | A copy of the design tokens. The single source is `design/tokens.json` in the website repo (Samwisee/follow-me-therapy). Sync with `tools/sync_tokens.sh` |
| `tools/gen_tokens.py` | Generates `FmtTokens.kt` from the tokens. CI fails if it's stale |
| `designsystem/` | The design system as a Compose library: tokens, theme (fluid sizes by window width, like the site), fonts, icons and components |
| `composeApp/` | The app: home, the three activities, and a design-system page |

## Run it

```sh
./gradlew :composeApp:wasmJsBrowserDevelopmentRun     # dev server with reload
./gradlew :composeApp:wasmJsBrowserDistribution       # production build in composeApp/build/dist/wasmJs/productionExecutable
```

CI builds every push and publishes `main` to GitHub Pages.

## Design system rules (same as the website)

- Never hard-code a colour, size or spacing: use `FmtColors`, `FmtSpace`, `FmtSize`, `FmtRadius`, `FmtBorder` and the resolved `FmtTheme.metrics` / `FmtTheme.type`.
- Semantic sizes (text, gutter, card-pad, grid-gap, stack, group-gap) are fluid: phone value at 360dp wide, laptop value at 1440dp.
- Corners nest: container radius = inner radius + padding + outline. Every container with its own corners wears an outline in its colour.
- One text edge: words and whatever a group holds sit card-pad + outline in; only the header and notes span wider.
- Accessibility target is WCAG 2.2 AAA: 7:1 text, a focus ring on everything interactive, 44dp targets, never colour alone.

Fonts: Fredoka and Quicksand, SIL Open Font License (see `licenses/`).
