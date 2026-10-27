# Muse Widget

Home-screen widgets and a companion app for **Muse from Meta**
(`com.facebook.aura`) on Samsung Galaxy devices, with two
search-bar styles — each a faithful port of its platform's bar,
from values recovered by decompiling the real launchers:

- **One UI** — Samsung's One UI 9 Finder bar, from One UI Home
  18.0.07.59 (fill `#F2E9E9E9` light / `#F23D3D3D` dark, 52dp
  pill, 32dp radius, 0.5dp outline, 14sp hint), with a
  wallpaper-frost glass treatment.
- **Pixel (Material You)** — the Pixel Launcher 17 QSB: a
  stadium plate filled with the framework's dynamic
  `surfaceContainerLow`, soft shadow, untinted logo, and the
  shortcut in a filled circle.

## Widgets

- **Search bar (4×1)** — the Finder pill, rendered as a bitmap so
  it can carry a real frosted-glass effect: the home-screen
  wallpaper is sampled, blurred, and composited under the tint
  (widgets can't backdrop-blur). Tapping it opens the ask page;
  the right-hand button is a configurable action whose glyph
  changes to match.
- **Action grid (4×2)** — New chat, Assistant, Search, Voice,
  Open app, Copy link, Destinations, Customize.
- Both widgets appear in the picker with live previews, and the
  bar widget is in the searchbox category. Long-press →
  Settings opens the customize screen.

## The ask page

A Finder/Google-style search screen in the app: keyboard up, a
glass pill docked over the keyboard (IME inset consumed
directly), recent asks and starter suggestions below, predictive
filtering as you type. Enter sends the text into Muse's
composer — see the deep-link note below for how, and why it
doesn't use a deep link.

## Customize

Theme (One UI blue / system palette / Stephany rose / custom hue
+ saturation), pill transparency, and the shortcut action. Per
the design's scope rule, **the theme color only ever touches the
M logo and the action icons** — the bar itself is always the
exact Finder bar. The Stephany theme is a muted rose pink
(`#B46C5D`) sampled from a reference photo.

## The hub app

A One UI-styled catalog of all 71 Muse destinations recovered
from the Muse app's route table (`hatch://` routes, verified
per-section applinks, web routes), with filter-as-you-type
search. Tapping a destination copies its URI — because of the
finding below, copying is the honest behavior; pretending to
launch them would just fail silently.

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
