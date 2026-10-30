# Follow Me Therapy app

Simone's home activities (the same programs as the printed worksheets) as an
app, built with Kotlin Multiplatform and Compose Multiplatform on the Follow Me
Therapy design system. Web (Kotlin/Wasm) first; Android and iOS targets slot in
later without changing the shared code.

| Path | What's there |
|---|---|
| `design/tokens.json`, `components.json`, `icons.json` | Copies of the design system's single source in the website repo (Samwisee/follow-me-therapy › design/). Never edit here |
| `tools/sync_design.sh` | Takes the design from the website (GitHub, or a local checkout), regenerates `FmtTokens.kt` (`gen_tokens.py`) and `FmtIcons.kt` (`gen_icons.py`), and runs the checks |
| `tools/check_components.py` | The website's checker (synced): every component in `components.json` exists here and takes its colours from its component tokens (`Fmt…Tokens`) |
| `tools/check_literals.py` | No hand-written colours or dp/sp sizes in UI code |
| `designsystem/` | The design system as a Compose library: tokens, theme (fluid sizes by window width, like the site), fonts, icons and components |
| `composeApp/` | The app: tabs (Today, Activities, For grown-ups), the guided flows, the on-device store (`Store.kt`) and a design-system page |

## Run it

```sh
./gradlew :composeApp:wasmJsBrowserDevelopmentRun     # dev server with reload
./gradlew :composeApp:wasmJsBrowserDistribution       # production build in composeApp/build/dist/wasmJs/productionExecutable
```

CI builds every push and publishes `main` to GitHub Pages:
https://samwisee.github.io/follow-me-therapy-app/

**Plans from Simone.** Simone makes a family's plan at `#planner` (her
service, a note, the morning routine and practice activities with days and
reminder times, the calm-down plan) and shares the link it makes. The plan is
inside the link (`#plan=<code>`, `Plan.kt`): the part after # never reaches a
server. Opening it shows the plan; Add my plan keeps it on the device. The
Plan tab adds reminders to the phone's calendar (a weekly `.ics`, no server)
and sends Simone the week's progress in words (shared or copied, only where
the family sends it). Today shows what's planned for today.

Every screen has its own address, so a link can go straight to it:
`#plan`, `#planner`, `#morning`, `#calm`, `#calm-now`, `#week`, `#grown-ups`,
`#design-system` (flow steps add `/n`, e.g. `#morning/2`).

Progress, the calm-down plan and the week's stars are saved on the device as
one versioned record (`Record.kt`: each entry with its value and when it
changed; local storage key `fmt.record`). Nothing is sent anywhere. The same
JSON is the backup file (For grown-ups › Save a backup / Restore; restoring
keeps whichever entry changed last) and is what a sync service would exchange
if there's ever a backend: send the record, merge by time. Bump
`Record.SCHEMA` and migrate in `Record.parse` when the shape changes.

It's a progressive web app: installable (`manifest.webmanifest`, colours from
the tokens, icons synced from the website), works offline (`sw.js`, network
first, one cache per build), and asks the browser to keep its data. The
install guide is at `#install`. On iPhone the Home Screen app keeps its own
data, separate from Safari: move it across with a backup.

After each build on `main`, CI also screenshots every screen at phone (390) and
laptop (1280) width, times scrolling, and pushes the results to the
`ci-screens` branch (`report.json` has frame times and console errors).

## How design changes reach the app

The website repo owns the design: `design/tokens.json` (colours, type, sizes,
the role each colour plays in each tone, and component tokens),
`design/components.json` (every shared component, its rules and where each
product implements it) and `design/icons.json`. The app never keeps its own:

- **Every build** (`build.yml`) downloads them from the website's `preview`
  branch, regenerates the Kotlin, and fails if the app no longer implements
  the design (a component missing, a colour taken directly, a hand-written size).
- **Hourly** (`design-sync.yml`, or run it from the Actions tab) does the same,
  commits what changed and rebuilds and deploys the app. So a design change on
  the website reaches the app within the hour, or fails loudly saying what the
  app is missing.
- To follow the live site instead of preview, set `DESIGN_REF: main` in both workflows.

## Design system rules (same as the website)

- Never hard-code a colour, size or spacing: use `FmtColors`, `FmtSpace`, `FmtSize`, `FmtRadius`, `FmtBorder` and the resolved `FmtTheme.metrics` / `FmtTheme.type`.
- Semantic sizes (text, gutter, card-pad, grid-gap, stack, group-gap) are fluid: phone value at 360dp wide, laptop value at 1440dp.
- Corners nest: container radius = inner radius + padding + outline. Every container with its own corners wears an outline in its colour.
- One text edge: words and whatever a group holds sit card-pad + outline in; only the header and notes span wider.
- Accessibility target is WCAG 2.2 AAA: 7:1 text, a focus ring on everything interactive, 44dp targets, never colour alone.

Fonts: Fredoka and Quicksand, SIL Open Font License (see `licenses/`).
