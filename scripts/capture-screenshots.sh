#!/usr/bin/env bash
# Capture script for the "Widget screenshots" workflow (.github/workflows/screenshot.yml).
#
# Why a file: reactivecircus/android-emulator-runner executes its `script:`
# block line-by-line, each line in a separate /usr/bin/sh -c process — shell
# variables do not survive between lines and multi-line constructs break.
# All stateful capture logic therefore lives here, and the workflow's script
# block is a single line: `bash scripts/capture-screenshots.sh`.
# See docs/SCREENSHOT-CI.md.
set -u

# APP_ID and the release APK (muse-widget.apk, downloaded by an earlier
# workflow step into the workspace root) come from the workflow environment.
adb install -r muse-widget.apk
adb shell am start -n "$APP_ID/.WidgetCustomizeActivity"
sleep 8

adb shell uiautomator dump /sdcard/window_dump.xml || true
adb pull /sdcard/window_dump.xml window_dump.xml || true
if grep -q "$APP_ID" window_dump.xml; then
  echo "Customize screen confirmed in UI dump"
else
  echo "WARNING: app package not found in UI dump"
fi

# One UI bar style is the default selection — capture it first.
adb exec-out screencap -p > screenshot-oneui.png
ls -l screenshot-oneui.png

# Find the Pixel bar-style radio from the pre-scroll UI dump. Its bounds in
# the dump span the full-width row, so the center lands on the tappable row.
COORDS=$(python3 -c 'import re; d = open("window_dump.xml").read(); m = re.search("barstyle_pixel[^>]*bounds=.\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]", d); print(((int(m.group(1)) + int(m.group(3))) // 2, (int(m.group(2)) + int(m.group(4))) // 2) if m else "NOTFOUND")')
echo "coords: $COORDS"
if [ "$COORDS" = "NOTFOUND" ] || [ -z "$COORDS" ]; then
  # Fallback measured from a verified capture of this exact screen on the
  # pixel_7 profile (1080x2400): the Pixel row center is at (394, 1037).
  echo "Dump parse missed; using verified fallback tap 394 1037"
  COORDS="394 1037"
else
  COORDS=$(echo "$COORDS" | tr -d '(),')
fi
set -- $COORDS
echo "Tapping Pixel bar-style radio at $1 $2"
adb shell input tap "$1" "$2"
sleep 2

adb exec-out screencap -p > screenshot-pixel.png
ls -l screenshot-pixel.png
