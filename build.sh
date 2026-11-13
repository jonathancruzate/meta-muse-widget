#!/bin/bash
# Build the One UI 9 Meta Muse Widget APK from source.
# Toolchain: tools/sdk (build-tools 35.0.0, platform android-35) + Temurin JDK 21.
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME="$HOME/workspace/custom-skills/local-rea/tools/jdk"
export PATH="$JAVA_HOME/bin:$PATH"
BT="${ANDROID_SDK_ROOT:-tools/sdk}/build-tools/35.0.0"
ANDROID_JAR="${ANDROID_SDK_ROOT:-tools/sdk}/platforms/android-36/android.jar"
KS="keystore/metamuse.keystore"
KS_PASS_FILE="keystore/keystore-password.txt"

rm -rf build/classes build/dex build/gen
mkdir -p build/gen build/classes build/dex keystore

if [ ! -f "$KS" ]; then
  PASS="$(openssl rand -base64 24 | tr -d '/+=' | head -c 24)"
  echo "$PASS" > "$KS_PASS_FILE"
  chmod 600 "$KS_PASS_FILE"
  keytool -genkeypair -keystore "$KS" -alias onemuse -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass "$PASS" -keypass "$PASS" \
    -dname "CN=Meta Muse Widget, O=JonathanCruzate, C=US" 2>/dev/null
  echo "keystore created"
fi
PASS="$(cat "$KS_PASS_FILE")"

echo "== aapt2 compile =="
find res -type f | sort > build/res-files.txt
"$BT/aapt2" compile --dir res -o build/res.zip

echo "== aapt2 link =="
"$BT/aapt2" link -o build/unsigned.apk \
  -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  --java build/gen \
  --min-sdk-version 24 --target-sdk-version 35 \
  --version-code 45 --version-name "5.3.0" \
  build/res.zip

echo "== panel endpoint: none (v4.9.1) =="
# v4.9.1 removed build-time endpoint injection entirely. The panel reads
# its data URL from SharedPreferences at runtime: the user pastes their
# own endpoint in Customize -> Panel data endpoint, and the app ships
# with none configured (the panel shows its "not configured" state until
# then). The no-secrets guard below checks the retired slug VALUE itself
# against dex + source + resources to prove nothing is baked in.
echo "== javac =="
find src build/gen -name "*.java" > build/sources.txt
javac -source 8 -target 8 -bootclasspath "$ANDROID_JAR" -d build/classes @build/sources.txt

echo "== d8 =="
"$BT/d8" --release --min-api 24 --lib "$ANDROID_JAR" --output build/dex $(find build/classes -name "*.class")

echo "== package dex =="
# classes.dex must sit at the APK root, not under dex/
(cd build/dex && zip -q ../unsigned.apk classes.dex)

echo "== render guard (v4.6.2, extended v4.8.0; REA-RENDER-DIFF-431) =="
# The 4.3.2->4.5.2 render break was a plain <View> (iv_logo_hit) in the
# widget layout: android.view.View is not a RemoteViews-supported type, so
# One UI Home fails inflation and shows "Couldn't add widget". v4.8.0
# extends the guard from the one search-bar layout to EVERY widget
# layout: everything referenced by any widget-info file
# (initialLayout + previewLayout) plus every res/layout/widget_*.xml
# (the panel widget's density layouts are referenced from provider
# code via the responsive RemoteViews map, not from widget-info):
# each compiled layout is scanned and EVERY element in it must be a
# RemoteViews-supported (@RemoteView) view class. The iv_logo_hit
# resource-name check stays as well.
RV_SUPPORTED="FrameLayout LinearLayout RelativeLayout GridLayout AnalogClock Button Chronometer ImageButton ImageView ProgressBar TextClock TextView ViewFlipper ListView GridView StackView AdapterViewFlipper"
WIDGET_LAYOUTS="$( { grep -hoE '@layout/[A-Za-z0-9_]+' res/xml/widget_info*.xml | sed 's,^@layout/,,'; ls res/layout/widget_*.xml | sed -e 's,^res/layout/,,' -e 's,\.xml$,,'; } | sort -u )"
if [ -z "$WIDGET_LAYOUTS" ]; then
  echo "RENDER GUARD FAILED: no widget layouts found in res/xml/widget_info*.xml"
  exit 1
fi
for L in $WIDGET_LAYOUTS; do
  echo "render guard: scanning compiled res/layout/$L.xml"
  ELEMENTS="$("$BT/aapt2" dump xmltree build/unsigned.apk --file "res/layout/$L.xml" | grep -oE 'E: [A-Za-z][A-Za-z0-9_.]*' | sed 's,^E: ,,' | sort -u)"
  for E in $ELEMENTS; do
    case " $RV_SUPPORTED " in
      *" $E "*) ;;
      *) echo "RENDER GUARD FAILED: res/layout/$L.xml contains non-RemoteViews element <$E>"; exit 1 ;;
    esac
  done
done
if "$BT/aapt2" dump resources build/unsigned.apk | grep -q "iv_logo_hit"; then
  echo "RENDER GUARD FAILED: iv_logo_hit present in resources.arsc"
  exit 1
fi
echo "render guard passed"

echo "== no-secrets guard (v4.7.0, LIVE-DATA-LANE-DESIGN §8.7) =="
# Constraint 1 of the live-data lane: no secrets in the APK, ever.
# Fail the build if source or the compiled dex contains either
# Worker secret's VALUE, the MacroDroid webhook URL, or the webhook
# host pattern. Secret values are read from their 0600 files into
# shell variables for the comparison only — never printed. Since
# v4.9.1 there are NO exceptions: the retired panel slug's value is
# itself one of the checked secrets.
DEX="build/dex/classes.dex"
secret_guard_check() {
  # $1 = literal value (may be empty), $2 = label
  local v="$1"
  [ "${#v}" -ge 8 ] || return 0
  if grep -qF "$v" "$DEX" || grep -rqF "$v" src res AndroidManifest.xml; then
    echo "NO-SECRETS GUARD FAILED: $2 found in dex or source"
    exit 1
  fi
}
POLL_SECRET_FILE="$HOME/workspace/goals/manage-my-smart-home/hidden_files/st-webhook-poll-secret.txt"
PHONE_SECRET_FILE="$HOME/workspace/goals/phone-notification-tray-management/hidden_files/phone_push_secret.txt"
WEBHOOKS_FILE="$HOME/workspace/goals/manage-my-smart-home/hidden_files/macrodroid-webhooks.json"
[ -f "$POLL_SECRET_FILE" ] && secret_guard_check "$(cat "$POLL_SECRET_FILE")" "POLL_SECRET value"
[ -f "$PHONE_SECRET_FILE" ] && secret_guard_check "$(cat "$PHONE_SECRET_FILE")" "PHONE_PUSH_SECRET value"
if [ -f "$WEBHOOKS_FILE" ]; then
  WEBHOOK_URL="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["muse"])' "$WEBHOOKS_FILE" 2>/dev/null || true)"
  secret_guard_check "$WEBHOOK_URL" "MacroDroid webhook URL"
fi
[ -f keystore/panel-slug.txt ] && secret_guard_check "$(cat keystore/panel-slug.txt)" "retired panel slug value"
if grep -qa "panel-data" "$DEX" || grep -qa "PanelConfig" "$DEX"; then
  echo "NO-SECRETS GUARD FAILED: baked panel endpoint artifact in dex"
  exit 1
fi
if grep -q "trigger.macrodroid.com" "$DEX" || grep -rq "trigger.macrodroid.com" src res; then
  echo "NO-SECRETS GUARD FAILED: MacroDroid webhook host pattern in dex or source"
  exit 1
fi
echo "no-secrets guard passed"

echo "== zipalign + sign =="
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$PASS" --key-pass "pass:$PASS" \
  --out Meta-Muse-Widget-5.3.0.apk build/aligned.apk

echo "== verify =="
"$BT/apksigner" verify --print-certs Meta-Muse-Widget-5.3.0.apk
ls -lh Meta-Muse-Widget-5.3.0.apk
