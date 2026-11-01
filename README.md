# Muse Widget

Home-screen widget and **Muse companion layer** for **Muse from Meta**
(`com.facebook.aura`) on Samsung Galaxy devices, running on stock
One UI Home. Current release: **v4.12.0**.

## v4.12.0 — Voice mode, via the accessibility workaround

- **The Voice activity is now "Voice mode" — and it actually
  enters voice mode.** Muse's `hatch://voice` deeplink is
  gated to Meta-signed apps, so the old shortcut could only
  open Muse's assistant surface. Voice mode now hands off to
  the companion **Muse Voice Mode Shortcut** app
  (https://github.com/jonathancruzate/muse-voice-mode-shortcut),
  whose accessibility service taps Muse's own voice button.
  It's a toggle: each tap of the shortcut flips voice mode on
  or off — the same switch as that app's side-button mapping.
- Works everywhere the activity can be assigned: the
  Activities picker, the shortcut panel widget, the search
  bar's right button, and the ask page's glyph. Existing
  assignments stored under the old "Voice" label keep working.
- If the Muse Voice Mode Shortcut app isn't installed, the
  shortcut explains itself with a toast and opens Muse's
  assistant surface instead — never a dead end.
- Installs over v4.11.0 in place (same package, same signing
  key). APK SHA-256
  `7940496792df1ffb7b30ec66427911940a282909475b011c26746c6bbb1614a7`.

## v4.11.0 — the package and APK now match the project name

- **Package renamed:** `com.samsung.oneui9.metamusewidget` →
  `com.jonathancruzate.metamusewidget`, so the app's identity
  matches this GitHub project (`jonathancruzate/meta-muse-widget`).
  The APK is now `Meta-Muse-Widget-4.11.0.apk` (was
  `Muse-Widget-<version>.apk`). The app's display name stays
  **Muse Widget**, and there are no feature changes.
- **Reinstall required.** A package rename is a new app identity
  to Android: v4.11.0 does **not** install over v4.10.0 or
  earlier. Uninstall the old app, install v4.11.0, then re-add
  the widgets to your home screen and set your preferences
  again (bar style, theme, shortcut cells, panel endpoint) —
  the old app's settings do not carry over.
- **New signing key for the new identity.** The legacy keystore's
  certificate DN still carried the old project's origin
  (`O=OneUI9`), so v4.11.0 is signed with a fresh keystore whose
  DN is `CN=Meta Muse Widget, O=JonathanCruzate, C=US`
  (cert SHA-256
  `2746d2a2d6dbe0fda4271acd14f4e5ab990da4e9155e7f9e75ae07fca4b814da`).
  The old keystore is preserved alongside as
  `keystore/keystore-legacy-oneui9.keystore`. The cert change is
  free: it happened before the new package
  (`com.jonathancruzate.metamusewidget`) was ever released or
  installed, and the old `com.samsung…` package line is abandoned
  anyway — there is no install or upgrade impact. Both build
  guards (render guard, no-secrets guard) pass. Final v4.11.0
  APK: 444,270 bytes, SHA-256
  `2c3a5dc437153fe96954213b29935083da5beecedce8df22abeb8ed0b71c5780`.

## v4.9.1 — Activities picker, and the panel takes your own endpoint

- **Activities picker (new in v4.9.0, first released here).** A
  long-press app shortcut — **Activities** — opens a picker of
  prompt-stem shortcuts: pick an activity (plan my day, check my
  messages, review my spending, …) and Muse opens with the
  prompt already started in the composer, ready to finish and
  send. It's the fast path for the asks you make every day.
- **Bring your own panel endpoint.** The live Muse panel (brief
  highlights + house status, in the panel and its widget) used
  to read from a URL baked in at build time — which meant it
  could only ever work on the author's own build. v4.9.1 removes
  the baked-in URL completely: open the app → **Panel data
  endpoint**, paste the full URL of your own panel-data endpoint,
  and the panel lights up. The URL is stored only on your device,
  the settings screen shows just "Configured · <host>" afterwards,
  and Clear removes it. The app ships with **no endpoint
  configured** — until you add one, the panel and its widget show
  their "not configured" state. Any endpoint that serves the
  panel JSON contract works; if your Muse maintains your brief,
  it can stand one up for you.
- **Guards tightened.** The no-secrets guard now fails the build
  if the old build-time value (or any baked panel URL) appears
  anywhere in source or the compiled dex — the endpoint is
  runtime configuration, never a build artifact. Render guard
  unchanged; same signing key; installs over v4.9.0.

## v4.8.0 — the panel is a widget now

- **Resizable panel widget.** The Muse panel is now also a
  home-screen widget (`MusePanelWidgetProvider`), beside the
  search bar, which stays frozen and untouched. Add it from the
  widget picker as **Muse panel**, then stretch it anywhere from
  a compact strip to full screen — it reflows through three
  densities: compact (brief headline + house line), medium
  (+ brief meta, three highlights, the decide counter, Refresh),
  and full (greeting/date header, five highlights, house block,
  Up Next, Refresh). On API 31+ the densities ship as one
  responsive RemoteViews with size breakpoints; below that the
  provider picks a density from the widget's size and re-picks
  on every resize.
- **Same live data, same rules.** The widget renders through the
  shared `PanelDataClient` fetch+cache: last-good data shows
  immediately and a background fetch refreshes it, so the widget
  never blocks on the network. Tapping the widget opens the full
  Muse panel; the in-widget Refresh and the panel itself both
  keep it in step (the panel pokes the widget after each fresh
  fetch). Up Next appears at full density only when calendar
  access is already granted — a widget can't ask for it.
- **One UI 9 + Material You.** The card is the One UI 9 focus
  block (18dp radius, day/night container fill); on API 31+ the
  fill switches to the Material You system neutral tones and the
  accent resolves at render time from the framework dynamic
  accent, with the One UI primary #0381FE as the fallback.
- **Guards extended.** The build's render guard now scans every
  widget layout (the search bar plus all three panel densities)
  and fails the build if any of them uses a view class
  RemoteViews can't inflate; the no-secrets guard is unchanged.
  No new permissions; same signing key; installs over v4.7.0.

## v4.7.0 — the panel goes live

- **Live data in the Muse panel.** A new "Muse today" card shows
  the day's brief highlights (distilled on the host from the
  already-redacted Daily Brief snapshot — the raw brief body never
  leaves the host) plus a house-status row (current mode with its
  report age, desired-vs-actual when out of sync). Design:
  [refs/LIVE-DATA-LANE-DESIGN.md](refs/LIVE-DATA-LANE-DESIGN.md).
- **How it connects.** *(Superseded in v4.9.1 — the endpoint is
  now set in the app; nothing is injected at build time.)*
  The host pushes the distillate to a
  secret-authed ingest endpoint on the existing mode-sync Worker;
  the app reads one merged JSON from a capability URL whose slug
  is injected at build time from a local 0600 file (the
  keystore-password pattern) — the slug is never in this source
  tree, and `build.sh` fails the build if any real secret pattern
  reaches the source or the compiled dex (the new no-secrets
  guard, beside the render guard). Producer:
  [tools/panel-brief-push.py](tools/panel-brief-push.py).
- **V1 content defaults.** Decision-kind highlights are excluded
  (only the count renders — "N waiting on you") and
  health-adjacent items are filtered out entirely; stale data
  always shows its age in words, never fake-fresh. Fetch happens
  on panel open (throttled) plus a manual Refresh; no background
  work, no new permissions, and the widget surfaces are
  untouched.

## v4.6.2 — render bug FIXED + Muse companion

- **Render bug fixed.** The "Couldn't add widget" error that broke the
  search-bar widget from v4.3.2 through v4.5.2 was root-caused by
  binary diff (REA/jadx) against the last working build, v4.3.1:
  v4.3.2 added a plain `android.view.View` (`iv_logo_hit`, a logo
  tap-target overlay) to the widget layout — a bare `View` is not a
  RemoteViews-supported type, so One UI Home failed inflation and
  substituted its error layout in both the picker preview and the
  placed widget. The overlay is gone (the logo tap now lives on the
  logo ImageView itself), and `build.sh` carries a build-time guard
  that fails the build if a plain `View` ever reappears in the
  compiled widget layout. Verified working on-device 2026-10-28.
- **Muse companion panel** (`MusePanelActivity`) — greeting, an
  **Up Next** card (next calendar event in 24h, `READ_CALENDAR`),
  Ask Muse actions, and home-control shortcuts to SmartThings and
  Google Home.
- **Muse QS tile** (`MuseTileService`) and a **"Muse today"**
  long-press shortcut open the panel; the **Ask Muse** QS tile and
  shortcut open the ask page.
- Renamed **Stef → Muse** throughout (v4.6.1).

## Search-bar styles

The widget ships with two search-bar styles — each a faithful port
of its platform's bar, from values recovered by decompiling the
real launchers:

- **One UI** — Samsung's One UI 9 Finder bar, from One UI Home
  18.0.07.59 (fill `#F2E9E9E9` light / `#F23D3D3D` dark, 52dp
  pill, 32dp radius, 0.5dp outline, 14sp hint), with a
  wallpaper-frost glass treatment.
- **Pixel (Material You)** — the Pixel Launcher 17 QSB: a
  stadium plate filled with the framework's dynamic
  `surfaceContainerLow`, soft shadow, the shortcut in a filled
  circle, and — under the System theme — the logo rendered as a
  17-stop sweep of the framework's accent tones, the same
  mechanism as Pixel's dynamic G (traced in the launcher's
  code). Any other theme tints the logo with its own accent:
  theming reaches the logo in both styles.

## Widget

- **Search bar (4×1)** — the Finder pill, rendered as a bitmap so
  it can carry a real frosted-glass effect: the home-screen
  wallpaper is sampled, blurred, and composited under the tint
  (widgets can't backdrop-blur). Tapping it opens the ask page;
  tapping the logo just opens the Muse app; the right-hand
  button is a configurable action whose glyph changes to match.
  It appears in the picker with a live preview, in the searchbox
  category. Long-press → the pencil opens the customize screen
  for that widget. The widget update path is hardened: a render
  failure falls back to a flat plate with the same tap targets
  instead of leaving the host an error placeholder.
  (An earlier 4×2 action-grid widget was removed in v4.5.0 —
  its actions were the deep links the finding below rules out.)

## The ask page

A Finder/Google-style search screen in the app: keyboard up, a
glass pill docked over the keyboard (IME inset consumed
directly), recent asks and starter suggestions below, predictive
filtering as you type. Enter sends the text into Muse's
composer — see the deep-link note below for how, and why it
doesn't use a deep link.

## The app is the settings page

Opening the app lands on the Customize screen — the model is
the Google search widget's own app. Bar style (One UI Finder /
Pixel Material You), theme (One UI blue / system palette /
Stephany rose / custom hue + saturation), pill transparency,
and the shortcut action, with a live preview; the same screen
hosts the **Panel data endpoint** setting described above.
Settings apply
globally, or per-widget when the screen is opened from the
widget itself. Per the design's scope rule, **the theme color
only ever touches the M logo and the action icons** — the bar
itself is always the exact Finder bar (or the Pixel plate). The
Stephany theme is a muted rose pink (`#B46C5D`) sampled from a
reference photo; it's a hidden option, unlocked by tapping the
preview pill five times.

Earlier versions opened on a catalog of all 71 Muse
destinations recovered from the Muse app's route table
(`hatch://` routes). It was removed in v4.5.0: because of the
finding below those URIs can't be launched from outside the
Meta family, so a browsable list of them was reference
material nobody could use.

## Why the deep links don't launch (reverse-engineering note)

`hatch://` deep links from this app open Muse to its home
screen with the destination silently discarded. Decompiling
Muse 13.0.0.4.159 shows why: the deep-link handler forwards the
intent to the main activity, which runs a **TrustedCaller**
check whenever the intent carries data. The check validates the
caller against package allowlists and an embedded Meta code-
signing root CA — a payload deep link is accepted only from a
Meta-signed app. The handler's own hop passes; a third-party
sender with the identical intent does not, and the data is
wiped on the spot. Payload deep-linking from outside the Meta
family is a dead end by cryptographic design, not a bug.

What *is* open is the surface built for third-party assistants:
an `android.intent.action.ASSIST` intent carrying the
`com.facebook.aura.extra.ASSIST_QUERY` extra lands in Muse's
main chat with the composer prefilled. That ungated surface is
what this app routes through (the deep-link handler's separate
`public_relay` path drops its payload by design, so it is not
an alternative).

## Building

No Gradle — the build is a single script driving `aapt2`,
`javac`, `d8`, `zipalign`, and `apksigner` directly:

```sh
# needs: JDK 21, Android SDK build-tools 35.0.0 + platform android-35
ANDROID_SDK_ROOT=/path/to/sdk ./build.sh
```

`build.sh` generates a local signing keystore on first run —
bring your own for anything you ship; the generated one is for
local installs only. Package: `com.jonathancruzate.metamusewidget`,
minSdk 24, targetSdk 35.

## License

MIT — see [LICENSE](LICENSE).

## Install it with Muse

Just give this prompt to your Muse:

> "Install Muse Widget from https://github.com/jonathancruzate/meta-muse-widget — read the README, install the latest release on my phone, and set it up for me."

Or add this repo in [Obtainium](https://github.com/ImranR98/Obtainium) — it tracks the GitHub releases and keeps the app updated automatically. It works really well.

