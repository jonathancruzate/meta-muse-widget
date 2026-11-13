# One UI 9 Meta Muse Widget — v4.0.0 build report

Built 2026-10-25 from the uploaded `OneUI9-MetaMuse-Widget__2__6_c2x8.apk`
(original: v3.0.0, versionCode 4, a single 4×1 search-bar widget that only
launched the Muse app). Full source: this folder (`src/`, `res/`,
`AndroidManifest.xml`, `build.sh` — rebuilds end-to-end with the vendored
Android SDK in `tools/`).

**Output:** `OneUI9-MetaMuse-Widget-4.0.0.apk` — 88 KB, package
`com.samsung.oneui9.metamusewidget`, versionCode 5, minSdk 24, targetSdk 35,
SHA-256 `af53e78ae095c471fe614073e98c474411a10b5cbc576e924641e049e1edf961`.

## What changed

- **The original widget, restyled to the full One UI spec** (see §8 below):
  glass pill search bar (One UI 9 glass values), Muse logo, "Ask Muse"
  hint that now opens **Muse search** (`hatch://search`), and a circular
  40dp **+ control** that starts a **new thread** (`hatch://thread/new`).
- **New 4×2 shortcut grid widget**: One UI 9 focus block (18dp) with 8
  medium-card cells (14dp) — Chat, New thread, Search, Feed, Explore,
  Goals, Library, Status.
- **New hub app** (the launcher icon): every Muse destination in grouped
  focus blocks, filter-as-you-type search, a bottom "N destinations"
  counter, and an empty state with an action.
- **A router with a fallback chain** for every tap: URI pinned to the Muse
  package → URI to any handler → the entry's web/app-link fallback → a
  plain Muse launch (the original's package list, kept) → Play Store →
  the original's "not found" toast.

## The URI catalog (70 destinations)

Discovered from the Muse app itself, not guessed:

- Muse APK **13.0.0.4.159** — the exact build installed on Jonathan's
  Galaxy S24+ (device `app.info`) — pulled from APKMirror per the standing
  mirror rule. SHA-256
  `405f562771f0c2c5ba570ee918f82d2aa8a6aa3304d8cc348e6606b6186d2fdc`.
- **Manifest** (`AuraDeeplinkHandlerActivity`): scheme `hatch` (any
  host/path); verified https app links for `agent.meta.ai`,
  `applink.muse.ai`, `muse.ai`, `www.muse.ai` (http too for muse.ai);
  the web-auth route `/account_link_auth`.
- **Code** (`AuraDeeplinkRouteMapper`, 44 route specs + the DEX literal
  templates): `hatch://chat` (+`?approval_id=`, `?message_id=`, `?prompt=`),
  `hatch://thread/main|new|<id>`, `hatch://search?q=`, `hatch://feed`
  (+`?unit_id=`), `hatch://explore`, `hatch://goals`, `hatch://library`
  (+`?tab=artifacts|media`, `/item?id=`), `hatch://spaces`,
  `hatch://space/<id>`, `hatch://memory` (+`?path=`), `hatch://status`
  (+`?tab=activity|tasks|identity`), `hatch://activity/<id>`,
  `hatch://task/<id>`, `hatch://settings` (+`?section=`, verified value
  `subscriptions`), `hatch://settings/devices`,
  `hatch://device_permissions`, `hatch://devices/repair`,
  `hatch://devices/invoke/<command>` (+ literal `invite.upsell`),
  `hatch://home_link_order`, `hatch://invite_friends` (+`?src=`),
  `hatch://join` (+`?code=`), `hatch://invite`, `hatch://voice`,
  `hatch://home`, `hatch://connectors`, `hatch://connect`,
  `hatch://disconnect`, `hatch://email_setup`, `hatch://foa_ac_link`,
  `hatch://node-card?<params>`, `hatch://peer/accept#<token>`,
  `hatch://s/<code>`, `hatch://mhl`, `hatch://subscription_hub?page=paywall&entry_point=chat`,
  and the applink.muse.ai path forms (`/ideas`, `/subscriptions`,
  `/buy_usage`, `/redeem_invite_code`, `/enable-wearables`,
  `/connect/channel`, `/connect/browser-credentials`, `/connect/complete`,
  `/stripe_link/connect/complete`, `/api/hatch/oauth/callback`).
- **Docs**: `hatch://search`; muse.ai web routes (`/`, `/subscriptions`,
  `/terms`, `/privacy`) and the download page.

Template rows (needing an `<id>`/`<token>`) display the template and launch
the base screen. Flow endpoints (OAuth callback, connect-complete,
account-link auth) are included for completeness — they open Muse but are
meant to be landed on mid-flow.

Exposed but **not** launchable URIs (documented, not hub rows): the Wear OS
data path `wear://*/aura/watch/v1/`, the share targets (ACTION_SEND /
SEND_MULTIPLE, PROCESS_TEXT), and the FileProvider authority
`com.facebook.aura.aura.fileprovider`. `tel:`/`smsto:` in the manifest are
`<queries>` (what Muse looks up), not routes it exposes.

## §8 verification (One UI Dashboard Standard)

1. **Spec sections, applied / not-applicable**
   - §1 Architecture — applied: viewing area on top (36sp title +
     subtitle), interaction below (search pill, then groups); 24dp side
     margins; widgets use the home-screen forms from the kit's System UI
     page (4×1, 4×2); text in sp so it follows the system font scale.
   - §2.1 App bar — N/A as a toolbar (the hub is a first-depth screen with
     an expanded title block and zero action buttons; the ≤3 rule holds
     trivially). §2.2 Expandable app bar — not applied: the list is short
     and the spec exempts cases where collapsing would cut content.
     §2.3 Bottom bar / §2.4 Bottom navigation — N/A: single-screen,
     browsing-first hub; no tab set. §2.5 Buttons — applied: contained
     surfaces only, never mixed with flat on one screen. §2.6 Slider —
     N/A. §2.7 Dialogs — N/A (no dialogs). §2.8 List — applied: group
     subheaders, row titles ≤1 line, bottom counter. §2.9 Search —
     applied: suggestions-by-filtering before/while typing, IME search
     opens Muse search. §2.10 Progress — N/A (no async loads).
     §2.11 No-items — applied: empty state text plus an action.
     §2.12 Label toast — applied as content descriptions on every
     icon-only control. §2.13 Action toast — N/A (the failure toast is
     text-only, no close-only action). §2.14 Navigation bar — applied:
     opaque, colored to the ground. §2.15 Edit mode / §2.16 Selection —
     N/A (nothing editable or multi-selectable).
   - §3 Visual — applied (tokens verified below): spec palette by
     category, Roboto (system sans), sentence-case labels, modular
     geometric icons in one palette, §9 radii (18/14/pill).
   - §4 Motion — applied where a widget can: instant, tangible response;
     the trampoline activity uses zero-duration transitions; no custom
     animation exists to conflict with reduced-motion.
   - §5 Auditory — silent by default, N/A.
   - §6 Accessibility — see spot-check below.
2. **Token check from the compiled APK** (`aapt2 dump resources`):
   primary `#FF0381FE`; ground `#FFFAFAFA` / night `#FF000000` (smoked
   black); glass `#B8FFFFFF` light / `#C7121214` night (= rgba(18,18,20,.78),
   the §9 glass value); card radius 18.0dp in the compiled drawable;
   fontFamily `sans` (Roboto on Samsung). Kit facts cross-checked
   (Jonathan's Figma copy, fileKey sweWqNrwx46BkcfhOr7jdw): dark
   container `#17171A`, Title 36, sentence-case labels, widget sizes.
   One deliberate spec-over-kit call: the kit's own primary `#387AFF` was
   NOT used — the spec/standard value `#0381FE` governs.
3. **Element check** — from compiled layouts/widget-info (no emulator or
   install lane exists in this environment, so no rendered captures —
   stated plainly rather than faked): search widget 250×60dp, target
   cells 4×1, category home_screen|searchbox, resize horizontal; grid
   widget 250×120dp, 4×2, resize both; hub rows ≥56dp; counter present;
   empty state present.
4. **Accessibility spot-check** — contrast: `#2E2E30` on `#FAFAFA`
   ≈13:1, `#FCFCFF` on `#000` ≈15:1, secondary `#76767A` on `#FAFAFA`
   ≈4.6:1, `#848487` on `#000` ≈5.9:1 (small-text minimum 4.5:1 met);
   status is never carried by color alone (icon + text + chevron on
   every row); focus order follows layout order; 200% text scaling is
   respected (sp units, wrap heights, min-height rows).
5. **Package checks** — `apksigner verify` passes (v2+v3), `zipalign -c`
   passes, `classes.dex` at the APK root (a first build put it under
   `dex/` — caught by this verification and fixed), all 82 catalog URI
   literals confirmed present in the built DEX, manifest parses with both
   widget receivers and both activities.

## v4.1.1 addendum (2026-10-25, same night) — the URI failure, diagnosed and fixed

Jonathan installed v4.0.0 and reported: every URI fails; only opening
the app works. Diagnosis from the Muse APK's own code (handler +
validator smali, kept in this folder):

- All v4.0.0 taps went to `AuraDeeplinkHandlerActivity`. Its
  `AuraDeeplinkAccessValidator.validate()` resolves the route, reads
  the route's access scope, and for restricted scopes runs a
  trusted-caller check (`LX/CRT.A00`) that throws SecurityException
  for callers outside Meta's family/config patterns → result DENIED →
  the handler logs "untrusted caller, rejecting deeplink" and drops
  the payload. Other routes are matched against server-driven caller
  patterns ("unified_security_deep_link" config). A third-party
  widget cannot make itself a trusted caller — that part is not
  fixable from outside, and v4.0.0's fallback chain never engaged
  because a dropped payload is not an exception.
- The handler's success path is an explicit intent to
  `AuraMainActivity` (exported, MAIN + ASSIST) carrying the original
  action/data/extras — `launchMainActivity`. v4.1.0's Router now
  replicates that internal hop as its primary route for every URI
  (explicit component + ACTION_VIEW + data, NEW_TASK | CLEAR_TOP),
  then falls back to the gated handler, any-handler VIEW, the web
  fallback, a plain launch, the Play Store, and the toast.
- Honest limit: this cannot be device-verified from here (the adb
  lane is retired). If Muse's main activity re-validates inbound
  data itself, restricted routes may still land on the home screen —
  the first tap on his phone is the test.

Other v4.1.0 changes:

- **Search-bar tap** now opens a new thread (`hatch://thread/new`)
  via the direct route — the compose surface, keyboard up — per his
  standing "type into main chat" ask.
- **The right-hand button is a customizable shortcut**: default Muse
  search; settable from the hub's new Widget settings card to any
  catalog destination, an Assistant entry (ACTION_ASSIST on
  AuraMainActivity, new catalog row), any custom URI, or **None**,
  which hides the button (his "otherwise remove it" branch).
  PendingIntents also got unique per-destination data URIs —
  PendingIntent matching ignores extras, so same-code buttons could
  previously merge.
- **Theme options** (hub Widget settings → Color theme): "One UI
  (spec palette)", "System palette (Material You / Samsung)", or
  "Stephany (muted pink)". Per Jonathan's direction the chosen color
  is applied to the accent elements — the Muse logo's backing circle
  and the shortcut control (plus grid cells and hub accents); the bar
  itself stays One UI glass in every mode. System mode reads the
  framework system colors (`system_accent1_*`, API 31+), i.e. the
  device's wallpaper-based Samsung color palette, light and dark;
  below API 31 it falls back to the spec palette. The Stephany theme
  is the muted pink of Stephany Garces's sweater (the one from when
  they first met; photo supplied by Jonathan 2026-10-25): the accent
  `#B46C5D` is sampled from the sweater knit itself (median of lit
  patches), soft `#EFDBD6`, strong `#96544A`, with dark values
  `#C98374` / `#4A3532` / `#DCAFA4`. Naming note: the option is named
  for **Stephany** — "Stef" is Muse's own name; Jonathan corrected an
  early build that conflated them, and the rename was applied through
  the code (resources, drawables, constants), not just the label.
- Signed with the same key as v4.0.0, so v4.1.1 installs **over**
  v4.0.0 in place (no uninstall needed this time). SHA-256
  `8fb06c4252924cbcba4b8e2faf2cd1934e99fe7f3e194d88ec38d9322709e78e`.

## v4.5.3 note (2026-10-28) — the bisect lands: the logo hit-view was the break

Jonathan's on-device bisect settled it: **v4.3.1 works** (he
reinstalled it and sent the APK back; its SHA-256 matches the
local build, `eef95c23…`). A full binary diff of v4.3.1 vs
v4.3.2 — same APK entry list throughout — isolates the entire
widget-path delta to two moves, both from the logo-tap change:

1. **Layout:** `widget_search_bar` gained a plain transparent
   `View` (`iv_logo_hit`, 52×48dp) overlaid on the logo.
2. **Provider:** `setContentDescription` +
   `setOnClickPendingIntent` aimed at that new view.

Everything else in v4.3.2 is in-app only (the Stephany theme
becoming a hidden option + its 5-tap unlock) or resource-ID
renumbering; `BarStyle`, `Router`, `WidgetPrefs`' widget reads
are untouched. So the v4.5.2 rollback aimed one version too
late — v4.3.2 was already broken, and every build since has
carried the overlay. Which of the two moves throws inside One
UI Home's host is not separable from outside, and doesn't need
to be: both are removed.

**The fix — restore the v4.3.1 widget core, keep the feature.**
The layout is back to v4.3.1 exactly (compiled-layout diff
against the v4.3.1 APK, normalized for ID numbering, is empty).
Logo-tap-to-open-Muse survives, implemented the way v4.3.1
already proved on-device: the click intent goes on
`iv_muse_logo` itself — an ImageView with a router
PendingIntent, the same pattern as the shortcut glyph — instead
of on a separate overlay view. `ic_open` / `ic_assistant` are
restored as the vector drawables v4.3.1 shipped (reconstructed
from its APK: `@color/primary_dark` fill, original path data);
the v4.5.1 raster PNGs of them are removed. The provider is
otherwise v4.5.2's, including the `bar_style_<id>` scrub; the
settings page, QS tile, shortcuts, and the in-app Pixel
rendering are unchanged.

Verification: aapt2 badging shows versionCode 28 / 4.5.3,
launchable activity unchanged; a jadx audit of the shipped dex
finds no `iv_logo_hit` anywhere and the logo click on
`R.id.iv_muse_logo`; `resources.arsc` carries no `iv_logo_hit`
either. Same signing key; installs over v4.5.2 in place.
SHA-256
`955333ed833cc3c2aea5eedaa0e4ae949acbbc7a8cc8bf4a7d1820ff65d28f62`.

## v4.5.2 note (2026-10-27, evening) — swarm rollback to the v4.3.2 widget core

v4.5.1 did not fix the widget either. Per Jonathan's direction
("use the swarm and use online dev resources") plus his timeline
fact — **the last version that worked well was before the Pixel
theme was added** — an Agent Swarm Mode B council (Architect /
Skeptic / TPM) deliberated over a full binary diff, and a
research pass covered the Android docs, Samsung's widget guide,
and AOSP host sources. What the evidence established:

- **v4.3.2 → v4.4.0 binary diff:** layout XML, widget_info,
  manifest, APK file list, and every compiled resource VALUE the
  widget references (colors, strings, the pill drawables, light
  and night) are identical. Only dex code changed — the Pixel
  branches. The v4.5.1 vector theory is therefore **retired**:
  the working v4.3.2 shipped the same layout with the same
  vectors. (The rasterized PNGs stay; they are harmless.)
- In One UI style the v4.4.0 update path is functionally the
  v4.3.2 path — every Pixel branch is gated on a `bar_style`
  preference. The one state that can route an old placed widget
  into the new code is a **per-widget `bar_style=pixel`
  override**, written while Pixel was being tested and persisted
  by widget id across updates (v4.3.2 had no such key).
- Research: the AOSP picker preview is pure host-side inflation
  (no app code), and `AppWidgetHostView` substitutes the error
  layout when an update's RemoteViews fail to inflate — so a
  wedged host-side state from the 4.4.x failure can outlive code
  fixes, which is why the delivery pairs this build with a
  remove / reinstall reset ladder, picker preview as the gate.

**The fix — maximal rollback, by construction.** The widget
provider's update is restored call-for-call to v4.3.2 (verified
by diffing the decompiled v4.5.2 provider against v4.3.2's):
logo = accent `setColorFilter`, hint = the original
`hintColor(ctx)`, shortcut = resource + filter, pill = the
glass render with no style branch and no fallback path. The
v4.5.0 try/catch + `fallbackViews` and the v4.4.0/v4.4.1 Pixel
widget machinery (`shortcutCircleBitmap`, the dynamic-logo
bitmap in updates, `flatPillBitmap`, `setShadowLayer` plate)
are deleted or unreachable — a grep of the decompiled provider
finds no `isPixel`, no `getIdentifier`, no bitmap call except
the pill background. `WidgetPrefs.barStyle(ctx, id)` now
ignores per-widget overrides, and the provider scrubs any
stored `bar_style_<id>` key on every update. Pixel rendering
survives ONLY in-app (the ask page), quarantined until it can
be re-proven in isolation; the settings page, QS tile, and
shortcuts are unchanged from v4.5.1.

Same signing key; installs over v4.5.1 in place. SHA-256
`494f30981c2d8a028384324d2fbb62c6b06dc76f0917696d8756540bd0c6abfc`.

## v4.5.1 note (2026-10-27) — the widget render fix + the app label

Jonathan's follow-up screenshots localized the "Couldn't add
widget" failure properly: **the widget picker's own preview of
the search bar shows the same error**. The preview is inflated
by the host (One UI Home) from the layout alone — no app code
runs — so the fault lives in what the layout asks the host to
inflate. Diffing against the original widget (which shipped
working): it contained **zero vector drawables**; ours had
vector drawables as the shortcut glyph in the RemoteViews
layout (and the provider set vector resources into the update
as well) — a documented RemoteViews hazard, since the host
process has no vector-compat inflater. Fix: `ic_open`,
`ic_assistant`, and `ic_plus` are rasterized to PNGs (glyph
polygons rendered at 32x and downscaled, filled with
primary_dark so every existing tint behaves identically), and
the vector XMLs are deleted. Nothing else in the layout is
non-bitmap: pill = shape/PNG, logo = PNG.

Also fixed: the app had renamed itself "Customize" — making
the settings activity the launcher in v4.5.0 carried its
activity label into the launcher and the widget picker group
header. The activity label is now the app name ("Muse
Widget", verified in the built APK's launchable-activity);
the page still titles itself Customize in code.

Same signing key; installs over v4.5.0 in place. SHA-256
`378806b96ff6716c87a4c5d586153772b436df329d66387c7c0c886a6e3f9171`.

## v4.5.0 note (2026-10-27) — the app is now just the settings page

Jonathan's verdict after a morning with v4.4.x, in his words:
the picked widget "is not usable because of uri and unneeded",
the color-theme option and Customize were redundant, theming
should reach the bar's left logo too, there was no settings
option on the widget's long-press menu — and his proposal:
"the app itself just be the settings like the google search
bar setting", "this is the only page we need". All of it lands
here:

- **The app is the Customize page now.** WidgetCustomizeActivity
  is the launcher activity (global scope) and stays the widget's
  configure activity (per-widget scope from the add flow and
  the long-press pencil). The URI catalog hub (MainActivity)
  is deleted — along with its duplicate theme and shortcut
  dialogs, which were the redundancy he flagged.
- **The 4x2 action-grid widget is removed.** Its eight actions
  were the catalog URIs Muse rejects by caller signature —
  the unusable-because-of-URI widget. One widget remains.
- **Theming reaches the left logo in every style.** The dynamic
  sweep now belongs to the System theme specifically (the
  framework tones are the system palette); any other theme —
  One UI, Custom, Stephany — tints the glyph with its own
  accent, in the Pixel bar as much as the One UI bar.
- **The widget update path is hardened.** His home screen
  showed Samsung's "Couldn't add widget" error placeholder.
  The configure contract and provider metadata check out in
  code; the exposed flank was the unguarded render path, so
  pill rendering now falls back to a flat plate on any failure,
  and a failed widget update falls back to a flat-plate
  RemoteViews with the same tap targets instead of dying
  before updateAppWidget is ever called. (build.sh also cleans
  its class output now — a stale-classes build briefly shipped
  the deleted classes inside a test APK; caught by dex
  inspection before release.)
- App-icon shortcuts (Ask Muse / New chat) retarget to the
  settings activity's router; the Customize shortcut is gone —
  opening the app is Customize.

Same signing key; installs over v4.4.1 in place. SHA-256
`4da510755b7e01326973487c17d8c9e4c30bc72b4412b5e6f2ccac520dca3ed1`.

## v4.4.1 note (2026-10-27) — the dynamic M (Pixel's color engine, traced)

Jonathan's question — does the Pixel G recolor through "some
kind of color engine"? — traced into the code with REA + agy
(full evidence: `refs/REA-G-COLOR-ENGINE.md`). Verdict:

- The G ships twice with identical 17-stop sweep geometry:
  `ic_super_g_color_24` (fixed brand-hex stops) and
  `ic_super_g_dynamic_24`. `UniversalSearchInputView` swaps
  between them — dynamic when **Themed Icons** is on.
- The dynamic stops are 8 colors bound to **framework dynamic
  tones** (accent3 primary, accent1 secondary/quaternary,
  accent2 tertiary, with `android:lStar` lightness tweaks) —
  the launcher runs no engine; it consumes the scheme the
  system computes from the wallpaper. The old
  `qsbIconTint*`/`customColorBrand*` path is dead code.

Applied: in the Pixel bar style the Muse M is now rendered as
that same sweep — the decomposed 17 offsets, the same
framework sources resolved at runtime, lStar tweaks applied
through a manual Lab conversion — clipped to the glyph. The
silver-metallic read in his "Device" screenshot is what those
muted accent tones produce in dark mode. The repo also moved
this morning: `jonathancruzate/meta-muse-widget` (the "oneui9"
prefix is gone).

Same signing key; installs over v4.4.0 in place. SHA-256
`2e7882eafe47182fa9fc588f149b889c9a0efc330ab7c785f17aa9833cbeb158`.

## v4.4.0 note (2026-10-27) — the Pixel bar style (Material You)

Jonathan's mapping: "One UI theme matches One UI, Material You
matches Pixel." So the bar now has a **style**, separate from the
color theme: Customize → **Bar style** → One UI (Samsung Finder
bar) or **Pixel (Material You bar)**, global + per-widget like
everything else.

The Pixel style is a port, not an impression — decomposed from
**Pixel Launcher 17 (907)** off APKMirror
(`refs/PIXEL-FINDINGS.md`): plate drawable `bg_google_qsb_shape`
is a stadium (corner radius = the 63dp slot height) with 6dp
shadow insets; bar height 52dp (`qsb_default_widget_height` —
the same cap the One UI style already used); the themed
home-screen fill `qsbFillColorThemedHomeScreen` is not a hex at
all but the framework's **dynamic surfaceContainerLow** —
Material You proper. Applied here:

- Plate: solid stadium, fill resolved at runtime from the
  framework's `system_surface_container_low` (Material 3
  baseline #F7F2FA / #1D1B20 fallback), soft shadow, no glass
  blur, no sheen, no hairline.
- Hint: dynamic onSurfaceVariant (baseline #49454F / #CAC4D0).
- Logo: untinted — Pixel's G is multicolor, so the M keeps its
  native colors; the accent theme tints only the shortcut.
- Shortcut: a filled circle (surfaceContainerHighest tone,
  glyph baked in with its accent tint) — the circular end
  button from Jonathan's screenshot. That button itself is
  Google-app furniture (no assets in the launcher package);
  here it presents our existing shortcut.
- The ask page wears the same plate; One UI style is untouched.

Same signing key; installs over v4.3.2 in place. SHA-256
`1c20eab9ee9fd16a941158570511433c98366c291d9989a1824c870af9b661ef`.

## v4.3.2 note (2026-10-27) — the logo opens the app; Stephany goes hidden

Two of Jonathan's calls:

- **The logo on the left of the search-bar widget just opens the
  Muse app** (his words). The logo glyph was inside the bar's
  single tap target (whole bar → ask page); it now has its own
  hit area — a transparent 52×48dp overlay at the bar's start,
  wired to the Open-app action (MODE_LAUNCH) under its own
  PendingIntent (requestCode 3000+id), content description "Open
  Muse". The glyph, hint text, and bar geometry are pixel-
  identical; the rest of the bar still opens the ask page, and
  the right-hand shortcut is unchanged.
- **The Stephany theme is now a hidden option.** Its radio no
  longer shows in Customize's theme list, and the hub's theme
  dialog leaves it out too. Unlock: tap the Customize preview
  pill five times (a "Stephany theme unlocked" toast confirms);
  the unlock persists, and the option also shows whenever
  Stephany is the active theme, so an existing selection is
  never stranded. The theme itself is untouched.

Same signing key; installs over v4.3.1 in place. SHA-256
`bc909c9da77f4e13a13cee34b8b3066644d408ccbc38403f720d9a05b8bd6622`.

## v4.3.1 note (2026-10-26) — renamed Muse Widget, transparent icon

- App renamed **Muse Widget** (his correction: "Meta and Muse
  are 2 words"); APK files are `Muse-Widget-<version>.apk`.
- Launcher/round icons regenerated from his M image with the
  black keyed to full transparency (corner alpha 0 verified);
  the bar's M logo uses the same keying.
- Published as GitHub Release v4.3.1 — the release Obtainium
  tracks for on-phone updates. SHA-256
  `eef95c23d78f53094121e459aa00eccd9527272f9e35d717739090505c52e6`.

## v4.3.0 note (2026-10-26) — the surviving set (audited against Muse itself)

Jonathan's catch before building: check nothing duplicates
Muse's own features. Audit of the Muse 13.0.0.4.159 manifest
killed the selection-menu item (native `AuraProcessTextActivity`),
the share target (native `AuraShareIntentHandlerActivity`), and
voice (native `HatchVoiceInteractionService`). What survived —
Muse ships no widgets, no app shortcuts, no QS tile:

- **Quick Settings tile** — "Ask Muse" tile opens the ask page
  from anywhere (lock-aware via unlockAndRun; API 34+
  PendingIntent collapse path).
- **Per-widget settings** — theme, custom hue/sat, transparency,
  and shortcut are now keyed by widget ID with fallback to the
  global values. Configuring a specific widget (long-press →
  Settings) edits that widget only; the hub edits the defaults
  new widgets inherit. Existing widgets are untouched until
  individually customized; removing a widget cleans its keys.
- **App icon shortcuts** — long-press the app icon: Ask Muse,
  New chat, Customize widget (routed through MainActivity, which
  dispatches and finishes before drawing).
- Source pushed to the public repo the same day (commit
  `77e1523`). SHA-256
  `3a4c2f06b73d5d704c64417932ef212c1d34d6bea93d7543b1cec9bba757c1ed`.

## v4.2.9 note (2026-10-26) — Google text predictions on the ask page

The ask page now pulls live predictions from Google's Suggest
endpoint (`suggestqueries.google.com/complete/search` — the
same keyless surface Chrome/Firefox autocomplete uses; verified
live before building):

- Typing (2+ characters) triggers a debounced (200ms) fetch on a
  background thread; up to 6 predictions merge under
  **Suggestions**, ahead of the local starters, deduped against
  the recents and the typed text itself. Stale responses are
  dropped by token + current-text check, so fast typing never
  shows rows for an older query.
- The app gains the INTERNET permission for this one feature —
  its first network use. Any failure (offline, timeout at 2.5s)
  silently leaves the local recents/starters standing; the page
  never depends on the network.
- Source pushed to the public repo the same day (commit
  `b7912ed`). SHA-256
  `85b8ccf2386ae5c0afc353bde7864b32f8df7eea20558efac025bf21418cc65c`.

## v4.2.8 note (2026-10-26) — pill docks over the keyboard

The ask page's pill stayed under the keyboard: with targetSdk 35
the window is edge-to-edge, so `adjustResize` alone no longer
lifts the layout. The page now consumes the IME window inset as
bottom padding on its root — the whole column rides up and the
pill docks over the keyboard, Finder-style (and rests on the
navigation-bar inset when the keyboard is down). Same signing
key; installs over v4.2.7 in place. SHA-256
`e298d0400108760510041161c23c6c7f14a5b18756b5b7f5664a543a4974418b`.

## v4.2.7 note (2026-10-26) — the ask page (his Finder/Google screenshots)

Tapping the widget bar no longer fires Muse directly — it opens
this app's **ask page**, modeled on the two screens he sent (One UI
9 Finder, Google search) and built to the One UI spec's search
pattern (suggestions before input, auto-complete as you type):

- Suggestions fill the viewing area: **Recent** (his own past
  asks, clock icons, a Clear-all action, capped at 12, newest
  first) and **Try asking** starters; as he types, matching
  recents (prefix matches first) and starters filter in as
  predictive suggestions.
- The input is a floating glass pill docked low on the screen
  (spec §9), rendered with the widget's own glass/accent, M logo
  at the left, his shortcut glyph at the right, keyboard up on
  arrival.
- **Enter (or tapping a suggestion) sends the text into Muse's
  composer** via the ASSIST_QUERY surface, saves it to Recent,
  and the page disappears. Empty Enter just opens the composer.
- The grid's Search cell opens the ask page too. The hub's own
  search-submit behavior is unchanged.
- Same signing key; installs over v4.2.6 in place. SHA-256
  `da79d86ddc4480bc68d17822eaaeef5b0b504b00b66b775b116173bc2f3134b4`.

## v4.2.6 note (2026-10-26) — REA verdict: the gate is cryptographic; rebuilt on the ungated surface

REA teardown of Muse 13.0.0.4.159 (full record:
`refs/REA-DEEPLINK-FINDINGS.md`). **The URIs are a dead end, and
now it's proven, not inferred:** both entry points re-run Meta's
TrustedCaller check on the sender — the handler logs "untrusted
caller, rejecting deeplink", and the main activity re-checks any
data-carrying intent and *silently clears the data* on failure
(which is exactly the lands-home behavior seen on device).
TrustedCaller verifies the caller's signing certificate against
Meta's own code-signing root CA — satisfiable only by Meta-family
apps. The handler's `public_relay` param is the designed
degradation: it opens the app *without* the payload.

**The working surface the same teardown found:** `ACTION_ASSIST`
into the main activity carries no data, so no gate — Muse routes
it to the main chat with the composer primed, and the extra
`com.facebook.aura.extra.ASSIST_QUERY` prefills the composer.
v4.2.6 is rebuilt on it:

- Search-bar tap, New chat, Assistant, Voice, Search → ASSIST.
  In the hub, typing in the search field and pressing search
  opens Muse with that text in the composer.
- The bar's shortcut is an action now (Assistant / Open app /
  Hidden) — old URI picks migrate (assist-family → Assistant,
  anything else → Open app).
- The 71-destination catalog stays as a **link library**: rows
  copy their URI with a toast explaining the gate (the links
  still work from Meta's apps and inside Muse). A note above
  the list says so. The grid widget carries actions only.
- **Widget picker preview fixed:** both widgets declare
  `previewLayout` (the picker now renders the real layout; the
  pill has a static glass stand-in drawable for previews), on
  top of the v4.2.5 preview image.
- **Long-press Settings:** the configure activity has been
  declared since v4.2.0, but a widget placed before that keeps
  its old provider info — remove and re-add the widget once
  after this update and the Settings entry appears.
- **Settings pass (One UI skill):** picker dialogs now rise
  from the bottom (spec) in both the hub and Customize; radio
  rows are 48dp; the hub explains itself in one short note
  instead of failing silently.
- Same signing key; installs over v4.2.5 in place. SHA-256
  `e54581bd7a14145858659fc450540586aa910191fd8f1ba2c949b73ef7abce07`.

## v4.2.5 note (2026-10-26) — the real blur fix + his M app icon

- **Blur, fixed properly:** a widget genuinely cannot
  backdrop-blur (RemoteViews never sees the pixels behind it), so
  the pill now composites **the launcher's own recipe**: the
  device's actual wallpaper, heavily frosted (downscale + double
  box blur), drawn under the Finder tint at its decomposed alpha.
  What shows through the bar is the wallpaper itself — over his
  photo wallpaper the pill picks up the same soft color bleed as
  Samsung's own bar. The texture is cached against the system
  wallpaper id and refreshes itself when the wallpaper changes;
  any read failure falls back to the flat glass treatment. The
  Customize preview renders the same pill. (Grid stays solid —
  spec §9: glass is the floating layer only.)
- **App icon = his supplied M:** launcher + round icons generated
  at all five densities (blue-gradient M on its native black, 80%
  width), both widgets' picker previews use it too, and the bar
  logo was regenerated from this exact file so every surface
  carries the same artwork.
- Same signing key; installs over v4.2.4 in place. SHA-256
  `28cbddadecf92b52ae5868cb8904b6d2fccde5fd5d220339e08b297554cd5f84`.

## v4.2.4 note (2026-10-26) — container geometry + glass (Jonathan's diagnosis)

- **He was right about the container:** the Finder/Google bars are
  not widgets — they are launcher surfaces in the launcher's own
  slimmer container (52dp bar), while a standard widget gets a full
  grid row and the bar used to stretch to fill it. The pill is now a
  fixed **52dp** tall, centered in the row, and the widget's minimum
  height dropped 60 → 52dp. What remains beyond that is the
  launcher's own cell padding, which no widget can remove.
- **One UI §9 glass treatment:** the pill renders as frosted glass —
  a vertical gradient (fill lightened at the top), a soft sheen wash
  over the top half, and an inner hairline just inside the outline.
  A widget cannot backdrop-blur (RemoteViews has no access to the
  pixels behind it; Samsung's own bar gets its blur from the
  launcher compositor) — this is the spec's glass look drawn the
  way a widget can draw it. The Customize preview mirrors it.
- Same signing key; installs over v4.2.3 in place. SHA-256
  `c8fc26d872759b60895e48fa9f015e304ea6d120858c9a6c1b161064b15d77bc`.

## v4.2.3 note (2026-10-26) — size + theme scope (Jonathan's correction)

- **Logo size from Samsung's own metric:** the decomposed
  `apps_drawable_start_search_icon_size` is 24dp — the M logo now
  sits at 34×24dp (was 38×27), the grid header mark at 24×17dp.
- **Theme color scope narrowed to his original rule:** it now
  affects **only the logo and the shortcut icon** (and the grid's
  icons). The bar itself — fill, 95% alpha, outline, hint — is the
  exact decomposed Finder bar in *every* theme; Stephany no longer
  pinks the bar, System no longer re-fills it, and Custom's
  hue/saturation drive the glyph tint alone. The transparency
  slider still fades the pill (opacity, not color), and the grid's
  card/cell backgrounds always stay spec styling.
- Same signing key; installs over v4.2.2 in place. SHA-256
  `6af5f361d9ff49e5947bfd0928dc62db20f86aab0ab1664c5047b752c50eb9b0`.

## v4.2.2 note (2026-10-26) — decomposed values, not estimates

The reference APKs were decomposed (One UI Home 18.0.07.59, Galaxy
Finder 11.1.00.0, Google App 17.69.20; full record in
`refs/REFS-FINDINGS.md`). Jonathan's pointer held: the Finder bar
lives in One UI Home. Its exact values now replace my sampled ones:

- Fill: #E9E9E9 light / **#3D3D3D** night (was #262628), at the
  bar's real **242/255 alpha** — `finder_search_bar_background_color`
  is #f2e9e9e9 / #f23d3d3d.
- A **0.5dp outline** (light #33000000, night #26FFFFFF) — the bar
  does have a hairline; my "borderless" read was wrong.
- Hint: #99000000 / #99FFFFFF exactly; text **14sp**; start padding 14dp.
- Confirmed matches: 32dp radius family, 52dp bar height (our pill
  is exactly 52dp inside the 60dp cell).
- Google's widget (for the Customize model): 48dp bar, #F1F3F4 /
  #3C4043 greys, near-monochrome icon tints in every theme, and a
  proto storing theme + hue + saturation + transparency — the same
  shape as ours. Their exact HSV lightness mapping stays unextracted
  (Compose ViewModel); ours remains an approximation, flagged in the
  findings doc.
- Same signing key; installs over v4.2.1 in place. SHA-256
  `e75b613d8db5f61be3d79b83053d085c4bb30fd060ef313656ccfc00e47b1a51`.

## v4.2.1 note (2026-10-26)

- **Logo fix (Jonathan's screenshot + supplied mark).** The old
  circle logo PNG is an opaque full-bleed image, so the theme tint
  filled it into a solid accent blob (the blue circle in his dark-
  mode shot). Replaced everywhere the logo is tinted — search bar,
  Customize preview, grid header — with the real Muse "M" mark he
  supplied (black background keyed out to alpha, gradient kept), so
  tinting replaces its color while the mark's shape survives: blue
  in One UI mode, system accent in System mode, dusty rose in
  Stephany mode, the chosen hue in Custom. The launcher app icon
  keeps the circle artwork.
- Same signing key; installs over v4.2.0 in place. SHA-256
  `9499e9695fe93c74514f400c27f1316b9e457f54212430f02260cc054249cf82`.
- In parallel: the Google app and Samsung Galaxy Finder APKs are
  being pulled from APKMirror to decompose how their bars and
  customization actually work; findings land in a follow-up build.

## v4.2.0 note (2026-10-26)

Google-widget-style customization, from Jonathan's screenshots of
the Google search widget's Customize screen:

- **New Customize screen** (also the widgets' configure activity,
  so it opens when a widget is added; reachable any time from the
  hub's new "Customize widget" row): live **Preview** of the bar on
  top, **Theme** presets below — One UI, System palette, Stephany
  pink, and a new **Custom** theme with **Hue** and **Saturation**
  sliders — a **Transparency** slider for the bar, and the
  **Change shortcut** row. Every change saves instantly, re-renders
  the placed widgets, and updates the preview, like Google's.
- To make transparency and arbitrary colors exact, the bar's pill
  is now **rendered as a bitmap at the widget's real size** (a
  static drawable cannot carry a slider-driven alpha); resizes
  re-render it. Light/dark keeps following the device in every
  theme, the way Google's "Device" theme behaves.
- The hub's theme dialog gains the Custom option too. The grid
  widget follows the preset themes as before (Custom color and
  transparency apply to the search bar).
- Same signing key; installs over v4.1.3 in place. SHA-256
  `8c7d82df94a69b7b69c4d1da077cbaec7b9bd332cdc55a1f0228fdd4c9c25115`.

## v4.1.3 note (2026-10-26)

The search-bar widget is remodeled on the **One UI 9 Finder bar**,
using Jonathan's own home-screen screenshot as the reference (the
Finder pill sampled ≈ #939393 on screen — a translucent light fill;
the solid equivalent is the spec component tone):

- Matte, borderless, full-round pill (28dp radius): `#E8E8EA` in
  light mode, `#262628` at night. The glass fill and stroke are gone
  from the widget (the hub keeps its glass styling in-app).
- Finder anatomy: plain glyph left, hint text, one plain action
  glyph right. The filled accent circles are gone; the theme color
  now shades both glyphs directly — the Muse logo and the (still
  mutating) shortcut icon, tinted as a matched pair in whichever
  theme is active.
- Routing, customization, and themes are otherwise unchanged. Same
  signing key; installs over v4.1.2 in place. SHA-256
  `801d62e654c78e9b62ed72e5346e8fd146227fd7855604af3b12a98e8ccb3fb1`.

## v4.1.2 note (2026-10-25, later)

- The Muse logo and the shortcut button already read as the same
  size on device (Jonathan's check) — a planned size change was
  dropped before it shipped.
- The theme color now also **shades the Muse logo itself**
  (a color filter in the theme accent, over its soft backing circle),
  so logo and shortcut read as a matched pair in the System-palette
  and Stephany modes; the grid widget's header logo gets the same
  treatment.
- The shortcut button's **glyph now mutates with the chosen
  shortcut**: it shows the destination's own catalog icon (Search →
  magnifier, New thread → plus, Goals → flag, and so on); a custom
  URI keeps the generic open glyph.
- Same signing key; installs over v4.1.1 in place. SHA-256
  `2709a8400bb0aeda537507a01ae815b3735462ab6bcdfb57d788810e6899e2e1`.

## v4.6.0 note (2026-10-28) — Stef companion layer

The companion layer on stock One UI Home, per Jonathan's pick:
no launcher replacement, Stef lives alongside One UI Home.

**What was added:**

- **StefPanelActivity** (not exported; all entry points are
  in-app) — a single first-depth screen built programmatically
  in ComposeActivity's row pattern, to the One UI standard:
  viewing area on top, focus blocks below (bg_card, 18dp
  radius), 24dp side margins, ground #FAFAFA / #000000 night,
  Roboto (sans), sentence case. Top to bottom: a time-aware
  greeting for Jonathan (Good morning / afternoon / evening,
  32sp bold) with today's date (e.g. Wednesday, October 28) as
  a secondary subtitle; an **Up next** focus block; an **Ask
  Stef** focus block (the Muse M logo row in the primary
  #0381FE tint opens ComposeActivity, a Chat row calls
  Router.assist(ctx, null), and a New chat row goes through
  Router.dispatch with UriCatalog.MODE_ASSIST — the catalog's
  existing new-thread mode; no new Muse deeplinks were
  invented, they are TrustedCaller-gated per Router.java);
  a **Home controls** focus block (SmartThings
  com.samsung.android.oneconnect and Google Home
  com.google.android.apps.chromecast.app, launched by package
  with a toast fallback when absent, plus the note that modes
  still run through Stef and these rows just open the apps);
  and the footer "Stef companion v4.6.0 — on stock One UI Home".
- **StefTileService** — a second QS tile labelled "Stef"
  (icon ic_person), copying AskTileService's pattern including
  the API 34 PendingIntent startActivityAndCollapse path and
  unlockAndRun on a locked device; opens StefPanelActivity.
  Registered in the manifest with BIND_QUICK_SETTINGS_TILE.
- **App-icon shortcut** — a third shortcut in shortcuts.xml,
  id "stef", short label "Stef today", icon ic_event.
- **Entry-point routing (chosen pattern):** StefPanelActivity
  stays exported=false. The shortcut routes exactly like the
  existing ask/new shortcuts — WidgetCustomizeActivity's
  routeShortcut() now handles widget://shortcut/stef and
  starts StefPanelActivity before drawing any UI. The QS
  tile opens the panel directly from inside the app.

**Calendar permission behaviour:** the manifest gains
READ_CALENDAR, used only by Up next. The block queries
CalendarContract.Instances for the next event in the next
24 hours (title, start time formatted with the device locale,
location when present) and refreshes on onResume. When the
permission is not granted, the block shows a "Show my next
event" row that requests it at runtime; onRequestPermissionsResult
refreshes the block. When granted and there is no event — and
on ANY calendar failure — the block degrades to "Nothing
coming up in the next 24 hours" and never crashes.

**What was deliberately NOT touched:** SearchBarWidgetProvider.java
is byte-identical to before this build (SHA-256
d8916d3b90f1d4857a4d490a377e6303d02617f9f0b25f5f6b2841640c358515,
the decompile-verified v4.3.2-equivalent provider from v4.5.2);
WidgetPrefs.java's widget update path, res/layout/widget_search_bar.xml,
and res/xml/widget_info_search.xml are likewise untouched, and
no new widget providers were added — the widget host state on
the phone is still wedged, and this build does not go near it.
No Pixel/bar-style branches in any new code, no new
internet/backend calls, and no embedded secrets or webhook
URLs in the new code.

**Verification:** build.sh ran clean; apksigner verify passes
with the same signing key as every release since v4.0.0
(cert SHA-256 34ef6099b69c3aab56a040c20b96edc403f08e40d819521950d279c5a130a899,
so v4.6.0 installs over v4.5.x in place); aapt2 badging reads
versionCode 28, versionName 4.6.0, with READ_CALENDAR among
the permissions; dexdump of build/dex/classes.dex lists
StefPanelActivity (plus its inner classes) and StefTileService.
The github-export/ copies were NOT updated: that tree is not
a trivial mirror of this source (its build.sh, widget layout,
colors, strings, provider, and catalog already diverge), so
mirroring is a separate reconciliation, not a file copy.

Same signing key; installs over v4.5.3 in place. APK
`Muse-Widget-4.6.0.apk`, 414,880 bytes, SHA-256
`36ed6d697d84e79410ef5dcc63bcd3645e824a21885429d076c4da42ef9596a9`.

## Install notes (read first)

- **Uninstall the old widget app first.** The original was signed by a
  "SamsungCommunity" certificate I don't have, so Android will refuse an
  in-place update. The new build is signed with a fresh key kept at
  `keystore/` in this folder (SHA-256
  `34ef6099b69c3aab56a040c20b96edc403f08e40d819521950d279c5a130a899`), so
  future updates from this source will install over this one. After
  uninstalling, install v4.0.0 and re-add the widgets.
- Not yet done: a GitHub commit of this source (per the standing
  always-update-GitHub rule) and an on-device install test — the adb lane
  is retired, so the install test is the first launch on the phone.

## v4.6.1 note (2026-10-28) — renamed Stef → Muse

Per Jonathan's order ("Call it Muse instead of Stef"), the
companion layer's persona name is Muse on every surface —
the thing and every surface that references it, renamed in
the same build:

- **Classes:** StefPanelActivity.java → MusePanelActivity.java
  (class renamed), StefTileService.java → MuseTileService.java;
  the old files are deleted.
- **Manifest:** activity/service declarations and comments now
  name .MusePanelActivity / .MuseTileService, labels point at
  the renamed title string.
- **Routing:** WidgetCustomizeActivity's shortcut branch is now
  widget://shortcut/muse → MusePanelActivity; shortcuts.xml's
  shortcut id is "muse" with short label "Muse today".
- **Strings (keys renamed too, stef_* → muse_companion_*):**
  title "Muse", short label "Muse today", ask row "Ask Muse",
  chat key renamed (value "Chat" unchanged), footer "Muse
  companion v4.6.1 — on stock One UI Home", the Home controls
  note now reads "Modes still run through Muse", and the QS
  tile label is "Muse". The greeting strings keep Jonathan's
  name (they never said Stef). "Stephany" — the theme, a
  different name — is untouched everywhere.
- **No behaviour change:** same panel content, same
  CalendarContract Up next logic, same Router surfaces, same
  permissions; SearchBarWidgetProvider.java is still
  byte-identical (SHA-256
  d8916d3b90f1d4857a4d490a377e6303d02617f9f0b25f5f6b2841640c358515),
  and WidgetPrefs.java, widget_search_bar.xml, and
  widget_info_search.xml are untouched.

**Verification:** grep for "Stef"/"stef" across src/, res/,
AndroidManifest.xml, and build.sh returns zero hits (this
report's dated history entries excepted); dexdump of
build/dex/classes.dex lists MusePanelActivity and MuseTileService
and no Stef* classes, and the dex contains zero "Stef" strings;
aapt2 badging reads versionCode 29, versionName 4.6.1 with
READ_CALENDAR present; apksigner verify passes with the same
signing key (cert SHA-256
34ef6099b69c3aab56a040c20b96edc403f08e40d819521950d279c5a130a899),
so v4.6.1 installs over v4.6.0 in place. The github-export/
copies were again NOT updated (diverged tree, same as v4.6.0).

APK `Muse-Widget-4.6.1.apk`, 414,880 bytes, SHA-256
`cc9fa0c5046e68ee21abfe318e8dd1813810771a452cac53bd2c1609a0d32c43`.

## v4.6.2 — verification release: render guard (2026-10-28)
REA investigation (refs/REA-RENDER-DIFF-431.md) confirmed the root cause: v4.3.2's plain `<View>` iv_logo_hit — not RemoteViews-supported — broke host inflation from 4.3.2 through 4.5.2; the swarm's 4.3.2↔4.4.0 baseline hid it. v4.6.2 = v4.6.1 + a build-time render guard in build.sh that FAILS the build if the compiled widget layout contains a plain `E: View` or `iv_logo_hit` reappears in resources.arsc. Guard passed during this build. versionCode 30, sha256 183e9c473f5d21c2610f1775b40724f0fcd7be1bf17a7d674bb9b100ba591f66, same cert; provider byte-identical (d8916d3b…); compiled layout plain-View count 0. Delivered in the companion side chat, supersedes v4.6.1 for the clean-install gate test.

## v4.6.2 — ON-DEVICE VERDICT: WORKS (2026-10-28 02:22 EDT)
Jonathan clean-installed v4.6.2: widget-picker preview renders, widget places and works. Render bug CLOSED. Root cause stands as the REA finding (plain `<View>` iv_logo_hit, 4.3.2–4.5.2). The render guard in build.sh is permanent.
- GitHub sync (2026-10-28 02:2x): export tree reconciled to the v4.6.2 main tree and pushed to jonathancruzate/meta-muse-widget main as commit 1bf8ef0 (68 text files); stray root strings.xml deleted in 3f3bd49. v4.6.2 Release + topics BLOCKED: the GitHub connector's live tool catalogue has no create_release and no topics tool — a browser pass is still owed for the Release (with the APK asset) and the topics. Binary PNGs (16: 10 mipmaps, 6 drawable-nodpi incl. new ic_plus.png) could not ride the connector's UTF-8 snapshot push and remain stale/absent remotely.

## v4.7.0 — live-data lane (2026-10-28)

The Muse panel goes live, per refs/LIVE-DATA-LANE-DESIGN.md
(Candidate A) with Jonathan's approved defaults applied: (1)
decide-kind highlights excluded in v1 — only the decide_count
number renders ("N waiting on you"), no decide titles/details;
health-adjacent items also excluded; (2) this slug-bearing build is
SIDELOAD-ONLY — the APK is not published to GitHub and the slug
never enters the github-export tree; (3) the house line shows
desired-vs-actual when out of sync, actual + report age otherwise.

**Server side (Worker `mode-sync`, bindings-preserving deploy):**
- `POST /mode-sync/panel-ingest` (POLL_SECRET): validates + caps the
  brief distillate per design §5 (≤5 highlights, decide-kind dropped
  at ingest too, title ≤80 / detail ≤140 truncated), stores KV
  `panel:brief`.
- `GET /panel-data/v1/<slug>`: merges `panel:brief` + `mode:desired`
  + `mode:actual` into the §5 contract, `Cache-Control: no-store`;
  wrong/missing slug → plain 404. New route
  `jonathancruzate.com/panel-data*` → mode-sync (live routes
  re-enumerated first — no `/panel-*` existed; nothing shadowed).
- New Worker secret `PANEL_SLUG` added via deploy.py's additive
  path; post-deploy read-back shows all pre-existing bindings
  (MODE_SYNC KV, POLL_SECRET, PHONE_PUSH_SECRET) preserved, and an
  authed mode-state read-back confirms the secrets still work.
  Neighbour probes after deploy: /mode-sync/mode-desired 403,
  /st-webhook/mode-desired 403 (legacy → mode-sync),
  /st-webhook/mode-latest 200 from st-webhook (not shadowed),
  /phone-ingress/ingress-poll 403, static / 200.
- The slug (128-bit) lives only in: the Worker secret, the VM 0600
  file `keystore/panel-slug.txt`, and this APK's generated
  PanelConfig class. Rotation = replace file + redeploy + rebuild.

**Producer:** `tools/panel-brief-push.py` reads
`~/workspace/daily-brief/dashboard_snapshot.json`, distills (section
kinds, health filter, caps; raw body never leaves the VM) and POSTs
via curl with the existing POLL_SECRET; failure exits nonzero and
must never block the brief. Dry-run verified against the real
2026-10-27 snapshot (Deepthi referral, CVS pickup, medications and
therapy items all correctly excluded; decide_count 6 carried).
Hook status: the brief lane is agent-run cron jobs with no shell
hook, so the producer ships as a runnable script — wiring is one
documented step appended to the brief compile/refresh job bodies
(see the script header). A real push + merged GET were verified
end-to-end 2026-10-28: response matches the §5 contract, house
`reported_at` passed through verbatim from the phone ack, wrong
slug → 404.

**App side:** new `PanelDataClient` (HttpURLConnection, 8 s
timeouts, 32 KB cap, last-good cache in app-private storage with
client fetch time) + generated `PanelConfig` (build.sh injects the
panel URL from the 0600 slug file; absent file → placeholder build
whose panel shows "not configured"). `MusePanelActivity` gains the
"Muse today" card under the date (headline, highlight rows, brief
date/time meta, decide counter, flat Refresh row, in-place
progress) and the house status row as the first row of Home
controls (out-of-sync shows "Away set · phone still reports Home";
age always in words, explicit stale form past 30 min; failure with
cache renders the cache labelled "Couldn't refresh · showing
<time>"; 404 shows the link-expired state, no retry loop). Fetch on
onResume throttled to 5 min + manual Refresh; no background work.
Manifest untouched.

**Guards:** build.sh gained the §8.7 no-secrets guard beside the
render guard — fails the build if source or compiled dex contains
either Worker secret's value, the MacroDroid webhook URL, or the
trigger.macrodroid.com host pattern. Both guards passed this build.

**Verification:** apksigner verify OK, same cert (34ef6099…);
badging versionCode 31 / versionName 4.7.0; permissions and
manifest components byte-identical sets vs v4.6.2 (INTERNET already
present; no new permissions, providers, or receivers); frozen
surfaces unchanged (SearchBarWidgetProvider.java
d8916d3b90f1d4857a4d490a377e6303d02617f9f0b25f5f6b2841640c358515,
WidgetPrefs.java, widget_search_bar.xml, widget_info_search.xml);
slug present exactly once in the APK dex (the generated class) and
absent from github-export, src, res, build.sh, refs, and the
manifest (grep-verified). github-export NOT updated for v4.7.0
(per default (2), the slug build's tree stays unpublished).

APK `Muse-Widget-4.7.0.apk`, 418,976 bytes, SHA-256
`c631a69ba8f021a54108ab8009c48db740a902180731421a7b685d1ec1e30b15`.
Sideload-only.
- GitHub sync for v4.7.0 (2026-10-28): export tree reconciled to the v4.7.0 main tree (build.sh, strings.xml, MusePanelActivity, new PanelDataClient.java, tools/panel-brief-push.py, refs/LIVE-DATA-LANE-DESIGN.md; README gained a v4.7.0 section; secret scans clean — slug/POLL_SECRET/PHONE_PUSH_SECRET values all absent from the export; no new PNGs). The snapshot push itself did NOT land: four push-files attempts died on service restarts and one write-approval timeout; remote head verified unchanged at cf5823fb after each attempt. Manifest (71 paths, NUL-delimited) is staged at ~/workspace/metamuse-widget/push470-paths.bin — one push-files run against github-export/ completes the sync; append the sync record only after a confirmed SHA.

## v4.8.1 note (2026-10-29) — the widget actually auto-refreshes now

Jonathan's report (02:42 EDT): "The widget doesn't auto refresh."
Diagnosis, from the code and the lanes around it:

- The panel widget's only automatic lane was
  `updatePeriodMillis` at the 30-minute floor. That delivery is
  a system hint — batched, deferred under Doze, and unreliable
  in practice for an app that isn't opened often. Every other
  lane needed Jonathan: the in-widget Refresh tap, or opening
  the panel (which pokes the widget). Between those, the house
  age, greeting, and Up Next simply froze at their last render.
- The search bar widget had `updatePeriodMillis="0"` — it never
  re-rendered on its own at all, so its provider-rendered pill
  bitmap could sit in the wrong day/night colors indefinitely.
- Server side is not the problem: the brief push is wired into
  `daily-brief-compile` (step 8, 06:30 daily) and the endpoint
  merges live house state at fetch time. The data was there;
  the widget just wasn't waking up to fetch it.

**Fix.** `MusePanelWidgetProvider` now keeps a self-scheduled
refresh chain: every update path (`onEnabled`, `onUpdate`,
`onAppWidgetOptionsChanged` — and through `onUpdate`, the
system period, `poke`, and the Refresh affordance) arms the
next refresh ~15 minutes out via
`AlarmManager.setAndAllowWhileIdle` (`ELAPSED_REALTIME_WAKEUP`),
delivered as the provider's own `ACTION_REFRESH` broadcast;
`onDisabled` cancels it. Re-arming reuses one PendingIntent
(request code 6000), so alarms never stack. The 30-minute
`updatePeriodMillis` stays as the platform backstop.
`widget_info_search` goes 0 → 1800000 so the search bar
re-renders on the platform cadence too (its provider and
layout are untouched). `PanelDataClient` UA bumped to
`MuseWidget/4.8.1`.

**Verification:** badging versionCode 33 / 4.8.1; a jadx audit
of the shipped dex shows the chain (`REFRESH_INTERVAL_MS`
900000, `setAndAllowWhileIdle`, three arming call sites);
both compiled widget infos read `updatePeriodMillis=1800000`;
render guard and no-secrets guard both passed;
`SearchBarWidgetProvider.java` byte-identical to the frozen
hash (`d8916d3b…`). Slug injected from the local file —
sideload-only, not published, same as v4.7.0/v4.8.0. Same
signing key; installs over v4.8.0 in place. SHA-256
`2ba9092d52128827bdd0735b4136789753a14ac3c0b88b1c96a7a4ec716a7204`.

One honest limit: if One UI has put the app into deep sleep
(Device care), even allow-while-idle alarms are held until the
app is opened once — no build can override that from inside;
the remedy is the app's battery setting (Unrestricted), which
was flagged to Jonathan with the delivery.

## v4.8.0 note (2026-10-28) — the panel is a widget now

Jonathan's ask, verbatim intent: why not a widget that expands
dynamically up to full screen on the home screen, matching
One UI 9 and Material You. So the panel is now ALSO a widget —
a new provider beside the frozen search bar; the panel activity
is unchanged except one additive poke (below).

- **New `MusePanelWidgetProvider`** + `widget_info_panel.xml`:
  resizable both directions (resizeMode horizontal|vertical,
  compiled 0x3), target 4×2 cells, min 180×100dp, min-resize
  110×52dp, `updatePeriodMillis` at the 30-minute floor
  (1800000, compiled-verified). No configure activity.
- **Size-adaptive densities** — compact (headline + house line),
  medium (+ brief meta, 3 highlights, decide counter, Refresh),
  full (greeting/date header, 5 highlights, decide counter,
  house block, Up Next, Refresh). On API 31+ the three ship as
  one responsive RemoteViews with SizeF breakpoints (170×120
  medium, 250×240 full); below 31 the provider picks the
  density from the widget options and
  `onAppWidgetOptionsChanged` re-picks on resize.
- **Same live data, same rules** as the v4.7.0 panel, via the
  shared `PanelDataClient` fetch+cache: every update renders
  the last-good cache immediately, then `goAsync`-fetches and
  re-renders; the widget never blocks on the network. New
  `PanelWidgetData` shapes the JSON into widget strings with
  the activity's semantics (brief age stated, house age in
  words with the explicit stale form, decide = count only).
  Refresh lanes: the 30-min period, the in-widget Refresh
  affordance (broadcast `ACTION_REFRESH` back to the provider),
  and `MusePanelWidgetProvider.poke()` — called by
  MusePanelActivity after each fresh fetch, so panel and
  widget stay in step. Up next (full density) renders only
  when READ_CALENDAR is already granted; without it the
  section is gone, silently — a widget cannot ask.
- **Styling — One UI 9 first, Material You blended:** the
  card is the spec focus block (18dp radius, app `container`
  fill, day/night through values-night) in
  `bg_panel_widget`; on API 31+ `drawable-v31` /
  `drawable-night-v31` variants swap the fill for the Material
  You system neutral tones (`system_neutral1_50` /
  `system_neutral1_800`), and the accent (Refresh text, logo
  tint) resolves at render time from the framework dynamic
  accent (`system_accent1_600`, name-resolved and SDK-guarded)
  with spec primary #0381FE as the below-31 fallback. Padding
  scales with density (14/16/20dp); Roboto/system font,
  sentence case throughout.
- **Guards:** the render guard now scans EVERY widget layout —
  the union of every layout referenced by any
  `res/xml/widget_info*.xml` and every `res/layout/widget_*.xml`
  — and requires every element to be a RemoteViews-supported
  class. This build's guard output lists all four:
  widget_panel_compact, widget_panel_full, widget_panel_medium,
  widget_search_bar. Negative-tested: a plain `<View>` injected
  into the compact layout fails the build with exit 1, naming
  the file and element; the layout was then restored and the
  final build is clean. The no-secrets guard passes unchanged.
- **Frozen surfaces byte-identical:** SearchBarWidgetProvider
  `d8916d3b…515`, WidgetPrefs `14b9dfff…`, widget_search_bar
  `8d9fa2ce…`, widget_info_search `eab062f1…`. Manifest delta
  vs v4.7.0 (compiled counts): activities 4→4, services 2→2,
  receivers 1→2 — exactly the one new panel-widget receiver
  (which also carries the refresh-action intent-filter).
  Permissions identical to v4.7.0; INTERNET + READ_CALENDAR
  were already declared.
- **Verification:** apksigner verify passes, same cert
  (34ef6099…); badging versionCode 32 / versionName 4.8.0;
  compiled widget_panel_full contains only LinearLayout (8),
  TextView (21), ImageView (1) — all RemoteViews-supported;
  dex carries MusePanelWidgetProvider + PanelWidgetData.
  As ever, no emulator/install lane exists here, so no
  rendered capture — the on-device check is Jonathan's first
  add of the "Muse panel" widget. Slug posture unchanged:
  this build's slug came from the same local 0600 file;
  sideload-only, not published.

Same signing key; installs over v4.7.0 in place. APK
`Muse-Widget-4.8.0.apk`, 431,826 bytes, SHA-256
`e06212e748a85cbaa2a73fca29e15543402b27579cb4736e964e2d61a3189d20`.
- GitHub sync for v4.8.0 (2026-10-28): export tree reconciled to the v4.8.0 main tree (AndroidManifest, build.sh with the extended render guard, strings.xml, MusePanelActivity poke, PanelDataClient, new MusePanelWidgetProvider + PanelWidgetData, widget_panel_compact/medium/full layouts, widget_info_panel.xml, bg_panel_widget in drawable + drawable-v31 + drawable-night-v31; README gained a v4.8.0 section). Snapshot push landed first try: commit b1609e0d51ab0a3229463fa60537cac207b18703 on main, 80 paths, read-back verified (MusePanelWidgetProvider + PanelDataClient + PanelWidgetData + panel layouts present on main; blob SHAs match local). Secret scans clean — panel slug, POLL_SECRET, PHONE_PUSH_SECRET values all absent from the export; panel-data URLs appear only in <slug> placeholder form. No new PNGs in v4.8.0 (nothing for the binary lane). No release created — the browser pass owns releases.

## v4.9.0 note (2026-10-29) — the activity picker, rebuilt on prompts instead of deep links

Jonathan's idea (10:13 EDT): bring back the activity picker,
but do it the way the Muse app itself does Goals and Tracking —
prefill the text ("I want to start a goal about ", "I want to
start tracking ", …) instead of firing the deep links we can't
use. The 71-destination catalog's `hatch://` routes stay gated
by Meta's TrustedCaller signature check (REA-DEEPLINK-FINDINGS),
so the picker never fires a URI: each row opens the ask page
with its prompt stem already typed, cursor at the end, and
Enter carries the finished sentence into Muse through the
ungated ASSIST_QUERY surface — the same lane the bar tap uses.

**What's in it.** A new `PickerActivity`: one first-depth
screen in the panel's programmatic pattern — grouped sections
(Start / Tabs / Status and account / Devices / Connect), icon
rows with the stem previewed on a second line (a trailing-off
stem shows "…"), and the One UI list bottom counter ("27
activities"). Curation lives in `UriCatalog`
(`pickerAction` / `pickerPrompt` / `pickerEntries`): 24 rows
carry stems — Goals → "I want to start a goal about ",
Upcoming tasks → "I want to start tracking ", Explore → "Show
me ideas about ", Connect a service → "I want to connect ",
Memory → "What do you remember about ", and full-sentence rows
like Feed → "Catch me up on my feed" that can be sent as-is —
and 3 rows fire their ungated surface directly (Voice and
Assistant → assist; Open Muse app → plain launch). The
link-plumbing groups (Link types, Web) and every ID-template
row stay out: they were never activities.

**Entry points.** The Muse panel's Ask Muse card gains an
"Activities" row, and a fourth app-icon shortcut ("Activities",
`widget://shortcut/activities`, routed in
WidgetCustomizeActivity) joins Ask / Muse today / New chat.

**Ask page.** `ComposeActivity` learns `EXTRA_PREFILL`: the
stem is set before the text watcher registers, so it doesn't
trigger the suggestion flow — the page opens on the plain
recents/starters list with the stem typed and ready to finish.

**Verification:** badging versionCode 34 / 4.9.0; jadx audit
of the shipped dex shows `PickerActivity` wired to
`EXTRA_PREFILL` and `UriCatalog` carrying both example stems
and `pickerEntries`; render guard and no-secrets guard both
passed; `SearchBarWidgetProvider.java` byte-identical to the
frozen hash (`d8916d3b…`); no widget layout or info touched —
the v4.8.1 refresh chain is unchanged. `PanelDataClient` UA
bumped to `MuseWidget/4.9.0`. Slug injected from the local
file — sideload-only, not published, same as v4.7.0–v4.8.1.
Same signing key; installs over v4.8.1 in place. APK
`Muse-Widget-4.9.0.apk`, 435,922 bytes, SHA-256
`fb6430bab6a05afe733f90cafd75e1f43a83e1f0caa9b8acdd18443584ceafa4`.

## v4.9.1 (2026-10-31) — panel endpoint becomes user-configurable; build-time slug injection REMOVED

Jonathan's call (2026-10-31 10:49 EDT): instead of splitting the panel into a private app, the public app keeps the panel and every user configures their own endpoint. The private-app split was cancelled the same morning (a scaffold at ~/workspace/muse-panel/ was left untouched/unbuilt — no keystore, no APK).

- **Removed:** the build.sh panel-config section (PanelConfig generation from keystore/panel-slug.txt) — deleted. No PanelConfig class exists in source, build, or APK. build/gen is now wiped at build start so a stale generated class can never ride along.
- **PanelDataClient** reads the endpoint from SharedPreferences via new WidgetPrefs accessors (`panel_endpoint_url` key): `panelEndpoint()` / `setPanelEndpoint()` / `clearPanelEndpoint()`. The pasted URL is treated as an opaque whole; fetch/cache/refresh/age behavior unchanged from v4.7+. `isConfigured()` now takes a Context (call sites: MusePanelActivity ×2, MusePanelWidgetProvider ×2). UA bumped to MuseWidget/4.9.1.
- **New setting** in WidgetCustomizeActivity: "Panel data endpoint" row — Set endpoint opens a bottom-anchored paste dialog (http/https only); after save the row shows only the configured state ("Configured · <host>"); Clear removes it. Saving/clearing pokes MusePanelWidgetProvider so widgets light up (or go dark) immediately. The panel activity picks the URL up on its next resume fetch.
- **Not configured state:** panel_not_configured string now reads "No panel endpoint set — add yours in Settings → Panel data endpoint."; panel_link_expired re-pointed the same way.
- **Guards:** render guard passed (search bar + all three panel density layouts). No-secrets guard extended: the RETIRED slug's value is now itself a checked secret (dex + src + res + manifest), and the dex is pattern-checked for "panel-data" / "PanelConfig" — both guards passed.
- **jadx audit of the shipped dex:** PanelConfig absent; panel classes (MusePanelActivity, MusePanelWidgetProvider, PanelDataClient, PanelWidgetData) and PickerActivity present; WidgetPrefs carries panel_endpoint_url; ZERO occurrences of the personal domain anywhere in decompiled code. Slug value occurs in 0 files across the entire unzipped APK payload. ("panel-data" appears once in resources.arsc — the settings dialog's generic help text, not a URL.)
- **Frozen provider:** SearchBarWidgetProvider.java unchanged, sha256 d8916d3b90f1d4857a4d490a377e6303d02617f9f0b25f5f6b2841640c358515.
- **Artifact:** Muse-Widget-4.9.1.apk, versionCode 35, versionName 4.9.1, 435,922 bytes, SHA-256 95527e4508292b915dc75d2eea58911ef570b7923a9276821c9c84778ef6d969. Same package + signing cert as v4.9.0 (installs over it). Panel users upgrading from a slug build must paste their endpoint once in Customize — by design.

## v4.9.2 note (2026-10-31) — Activities moves onto the app's own screen

Context: v4.9.1 shipped this morning as the public release —
the panel endpoint moved out of the build into a user setting
(Customize → Panel data endpoint), the picker intact. Jonathan
then reported (11:04 EDT): "I'm not seeing it in the app." He
was right in the way that matters: opening the app lands on the
Customize screen, and Activities wasn't there — its only doors
were the Muse panel's Ask Muse card and the long-press
app-icon shortcut.

**Fix.** The Customize screen's card now opens with an
Activities row — label, the subtitle "Start goals, tracking,
and more — as prompts", and an Open button that starts
`PickerActivity` — above Bar style, in the page's own
label/value/button row pattern. Nothing else changes: the
picker, the ask-page prefill, the panel, and the endpoint
setting are exactly v4.9.1's.

**Verification:** badging versionCode 36 / 4.9.2; jadx audit
of the shipped dex shows WidgetCustomizeActivity starting
PickerActivity from the new row; render guard and no-secrets
guard passed; `SearchBarWidgetProvider.java` byte-identical
to the frozen hash (`d8916d3b…`). Slug-free build (the
endpoint is the user's setting now). Same signing key;
installs over v4.9.1 in place. APK `Muse-Widget-4.9.2.apk`,
SHA-256
`689320da39e61c7b370a5cfd4da5b11b45fbfa2c566b219e6b7fe39015269b4d`.

## v4.10.0 note (2026-10-31) — the shortcut panel is back, on activities; the bar button takes any activity

Jonathan's ask (11:10 EDT): a shortcut panel like the old
grid, addable in Settings like before, filled with the new
activities — and the search-bar widget's shortcut button
assignable to any of these activities too. The old grid died
in v4.5.0 because its cells fired gated catalog URIs; this one
fires the activities' prompt/direct surfaces instead.

**One target model.** A new `ShortcutTargets` resolves a
picker activity to the intent every surface shares: ASK
activities open the ask page with their stem prefilled;
DIRECT activities (Voice, Assistant, Open Muse app) ride the
LaunchMuseActivity trampoline into Router. The bar button's
assignment is stored in the existing URI slot as
`widget://activity/<label>` (`WidgetPrefs.shortcutTargetEntry`
/ `setShortcutActivity`); every legacy stored value
(widget://assist, widget://launch, a hatch:// URI, hidden)
resolves exactly as before — nothing migrates destructively.

**The panel.** New `ShortcutPanelWidgetProvider` +
`widget_shortcuts.xml`: the old grid's shape — a bg_card focus
block (18dp) with up to eight bg_cell medium cards (14dp) in
two rows of four. Layout uses only RemoteViews-supported
classes (the render guard scans it). The cell set is one
global setting (default: New thread, Search, Goals, Upcoming
tasks, Feed, Explore, Memory, Assistant), edited in the
Customize screen's new Shortcut panel section — a row per
cell with Remove, an Add shortcut button (gone at the cap),
changes re-render placed panels immediately through
`WidgetPrefs.refreshWidgets` (which now also names this
provider). No update period. An empty set renders one setup
cell that opens Settings. The widget's configure activity is
the Customize screen, so the add flow lands where the cells
are edited.

**The bar button.** The Customize "Right-button shortcut"
dialog now lists every picker activity plus None (hide the
button); the preview glyph and value follow the assignment.
`SearchBarWidgetProvider` gained an activity branch ahead of
its legacy shortcut handling — the one deliberate edit to
that file in this build; its rendering path (pill bitmap,
logo, container, the v4.5.3 logo treatment) is untouched, and
`widget_search_bar.xml` is byte-identical. The ask page's
mirror glyph resolves the same assignment and fires it too.

**Verification:** badging versionCode 37 / 4.10.0; jadx audit
of the shipped dex shows ShortcutPanelWidgetProvider reading
WidgetPrefs.panelShortcuts and firing ShortcutTargets, and
ShortcutTargets carrying both the prefill and trampoline
paths; render guard (including the new layout) and
no-secrets guard passed. Slug-free (the panel endpoint is a
user setting since v4.9.1). Same signing key; installs over
v4.9.2 in place. APK `Muse-Widget-4.10.0.apk`, SHA-256
`73c984155a8c735b45fe912860d82f5951531def77ea72b3de3d8a686b887d45`.

## v4.11.0 note (2026-11-01) — package + APK renamed to match the GitHub project name

Jonathan's order (2026-11-01): every APK and package name
matches its GitHub project name, reinstall accepted. This
project's repo is `jonathancruzate/meta-muse-widget`, so:

- **Package:** `com.samsung.oneui9.metamusewidget` →
  `com.jonathancruzate.metamusewidget`. Source tree moved to
  `src/com/jonathancruzate/metamusewidget/` (18 files, package
  declarations updated); manifest package attribute, the
  `PANEL_WIDGET_REFRESH` action string (manifest + provider
  constant), both widget-info `android:configure` class names,
  and all four app-shortcut targetPackage/targetClass pairs
  updated. ComponentNames in code are class-based and prefs
  file names are plain (`widget_prefs`, `compose`), so nothing
  else carried the old identity. References to OTHER apps'
  packages (Muse `com.meta.*` / `com.facebook.aura`, SmartThings
  `com.samsung.android.oneconnect`) untouched.
- **APK:** `Muse-Widget-<version>.apk` →
  `Meta-Muse-Widget-4.11.0.apk`. Display label unchanged:
  "Muse Widget".
- **Reinstall required:** the new package does not install over
  v4.10.0 or earlier. Uninstall the old app first; home-screen
  widgets must be re-added and preferences re-set afterwards
  (the old app's data, including the panel endpoint, does not
  carry over).
- **Verification:** badging versionCode 38 / 4.11.0, package
  `com.jonathancruzate.metamusewidget`, label "Muse Widget";
  render guard and no-secrets guard passed; shipped dex
  contains only the new package (1,331 class references, zero
  old-package). Same signing key (cert SHA-256 34ef6099…).
  APK `Meta-Muse-Widget-4.11.0.apk`, 444,270 bytes, SHA-256
  `d19ed0764351631541bf08731bf484e57c1d363b4ec8f1dcba21c5945ce44cb8`.

**Cert supersession (2026-11-01, later the same day):** the
"Same signing key (cert SHA-256 34ef6099…)" verification above
describes the FIRST v4.11.0 build (APK SHA-256 d19ed076…),
which was never released. Per Jonathan's order that no "One UI 9"
trace remain in the package identity, that build was superseded
by a re-signed v4.11.0 (same versionCode 38 / versionName
4.11.0) under a NEW keystore: DN `CN=Meta Muse Widget,
O=JonathanCruzate, C=US`, cert SHA-256
`2746d2a2d6dbe0fda4271acd14f4e5ab990da4e9155e7f9e75ae07fca4b814da`.
build.sh's keystore-generation DN updated to match; the legacy
keystore is preserved as `keystore/keystore-legacy-oneui9.keystore`
(password preserved in `keystore/keystore-password-legacy-oneui9.txt`,
verified readable). Rationale: nothing had shipped or been
installed under the `com.jonathancruzate.metamusewidget` identity
and the `com.samsung…` package is abandoned, so the cert change
has no install impact. Rebuilt APK: `Meta-Muse-Widget-4.11.0.apk`,
444,270 bytes, SHA-256
`2c3a5dc437153fe96954213b29935083da5beecedce8df22abeb8ed0b71c5780`;
render guard and no-secrets guard passed; badging package
`com.jonathancruzate.metamusewidget`, versionCode 38 / 4.11.0,
label "Muse Widget".

## v4.12.0 note (2026-11-01) — Voice mode shortcut now uses the accessibility workaround

Jonathan's order (2026-11-01): replace the widget's Voice
shortcut with the accessibility workaround built for the Muse
Voice Mode Shortcut app.

- **The "Voice" activity is now "Voice mode."** It no longer
  fires the plain ACTION_ASSIST surface. Through every surface
  that can assign it (Activities picker row, shortcut-panel
  cell, search-bar right button, ask-page glyph) it dispatches
  the new Router mode MODE_VOICE_MODE (6), which starts the
  Muse Voice Mode Shortcut app's no-UI toggle activity
  (`com.jonathancruzate.metamusevoicemodeshortcut/.ShortcutActivity`,
  ACTION_VIEW, NEW_TASK). That app's accessibility service taps
  Muse's own voice button — a toggle, so each fire flips voice
  mode on or off, the same switch as his side-button long
  press. `hatch://voice` itself stays TrustedCaller-gated; this
  is the designed handoff around it.
- **Fallback:** if the shortcut app isn't installed, a toast
  ("Voice mode needs the Muse Voice Mode Shortcut app —
  opening Muse instead") shows and the old assist surface
  opens, so the shortcut never dead-ends.
- **Compatibility:** stored assignments keep working —
  `ShortcutTargets.entryForLabel` maps the legacy label "Voice"
  to the renamed entry, so existing panel cells and bar-button
  assignments (stored as `widget://activity/Voice`) resolve
  unchanged. Assistant and Open Muse app are untouched. The
  entry's icon is now ic_mic (was ic_pulse).
- Changes: UriCatalog (entry rename, MODE_VOICE_MODE,
  pickerAction, gridEntries), Router (voiceMode() + dispatch
  case + package/activity constants), ShortcutTargets (mode
  map + label alias), PickerActivity (direct-row dispatch),
  strings (voice_shortcut_missing), PanelDataClient UA →
  MuseWidget/4.12.0. No widget layouts touched; no manifest
  change (QUERY_ALL_PACKAGES was already declared).
- Verification: badging versionCode 39 / 4.12.0, package
  `com.jonathancruzate.metamusewidget`, label "Muse Widget";
  signer cert SHA-256 `2746d2a2…` (same as v4.11.0 — installs
  over it in place); render + no-secrets guards pass; jadx
  audit of the shipped dex shows Router's voiceMode handoff
  (package + ShortcutActivity constants), the ShortcutTargets
  mode map and legacy "Voice" alias, and the renamed picker
  entry. APK `Meta-Muse-Widget-4.12.0.apk`, SHA-256
  `7940496792df1ffb7b30ec66427911940a282909475b011c26746c6bbb1614a7`.

## v4.12.1 note (2026-11-01) — nonfunctional URI shortcuts removed; Voice label restored

Jonathan's orders (2026-11-01): remove any nonfunctional URI
shortcuts, and rename the "Voice mode" activity back to just
Voice.

- **Audit first:** every user-facing shortcut surface was
  checked — the 27 Activities, shortcut-panel cells, the bar
  button (activity assignments + the legacy assist/launch
  migration), the four app-icon shortcuts, both QS tiles, the
  companion panel's rows, and the ask page's list
  (recents/starters/web suggestions). All functional. The
  nonfunctional URI material was the catalog itself:
  UriCatalog still carried all 71 REA-catalogued Muse URIs, 44
  of which can never open from a third-party app
  (TrustedCaller gate) and existed only as copy-the-URI
  reference rows (MODE_COPY) inside the shipped app.
- **Removed:** the 44 non-picker catalog entries (all
  template/<id> rows, the Link types and Web groups, and the
  extra reference destinations), the MODE_COPY dispatch +
  Router.copy() + the copied_gate string, the dead
  gridEntries()/findByUri()/firstOfGroup() helpers, and the
  dead hub strings (counter_template, no_results,
  open_muse_search). UriCatalog now holds exactly the 27
  working activities; its class header records where the full
  REA catalog lives (git history + refs/). The picker renders
  identically — same 27 rows, same order (verified in the
  shipped dex).
- **Renamed:** the activity label "Voice mode" → "Voice"
  everywhere it shows (picker row, shortcut dialogs, panel
  cells, bar button). Behavior unchanged from v4.12.0 — Voice
  still hands off to the Muse Voice Mode Shortcut app's
  accessibility toggle. Assignments stored during v4.12.0
  under "Voice mode" still resolve (the alias in
  ShortcutTargets.entryForLabel flipped direction).
- Verification: badging versionCode 40 / 4.12.1, package
  `com.jonathancruzate.metamusewidget`; signer cert SHA-256
  `2746d2a2…` (installs over v4.12.0 in place); render +
  no-secrets guards pass; jadx audit of the shipped dex: 27
  ENTRIES, pruned labels absent, no MODE_COPY, "Voice" label
  + legacy alias present. APK `Meta-Muse-Widget-4.12.1.apk`,
  SHA-256
  `c33bcb5385d3f4d1cbe38b4e009ce4bff1a1b248a837382734abbcf03fa0e32d`.

## Placement note (2026-11-07) — not a build

Muse shipped native home-screen widgets for Spaces. Jonathan
replaced the Muse Panel widget on his home screen with them
(the Daily Brief space widget is the one shown, verified live
against the brief payload that evening: 7:37 PM update, 53°
Partly Cloudy, Top of mind = Guillermo reimbursement + Stephany
callback). His decision, same evening: **keep this app for its
search bar and shortcuts** — the app is not retired, only the
Panel widget lost its home-screen place, and the panel data
lane is unchanged. Public note added to the GitHub README
("Status note — 2026-11-07") and SKILL.md updated to match,
at his request.

## v5.0.0 — Muse Launcher Pack, Phase 1 (built 2026-11-12)

The Muse Widget app becomes the Muse Launcher Pack (design:
`Muse-Launcher-Pack-Design.md` in the companion-layer goal;
execution ordered by Jonathan 2026-11-12 04:19 EST, swarm
#sprint-20261112-launcher-pack-execution). Same package, same
signing key, install-over upgrade from v4.12.2; the app label
is now "Muse Launcher Pack" (design D1).

- **New providers, all beside the frozen surfaces:**
  MuseOmnibarWidgetProvider (4x1 pill: body = Muse Search,
  mark = Muse Today, mic = voice; idle hint rotates feed
  state), MuseTileWidgetProvider (one provider, per-instance
  kind from TileConfigureActivity, v1 kinds: brief, up_next,
  house, transit, media, voice, decide, clock),
  MuseStackWidgetProvider (4x2 Context Stack cards, kinds by
  instance order), MuseGridWidgetProvider (favorite apps,
  direct launches, All apps header).
- **New activities:** MuseSearchActivity (apps + Ask Muse
  prefill via ComposeActivity.EXTRA_PREFILL + web row),
  MuseGridActivity (filterable app grid + App Pairs sequenced
  through Device Assistance app_launch when paired),
  HouseActivity, SetupWizardActivity (self-checking rows:
  endpoint, widget drift via PackDrift, Device Assistance
  pairing, calendar, Good Lock), PackVoiceActivity trampoline.
- **Feed bundle v2 client:** tiles read the same panel lane
  (PanelDataClient cache/fetch, user-set endpoint, nothing
  baked); v2 fields (up_next / transit / money) render when
  present and degrade to designed empty states when absent.
- **Device Assistance client (design 7.2 as amended):**
  DaClient speaks ContentProvider call() to Device
  Assistance's LocalChannel; pairing token claimed inside
  DA's on-device window, stored only in private prefs.
- **Discipline notes:** this codebase compiles with
  -bootclasspath + source 8, so the pack code contains NO
  lambdas (LambdaMetafactory is absent from that classpath;
  first build attempt proved it). Compile jar moved to
  android-36 in v5.1.0 for the Live Updates APIs.
- Frozen surfaces verified byte-identical after the build
  (baseline hashes taken 2026-11-12 pre-build: SearchBar
  provider 8c38e8da…, WidgetPrefs 45faf007…, search layout
  8d9fa2ce…, search widget-info 66ab0aa5…). NOTE: the
  SearchBar provider hash differs from the value recorded in
  the v4.5.3-era notes (d8916d3b…): the v4.10.0 deliberate
  exception changed it; today's baseline is the live truth.
- Verification: render guard passes on all 11 widget layouts
  (4 new), no-secrets guard passes, badging versionCode 42 /
  5.0.0. APK `Meta-Muse-Widget-5.0.0.apk`, SHA-256
  `5ab5f69a7b61aa9fc11b1a5933611ba2a6950153be07459d0ac95018ef30a635`.

## v5.1.0 — Muse Launcher Pack, Phase 2 surfaces (built 2026-11-12)

- **MuseHouseWidgetProvider (House Control, design W5):**
  verified house state + DND / media / voice / today action
  cells through PackActionReceiver to the authenticated
  DaClient lane; renders from the ack path, shows the pairing
  state honestly when unpaired. No flashlight (design fence).
- **Money and People tile kinds** join the configure list
  (PackData reads the v2 money feed; People renders Stephany
  presence state only, never coordinates).
- **LiveUpdateFeeder (design 7.4):** the leave-by countdown
  as an ongoing notification, promoted-ongoing on API 36+
  (the vendored android-36 stub predates the final Builder
  method, so promotion is requested reflectively with the
  documented extras key as fallback), self-cancelling after
  the leave-by. POST_NOTIFICATIONS declared; the grant is
  the user's.
- Server side landed the same night: feed bundle v2 on the
  mode-sync Worker (ingest sanitizes up_next / transit /
  money; the read lane serves feed_version 2 alongside the
  v1 shape) and panel-brief-push.py produces up_next +
  transit distillates. Verified live end to end.
- Verification: guards pass; badging versionCode 43 / 5.1.0.
  APK `Meta-Muse-Widget-5.1.0.apk`, SHA-256
  `5ed4c23c4252e68adc4ddc86104e93bf6214a65a7a622335718c7b9eefb8ffd0`.

## v5.2.0 — Muse Launcher Pack, Phase 3 polish (built 2026-11-12)

- **Per-mode theming (PackThemes):** tiles follow the house
  mode: Sleep/night renders the night-dim card with light
  text regardless of system theme, Work renders the rose
  tint; PackPrefs.themeMode pins day/night/work.
- **Drift detection (PackDrift):** the House Control widget
  joins the spec minimum; the Setup tile kind and the wizard
  report placed counts + kinds against the Muse Theme spec
  (theme kit v1.1, layout-spec.json).
- **Sleep tile (design D4):** SleepReader reads last night's
  SleepSessionRecords through framework Health Connect
  (API 34+, TimeInstantRangeFilter.Builder shape), caches a
  one-line summary on device only. No grant = designed empty
  state; the grant stays Jonathan's in Health Connect.
  android.permission.health.READ_SLEEP declared.
- **Wizard** gains the Health Connect row with the HC
  settings intent.
- Verification: guards pass; badging versionCode 44 / 5.2.0;
  all 22 manifest components present including the five pack
  providers. APK `Meta-Muse-Widget-5.2.0.apk`, SHA-256
  `0f2ef6d7854cab6248c6822cb5ed755a344b9cdb3d9479c8985ab85a84468fff`.
- **Install status:** ADB lane attempted 2026-11-12 04:41
  EST; the phone was unreachable (wireless debugging
  endpoint refused), so v5.2.0 is staged, not installed.
  The on-phone session (placement, pairing, grants, Home Up
  backup) is the theme kit's setup-guide.md.

## v5.3.0 (versionCode 45) — Muse-app aesthetic match, 2026-11-13

At Jonathan's order ("aesthetically match the existing muse apps
from the muse android app by decomposing the muse apk"), the
official Muse app (com.facebook.aura 13.0.0.4.159, quarantine
copy) was decomposed: apktool resources decode + jadx 1.5.1
single-class decompilation of the VDS Compose theme classes
(BaseColors, AuraColorsPaletteKt, the six semantic color classes,
AuraShapesKt, AuraBrushesKt, typography). Full token set:
~/workspace/muse-ux-ui-rea/MUSE-DESIGN-TOKENS.md.

Findings applied: the Muse app's identity is a neutral gray
system (light background #FCFCFC, card #FFFFFF, text #111112
with 60/35/29% alpha tiers; dark background #050505, card
#1F1F1F, elevated #343638, text #F5F5F5) with a blue accent
(#0064D4 light / #007FFD dark) and the signature gradient
MAGENTA_550 #C143E4 to BLUE_600 #0363D5. Card radius 20dp,
chip 16dp, pill full-round. Design D6's rose #EE2E7B appears
nowhere in the app palette and is superseded by this order.

Changes: values/colors.xml + values-night/colors.xml remapped
to the VDS light/dark sets (new muse_accent, muse_accent_soft,
muse_gradient_start/end, muse_bubble, border); all muse_rose
references renamed to muse_accent across 5 layouts + 2
drawables; pack card radii unified to 20dp (was 18/4 joined),
night-dim card #1B1B1D to #1F1F1F, action cell radius 14 to
16dp; Omnibar restyled to the VDS card look (white card fill,
1dp #E9EAEB hairline border, pill kept); PackThemes night text
colors to VDS dark (accent #007FFD, primary #F5F5F5, secondary
#99F5F5F5); new bg_muse_gradient drawable (the signature
gradient at card radius). Kept deliberately: the Stephany
sweater accent (Jonathan's own direction for the logo backing)
and the One UI glass/finder/nowbar values (frozen search-bar
surface family). Typography stays the system font; the Muse
app's Optimistic is proprietary and is not bundled (noted
divergence in the tokens doc).

Frozen surfaces byte-identical (SearchBarWidgetProvider
8c38e8da, WidgetPrefs 45faf007, widget_search_bar.xml
8d9fa2ce, widget_info_search.xml 66ab0aa5). Both guards
passed. Cert unchanged (SHA-256 2746d2a2...). APK 476K,
SHA-256 6f4e9b4b78861f5a959b884ecca302f2376f1baaec9ab5f685e
7b5d4b926eaad. Installed -r via the ADB lane and verified by
dumpsys (versionCode 45, versionName 5.3.0).

## v5.3.1 (code 46) — 2026-11-13: widget text matched to Muse's Spaces widgets

Per Jonathan's order, the pack's name and update-time text now uses
the exact metrics of the Muse app's own Spaces widgets, extracted
from his installed Muse app (15.0.0.3.156, pulled from the phone;
the 13.0.0.4.159 reference build predates the widgets, manifest has
zero APPWIDGET receivers). Muse widget styles (Optimistic VF):
space name 15sp, 'wght' 600, #111112 (night #F5F5F5),
letterSpacing 0.015; update time 13sp, #99000409 (night #99F2F7FF),
letterSpacing 0.01. Applied: panel tv_headline 16sp to 15sp +
letterSpacing .015 (all three panels); panel tv_refresh takes the
new muse_updated color (layout + MusePanelWidgetProvider, which
previously forced the theme accent); tile/stack tv_age 11sp to 13sp
+ muse_updated + letterSpacing .01; PackThemes night-dim age/refresh
now #99F2F7FF. Font family stays the system sans: Optimistic is
Meta's proprietary font and the repo ships publicly; size, weight,
color and spacing match exactly. Frozen surfaces unchanged
(SearchBarWidgetProvider 8c38e8da, WidgetPrefs 45faf007,
widget_search_bar.xml 8d9fa2ce, widget_info_search.xml 66ab0aa5).
Both guards passed. Cert unchanged (2746d2a2...). APK 476K,
SHA-256 ae97f804c7f2ba22 (full hash in the install record).
Installed -r via the ADB lane, dumpsys-verified (46 / 5.3.1).
