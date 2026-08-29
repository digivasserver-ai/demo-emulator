#!/usr/bin/env bash
# (run on the runner, with ANDROID_HOME set) — configure the running emulator:
#  1. install demo CA as a system CA (https to 10.0.2.2)
#  2. point microG DroidGuard at the TLS server in Network mode
set -euo pipefail
ADB="${ANDROID_HOME}/platform-tools/adb"

# ---- 1) system CA ----
CERT="$1"   # path to "ca-<hash>.0"
echo "> push CA to /system/etc/security/cacerts"
"$ADB" shell "mount -o rw,remount /" >/dev/null 2>&1 || true
"$ADB" shell "mount -o rw,remount /system" >/dev/null 2>&1 || true
"$ADB" push "$CERT" /data/local/tmp/ca-cert.0 >/dev/null
"$ADB" shell "cp /data/local/tmp/ca-cert.0 /system/etc/security/cacerts/$(basename "$CERT")" || \
  "$ADB" shell "cp /data/local/tmp/ca-cert.0 /apex/com.android.conscrypt/cacerts/$(basename "$CERT")" || true
"$ADB" shell "chmod 644 /system/etc/security/cacerts/$(basename "$CERT")" || true
"$ADB" shell "ls -la /system/etc/security/cacerts/ | grep $2" || true

# ---- 2) microG DroidGuard prefs ----
# Force-stop first so GmsCore recreates its data dir (do it before push)
"$ADB" shell "am force-stop com.google.android.gms" >/dev/null 2>&1 || true
"$ADB" shell "mkdir -p /data/data/com.google.android.gms/shared_prefs" || true
GMS_UID=$("$ADB" shell "stat -c %u /data/data/com.google.android.gms 2>/dev/null | tr -d '\r'" || echo 9999)
echo "> GmsCore uid=$GMS_UID"
"$ADB" shell "cat > /data/data/com.google.android.gms/shared_prefs/com.google.android.gms_preferences.xml" <<'EOF'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="droidguard_enabled" value="true" />
    <string name="droidguard_mode">Network</string>
    <string name="droidguard_network_server_url">https://10.0.2.2:8443/</string>
    <boolean name="droidguard_block_hw_attestation" value="true" />
</map>
EOF
"$ADB" shell "chown -R $GMS_UID:$GMS_UID /data/data/com.google.android.gms/shared_prefs"
"$ADB" shell "chmod 660 /data/data/com.google.android.gms/shared_prefs/com.google.android.gms_preferences.xml"
echo "> prefs written:"
"$ADB" shell "cat /data/data/com.google.android.gms/shared_prefs/com.google.android.gms_preferences.xml"