#!/usr/bin/env bash
# Build the DroidGuard probe APK without Gradle/AGP:
# javac -> d8 -> aapt2 link -> zipalign -> apksigner
# (AIDL stubs are hand-written under src/, so no aidl step is needed.)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
BT_VERSION="${BT_VERSION:-34.0.0}"
BT="$ANDROID_HOME/build-tools/$BT_VERSION"
PLATFORM="$ANDROID_HOME/platforms/android-30/android.jar"

AAPT2="$BT/aapt2"
D8="$BT/d8"
ZIPALIGN="$BT/zipalign"
APKSIGNER="$BT/apksigner"

echo "ROOT=$ROOT"
"$BT/aapt2" version
ls "$PLATFORM" >/dev/null

BUILD="$ROOT/build"
rm -rf "$BUILD" && mkdir -p "$BUILD/cls" "$BUILD/dexout"

# 1) javac (Android API 30 bootclasspath; source/target 8 because
#    -bootclasspath is not allowed with target 11 on JDK 17)
find "$ROOT/src" -name '*.java' > "$BUILD/sources.txt"
javac -source 8 -target 8 -bootclasspath "$PLATFORM" \
  -d "$BUILD/cls" @"$BUILD/sources.txt"
echo "classes: $(find "$BUILD/cls" -name '*.class' | wc -l)"

# 2) d8 -> dex
"$D8" --lib "$PLATFORM" --min-api 30 --output "$BUILD/dexout" \
  $(find "$BUILD/cls" -name '*.class' | sort)
ls -la "$BUILD/dexout/classes.dex"

# 4) aapt2 link (manifest + resources, no res/)
"$AAPT2" link -o "$BUILD/base.apk" -I "$PLATFORM" \
  --manifest "$ROOT/AndroidManifest.xml" \
  --min-sdk-version 30 --target-sdk-version 30 \
  --version-code 1 --version-name 1.0 \
  --auto-add-overlay
(cd "$BUILD/dexout" && zip -uj "$BUILD/base.apk" classes.dex)
echo "apk (unsigned): $(stat -c %s "$BUILD/base.apk") bytes"

# 5) zipalign
"$ZIPALIGN" -f 4 "$BUILD/base.apk" "$BUILD/aligned.apk"

# 6) sign with a generated debug keystore
if [ ! -f "$BUILD/demo.keystore" ]; then
  keytool -genkeypair -keystore "$BUILD/demo.keystore" -alias demo \
    -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android \
    -dname "CN=Demo, OU=Demo, O=Demo, L=Demo, S=Demo, C=US" >/dev/null 2>&1
fi
"$APKSIGNER" sign --ks "$BUILD/demo.keystore" --ks-pass pass:android \
  --out "$BUILD/demo.apk" "$BUILD/aligned.apk"
echo "signed: $BUILD/demo.apk ($(stat -c %s "$BUILD/demo.apk") bytes)"
"$APKSIGNER" verify --print-certs "$BUILD/demo.apk" | head -3 || true