#!/usr/bin/env bash
# Build the DroidGuard probe APK without Gradle/AGP:
# aidl -> javac -> d8 -> aapt2 link -> zipalign -> apksigner
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
BT_VERSION="${BT_VERSION:-34.0.0}"
BT="$ANDROID_HOME/build-tools/$BT_VERSION"
PLATFORM="$ANDROID_HOME/platforms/android-30/android.jar"

AIDL="$BT/aidl"
AAPT2="$BT/aapt2"
D8="$BT/d8"
ZIPALIGN="$BT/zipalign"
APKSIGNER="$BT/apksigner"

echo "ROOT=$ROOT"
"$BT/aapt2" version
ls "$PLATFORM" >/dev/null

BUILD="$ROOT/build"
rm -rf "$BUILD" && mkdir -p "$BUILD/gen" "$BUILD/cls" "$BUILD/dexout"

# 1) AIDL -> Java stubs
while IFS= read -r f; do
  rel="${f#"$ROOT/aidl/"}"
  echo "aidl: $rel"
  "$AIDL" -p"$PLATFORM" -I"$ROOT/aidl" -o"$BUILD/gen" "$f"
done < <(find "$ROOT/aidl" -name '*.aidl' | sort)

echo "generated: $(find "$BUILD/gen" -name '*.java' | wc -l) java files"

# 2) javac (Android API 30 bootclasspath)
find "$BUILD/gen" "$ROOT/src" -name '*.java' > "$BUILD/sources.txt"
javac -source 11 -target 11 -bootclasspath "$PLATFORM" -classpath "$BUILD/gen" \
  -d "$BUILD/cls" @"$BUILD/sources.txt"
echo "classes: $(find "$BUILD/cls" -name '*.class' | wc -l)"

# 3) d8 -> dex
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