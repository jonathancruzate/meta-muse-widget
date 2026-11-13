# Meta Muse Widget — Muse's home-screen front door to Muse

The widget and companion app Muse built for Jonathan's Galaxy S24+:
a search-bar widget, a live Muse panel (activity + widget), and an
activities shortcut system for the Muse app (`com.facebook.aura`),
all on stock One UI Home. It was built when Muse shipped no widgets
of its own. **Placement status (2026-11-07): Muse now ships native
Spaces widgets, and Jonathan replaced the Muse Panel widget on his
home screen with them — the Daily Brief space widget is the one he
showed live that evening. Do not assume the Panel widget is placed
or is his primary home-screen surface. **His decision the same
evening: keep this app for its search bar and shortcuts** — the app
is not retired; only the Panel widget was replaced, and the panel
data lane below stays as-is. A note of the change is in the
project's GitHub README (Status note — 2026-11-07).** Project:
`~/workspace/meta-muse-widget/` (public repo
`jonathancruzate/meta-muse-widget`); the full build history is
`BUILD-REPORT.md` in that folder.

This skill ships with the app (Jonathan's standing rule, 2026-11-03:
every APK we build carries a skill teaching Muse how to interact with
and use it). The in-repo copy is
`~/workspace/meta-muse-widget/SKILL.md`; the installed copy is
`~/workspace/custom-skills/local-meta-muse-widget/SKILL.md`. They are
the same document — update both in the same handling, always.

## Identity and version truth

- **Package:** `com.jonathancruzate.metamusewidget` (renamed in
  v4.11.0 from `com.samsung.oneui9.metamusewidget` to match the
  GitHub project name; the old package line is abandoned). Display
  label: **Muse Launcher Pack** since v5.0.0 (was Muse Widget
  through v4.12.2). minSdk 24, targetSdk 35.
- **Latest build: v5.3.0 (versionCode 45)** — see the v5.3.0 note below; prior: v5.2.0 (versionCode 44)** —
  `Meta-Muse-Widget-5.2.0.apk` in the project root, SHA-256
  `0f2ef6d7854cab6248c6822cb5ed755a344b9cdb3d9479c8985ab85a84468fff`.
  The v5 line is the Muse Launcher Pack (below); intermediate
  builds v5.0.0 (code 42) and v5.1.0 (code 43) are in
  BUILD-REPORT.md with their hashes. Staged, not yet installed
  as of 2026-11-12 (phone ADB lane was down at build time).
  Version truth lives in `build.sh` (its `--version-code` /
  `--version-name` on the aapt2 link line); per-version notes live in
  `BUILD-REPORT.md` (its last entry is v4.12.1 — the v4.12.2 note
  lives in `github-export/README.md`). Versioned APKs accumulate in
  the project root; never treat an older APK there as current.
- **Signing:** keystore `keystore/metamuse.keystore` (alias
  `onemuse`), cert SHA-256
  `2746d2a2d6dbe0fda4271acd14f4e5ab990da4e9155e7f9e75ae07fca4b814da`
  since v4.11.0, so builds install over each other in place. (The
  pre-rename line used cert `34ef6099…`; the legacy keystore is kept
  as `keystore/keystore-legacy-oneui9.keystore`.)
- **What is actually installed on the phone is unverified from the
  repo** — check before assuming (see Verify below). Chat-delivered
  APKs can sit uninstalled while the repo moves on.
- **v5.3.0 (versionCode 45) — Muse-app aesthetic match
  (2026-11-13):** palette remapped to the official Muse app's
  VDS tokens decomposed from com.facebook.aura 13.0.0.4.159
  (accent #0064D4 / dark #007FFD, grounds #FCFCFC / #050505,
  cards #FFFFFF / #1F1F1F, text #111112 / #F5F5F5, card radius
  20dp; tokens doc in muse-ux-ui-rea). Supersedes D6's rose.
  Installed and dumpsys-verified the same night.


## The fact that shapes everything: Muse's deeplinks are gated

Muse's `hatch://` URIs are protected by Meta's TrustedCaller check —
the handler and the main activity verify the caller's signing
certificate against Meta's own root CA, so **no third-party app can
fire a data-carrying Muse deeplink** (proven by REA, not inferred:
`refs/REA-DEEPLINK-FINDINGS.md`). The full 71-URI catalog is recorded
in `BUILD-REPORT.md` and `refs/` for reference, but the shipped app
holds only the 27 working activities (the gated copy-a-URI rows were
removed in v4.12.1). The surfaces that DO work, and that every
feature is built on:

- **`ACTION_ASSIST` into Muse's main activity** — no data, no gate;
  Muse opens the main chat with the composer primed. The extra
  `com.facebook.aura.extra.ASSIST_QUERY` prefills the composer text.
  This is how the ask page sends, and how prompt-stem activities work.
- **Plain package launch** (open Muse) and the Router's fallback
  chain behind it: direct main-activity hop → gated handler →
  any-handler VIEW → web/app-link fallback → plain launch → Play
  Store → "not found" toast.
- **The Voice handoff:** the Voice activity dispatches to the Muse
  Voice Mode Shortcut app
  (`com.jonathancruzate.metamusevoicemodeshortcut/.ShortcutActivity`),
  whose accessibility service taps Muse's own voice button (a
  toggle). If that app is missing, a toast explains and the assist
  surface opens instead — never a dead end.

## What the app contains (per AndroidManifest.xml)

**Widgets (all placed from the launcher's widget picker, group
"Muse Widget"):**

1. **Search bar** (`SearchBarWidgetProvider`) — the 4×1 pill:
   tap opens the ask page; the M logo has its own tap that opens
   the Muse app; the right-hand button is a customizable shortcut
   (any activity, Assistant, Open app, or None to hide it).
   Re-renders on the 30-min platform cadence.
2. **Muse panel** (`MusePanelWidgetProvider`, v4.8.0+) —
   resizable live panel, size-adaptive (compact / medium / full
   densities): brief headline + highlights, decide count, house
   status, Up Next. Refresh: a self-scheduled ~15-min
   `AlarmManager.setAndAllowWhileIdle` chain firing the provider's
   own `com.jonathancruzate.metamusewidget.action.PANEL_WIDGET_REFRESH`
   broadcast, the 30-min `updatePeriodMillis` backstop, and an
   in-widget Refresh row. Renders the last-good cache first, then
   fetches — never blocks on the network.
3. **Shortcut panel** (`ShortcutPanelWidgetProvider`, v4.10.0+) —
   up to eight activity shortcuts in a 4×2 focus block. The cell
   set is one global setting edited in Customize (default: New
   thread, Search, Goals, Upcoming tasks, Feed, Explore, Memory,
   Assistant); an empty set renders one setup cell opening Settings.

**In-app surfaces:**

- **The app itself is the Customize screen**
  (`WidgetCustomizeActivity` — launcher activity AND the widgets'
  configure activity, global scope from the launcher, per-widget
  scope from the add flow / long-press pencil): live preview, bar
  style (One UI / Pixel), color themes (One UI, System palette,
  Custom hue/saturation, Stephany — hidden, 5-tap unlock on the
  preview pill), transparency, right-button shortcut, shortcut-panel
  cells, **Panel data endpoint**, and (v4.12.2) the **Voice mode**
  section showing the companion shortcut app's accessibility-service
  state with an Enable button into Android Accessibility settings.
- **Ask page** (`ComposeActivity`, not exported): suggestions
  (recents, starters, live Google Suggest predictions), Enter sends
  the text into Muse's composer via the ASSIST surface. Accepts a
  prefilled prompt stem from the picker.
- **Muse companion panel** (`MusePanelActivity`, not exported):
  greeting + date, the "Muse today" live card, Up next (next
  calendar event in 24h; needs READ_CALENDAR, degrades silently),
  Ask Muse rows, Home controls rows (open SmartThings / Google
  Home apps by package). Entry points are in-app only: the QS tile
  and the "Muse today" app-icon shortcut.
- **Activities picker** (`PickerActivity`, not exported): the 27
  activities in grouped sections; prompt-stem rows prefill the ask
  page, direct rows (Voice, Assistant, Open Muse app) fire at once.
- **QS tiles:** `AskTileService` (ask page) and `MuseTileService`
  (panel) — the user adds them to his QS panel himself.
- **App-icon shortcuts** (long-press): Ask Muse, New chat, Muse
  today, Activities — routed through `WidgetCustomizeActivity` as
  `widget://shortcut/…` intents.
- **Router trampoline** (`LaunchMuseActivity`, not exported,
  no-history): fires one target with the fallback chain above.

Permissions: QUERY_ALL_PACKAGES (find Muse + fallback packages),
INTERNET (Google Suggest + panel fetch), READ_CALENDAR (Up next),
READ_EXTERNAL_STORAGE ≤ API 32 (wallpaper for the glass pill).

## The panel data lane (Muse's side of the contract)

- Since v4.9.1 the panel endpoint is a **user setting**, not a
  build artifact: Customize → Panel data endpoint → paste the full
  URL. The app ships unconfigured and shows its "not configured"
  state until then. Jonathan's endpoint value is a secret — it
  lives in his phone's setting and server-side config; **never
  print it, and never bake it into a build** (the no-secrets guard
  fails the build if the retired slug value, either Worker secret,
  or the MacroDroid webhook appears in dex/source).
- Muse maintains the server side: the mode-sync Worker serves the
  merged panel JSON (brief distillate + house desired-vs-actual),
  and `tools/panel-brief-push.py` distills
  `~/workspace/daily-brief/dashboard_snapshot.json` and posts it —
  wired as a step of the daily-brief compile job; a failed push
  must never block the brief. Design record:
  `refs/LIVE-DATA-LANE-DESIGN.md`.
- Content rules at ingest (design §5): highlights capped, titles
  ≤80 / details ≤140 chars, **decide-kind items excluded — only the
  decide COUNT renders**, health-adjacent items excluded, raw brief
  body never leaves the VM.

## Build lane

Raw (non-Gradle) build, entirely from the project folder:

```
cd ~/workspace/meta-muse-widget && ./build.sh
```

Vendored Android SDK in `tools/` (build-tools 35.0.0, platform
android-35) + the Temurin JDK 21 from the local-rea tools. Output:
`Meta-Muse-Widget-<version>.apk` in the project root. Bump the
version in build.sh's aapt2 link line, and add the version note to
`BUILD-REPORT.md`, in the same handling.

**Two build guards FAIL the build — never bypass them:**

1. **Render guard** (v4.6.2, extended v4.8.0): every element of
   every compiled widget layout must be a RemoteViews-supported
   class; `iv_logo_hit` in resources.arsc also fails it.
2. **No-secrets guard** (v4.7.0): the secret checks described
   above; also fails on the strings "panel-data" / "PanelConfig"
   in the dex (baked-endpoint artifacts).

Audit the other raw builds for the androidx empty-library trap
(AGENTS.md): this app is plain Java against android.jar with no
androidx dependencies, so that trap does not apply here as built —
re-check if a dependency is ever added.

## The regression that must never recur

**v4.3.2 added a plain transparent `<View>` (`iv_logo_hit`) as a
logo tap overlay in the search-bar layout. `android.view.View` is
not RemoteViews-supported, so One UI Home failed to inflate the
widget — "Couldn't add widget", picker preview broken — and every
build from v4.3.2 through v4.5.2 carried it** (the swarm's first
rollback aimed at v4.3.2 as the baseline and missed that it was
already broken; REA's binary diff against his stated last-known-good
v4.3.1 found it — `refs/REA-RENDER-DIFF-431.md`). Rules:

- **Never put a plain `View` (or any non-RemoteViews class) in a
  widget layout.** Click targets go on supported views themselves
  (the logo tap lives on the `iv_muse_logo` ImageView — the v4.5.3
  fix).
- **No vector drawables as RemoteViews content either** (v4.5.1):
  the host process has no vector-compat inflater; rasterize glyphs
  to PNG.
- When a widget breaks, diff against **his stated last-known-good
  binary**, never an inferred baseline (AGENTS.md standing rule).
- Wedged host state can outlive a code fix: the recovery ladder is
  picker preview renders → remove widget → re-add (a widget placed
  before the configure activity existed also needs one remove /
  re-add before the long-press Settings entry appears).

**Frozen surface:** `SearchBarWidgetProvider.java` has been
byte-identical (SHA-256 `d8916d3b…`) since the v4.5.3 restoration —
its rendering path is proven on-device; do not edit it casually.
The one deliberate exception was v4.10.0 (an activity branch ahead
of its legacy shortcut handling; layout and rendering untouched).

## Install / upgrade / verify

- **Upgrade in place:** same package + same cert since v4.11.0
  means a new build installs over the old (ADB install -r lane, or
  the GitHub Release that Obtainium tracks for on-phone updates).
  The v4.11.0 package rename itself was the exception: uninstall
  first, re-add widgets, re-set preferences (panel endpoint incl.).
- **Verify what's on the phone** before claiming a version is
  live: read `dumpsys package com.jonathancruzate.metamusewidget`
  for the installed versionCode and compare to build.sh (41 for
  v4.12.2). Widget state is host state — confirm placement/render
  on-device (picker preview + home screen), never from the APK.
- Panel dark after an upgrade from a slug build (≤v4.9.0): the
  endpoint must be pasted once in Customize — by design, not a bug.
- Panel widget stale despite the refresh chain: if One UI Device
  care has the app in deep sleep, even allow-while-idle alarms are
  held until the app is opened once; the remedy is the app's
  battery setting (Unrestricted) — his tap, flag it to him.

## Workflow use

- When Jonathan wants a Muse action from the home screen, the
  answer is an **activity**: check the picker's 27 first
  (`UriCatalog`) before proposing anything new, and remember new
  activities are prompt stems or ungated surfaces — never a new
  `hatch://` deeplink.
- When he reports the panel/widget stale or wrong, work the lane
  in order: endpoint configured? → brief push landing? → Worker
  merge healthy? → app fetch/cache? → host refresh state? Most
  historical failures were host-side (render break, wedged state,
  deep sleep), not data-side.
- Every build/change upstreams in the same handling: source push
  to `jonathancruzate/meta-muse-widget` (the `github-export/` tree
  is the export mirror — reconcile it, don't copy blindly; it has
  diverged before), GitHub Release where it ships one, BUILD-REPORT
  note, and this skill if the procedure changed.

## Pointers

- `BUILD-REPORT.md` — the full version-by-version record (URI
  catalog §, the v4.3.2 bisect, every guard's origin).
- `refs/REA-DEEPLINK-FINDINGS.md` — the TrustedCaller gate proof.
- `refs/REA-RENDER-DIFF-431.md` — the plain-View root cause.
- `refs/LIVE-DATA-LANE-DESIGN.md` — the panel data contract (§5).
- `refs/PIXEL-FINDINGS.md`, `refs/REA-G-COLOR-ENGINE.md`,
  `refs/REFS-FINDINGS.md` — the decomposed One UI / Pixel values
  the styling is built from.
- `github-export/README.md` — the public-facing release notes
  (carries the v4.12.2 note).


## Muse Launcher Pack (v5.x, built 2026-11-12)

The app is now the widget-pack layer of the Muse Launcher
(design + execution record: the companion-layer goal's
`Muse-Launcher-Pack-Design.md`; theme kit beside it). One UI
Home stays the launcher; the pack adds:

- **Providers:** MuseOmnibarWidgetProvider (search/today/
  voice pill), MuseTileWidgetProvider (one provider,
  per-instance kind via TileConfigureActivity; kinds: brief,
  up_next, house, transit, media, voice, decide, clock,
  money, people, sleep, setup), MuseStackWidgetProvider
  (4x2 Context Stack cards), MuseGridWidgetProvider
  (favorite apps), MuseHouseWidgetProvider (House Control:
  verified state + DND/media/voice/today through Device
  Assistance). The three pre-pack widgets are unchanged; the
  search-bar surfaces are FROZEN (baseline hashes in
  BUILD-REPORT v5.0.0 entry; note the provider hash moved in
  v4.10.0, so the v4.5.3-era recorded hash is stale).
- **Activities:** MuseSearchActivity, MuseGridActivity
  (App Pairs sequenced via DA app_launch when paired),
  HouseActivity, SetupWizardActivity (self-checking setup +
  drift report via PackDrift), PackVoiceActivity.
- **Feed bundle v2:** the panel lane now also serves
  up_next / transit / money distillates (Worker feed_version
  2; producer panel-brief-push.py). Tiles degrade to designed
  empty states when a feed is absent. Money has no producer
  yet (no finance distillate in the brief snapshot).
- **Device Assistance local channel:** DaClient ->
  ContentProvider call() on DA's LocalChannel (cert-verified
  + pairing token; see the Device Assistance skill). Pack
  prefs live in PackPrefs (file "muse_pack"), NOT the frozen
  WidgetPrefs.
- **Live Updates:** LiveUpdateFeeder posts the leave-by
  countdown as an ongoing notification (promoted ongoing on
  API 36+, requested reflectively because the vendored
  android-36 stub predates the Builder method).
- **Sleep tile:** framework Health Connect read of last
  night's sessions (API 34+), on-device only, grant is the
  user's in Health Connect.
- **Build law additions:** NO lambdas anywhere in this app
  (javac runs -bootclasspath + source 8; LambdaMetafactory is
  absent and the build fails on any lambda). Compile jar is
  android-36 since v5.1.0. The render + no-secrets guards
  cover every new layout/provider automatically.
