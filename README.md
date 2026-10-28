# Muse Widget

Home-screen widget and **Muse companion layer** for **Muse from Meta**
(`com.facebook.aura`) on Samsung Galaxy devices, running on stock
One UI Home. Current release: **v4.7.0**.

## v4.7.0 — the panel goes live

- **Live data in the Muse panel.** A new "Muse today" card shows
  the day's brief highlights (distilled on the host from the
  already-redacted Daily Brief snapshot — the raw brief body never
  leaves the host) plus a house-status row (current mode with its
  report age, desired-vs-actual when out of sync). Design:
  [refs/LIVE-DATA-LANE-DESIGN.md](refs/LIVE-DATA-LANE-DESIGN.md).
- **How it connects.** The host pushes the distillate to a
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
and the shortcut action, with a live preview; settings apply
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
local installs only. Package: `com.samsung.oneui9.metamusewidget`,
minSdk 24, targetSdk 35.

## License

MIT — see [LICENSE](LICENSE).
