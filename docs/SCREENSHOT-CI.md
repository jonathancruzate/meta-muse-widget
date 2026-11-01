# Screenshot CI (`.github/workflows/screenshot.yml`)

Produces genuine emulator screenshots of the Muse Widget **Customize**
screen (the screen with the live widget preview) without needing a
physical device or a local emulator with hardware virtualization.

## What it does

1. Runs on a GitHub-hosted `ubuntu-latest` runner and enables KVM, so
   the Android emulator is hardware-accelerated and boots in minutes.
2. Downloads the **pinned GitHub Release APK** (the shipped binary —
   currently `v4.10.0` / `Muse-Widget-4.10.0.apk`) and verifies its
   SHA-256 against the value recorded in the workflow before
   installing it. It never builds a debug APK.
3. Boots an API 35 `google_apis` x86_64 emulator (Pixel 7 profile,
   headless, software GPU), installs the APK, and opens the Customize
   screen (`com.samsung.oneui9.metamusewidget/.WidgetCustomizeActivity`).
4. Confirms the app is on screen from a uiautomator dump, then captures:
   - `screenshot-oneui.png` — Customize screen with the default
     One UI bar-style preview.
   - `screenshot-pixel.png` — after tapping the in-app
     "Pixel (Material You bar)" radio, the same screen scrolled back
     to the top, showing the Pixel-style preview.
   - `window_dump.xml` — the uiautomator hierarchy, for diagnosis.

## How to run it

- **Manually:** GitHub → Actions → *Widget screenshots* → *Run workflow*.
- **Automatically:** any push to `main` that touches
  `.github/workflows/screenshot.yml` runs it once.

## Where the screenshots land

On the workflow run's page, under **Artifacts → `widget-screenshots`**
(a zip containing the PNGs above). Artifacts are kept for the
repository's default retention period.

## Updating for a new release

Edit the `env` block at the top of `.github/workflows/screenshot.yml`:
set `RELEASE_TAG`, `APK_NAME`, and `APK_SHA256` to the new release's
tag, asset filename, and the SHA-256 published in its release notes.

## Execution model (learned 2026-10-31, runs #1–#4)
`reactivecircus/android-emulator-runner` executes its `script:` block line-by-line, each line in a separate `/usr/bin/sh -c` process: shell variables do not persist between lines, and multi-line constructs (quoted strings, heredocs, if-blocks) break. The workflow's script block is therefore a single line — `bash scripts/capture-screenshots.sh` — and all stateful capture logic (install, launch, UI-dump parse with a measured-coordinate fallback, taps, screencaps) lives in that script file, where normal bash semantics apply.
