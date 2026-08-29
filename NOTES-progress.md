# Demo-emulator: DroidGuard Remote Demo — Progress & Findings (2026-08-29)

## GOAL
Produce demo video evidence for microG DroidGuard PR #3750 (multi-step remote flow:
initWithRequest -> snapshot -> close) by running the patched GmsCore on a GitHub-hosted
Android emulator (aosp_atd x86_64, API 30) and driving the flow from our probe app.

## REPOS (all under digivasserver-ai on GitHub)
- `ci-repro` — microG GmsCore mirror + patched droidguard (PR #3750 head @ 97b0877) +
  CI workflows to build + publish release APKs. Default branch: **master**.
- `demo-emulator` — probe app + reference droidguard server + the emulator workflow.
  Branch: main.

## PIPELINE (demo-emulator/.github/workflows/install-test.yml)
1. Install SDK + emulator + aosp_atd x86_64 image (KVM).
2. Build probe APK on runner (`probe/build_apk.sh`).
3. Download `com.google.android.gms-252432000.apk` from release `apks-97b0877`,
   sign (microG release APKs are unsigned), install.
4. Write DroidGuard prefs to /data/data/com.google.android.gms/shared_prefs (root).
5. Start `droidguard-server/droidguard_multistep_server.py` on runner :8080.
   (Emulator reaches host via 10.0.2.2; NSC in the APK allows cleartext to 10.0.2.2.)
6. Probe binds `com.google.android.gms.droidguard.service.START`, sweeps broker codes,
   runs demo, writes marker `demo_result.txt` (FAILED/DEMO-COMPLETE).
7. dumpsys + logcat → artifacts `install-test` (emulator.log, server.log, logcat.txt...).

## CURRENT STATUS (as of 15:2x UTC)
- Workflow runs GREEN except "Run probe + verify marker" — the demo itself FAILS:
  `FAILED no broker code matched` (sweep of codes 45,41,25,26,24,23,46,47,28).
- Reference server runs fine (returns begin/snapshot JSON over HTTP).

## THE CORE MYSTERY — broker transaction code + parcel layout mismatch
The probe implements microG's `IGmsServiceBroker.getService` protocol:
`transact(code=45, token, IGmsCallbacks binder, GetServiceRequest)`.

### Evidence
- microG broker logs `GmsServiceBroker: onTransact [unknown]: 45` in earlier runs
  (AbstractGmsServiceBroker.onTransact falls through =>
  the GENERATED Stub did NOT dispatch code 45).
- Sweep results:
  - Most codes: `transact=false` + NO callback + NO exception (STUB returned false =>
    not dispatched => empty reply => clean readException).
  - **code 24 (one run): threw `SecurityException: UID [10072] is not related to
    packageName [...] (seems to be com.digivasserver.demo)` with GARBAGE packageName
    bytes** — this is DroidGuard's OWN `PackageUtils.getAndCheckCallingPackageOrImpersonation`
    inside `DroidGuardServiceBroker.handleServiceRequest` => **code 24 = getService in the
    installed APK** and the SECONDARY problem is the parcel layout of GetServiceRequest
    (garbage packageName => server read different field layout).
    (Second run code 24 was silent — non-determinism not yet explained.)
  - code 23 (both runs): threw `IllegalStateException: Bad magic number for Bundle: 0x16`
    => also a REAL method (some getXService with Bundle at that position).
- The repo's OWN `IGmsServiceBroker.aidl` (at 97b0877) has `getService(...)=45` and
  `GetServiceRequest` safeparcel v6 fields 1..15 — probe matches that EXACTLY.
- The installed APK is R8-**minified** (dexdump shows `LJ/N;`-style class names; NO
  `IGmsServiceBroker$Stub` class name exists in classes*.dex — that's why name-based
  dexdump found nothing; AIDL interface class names are NOT kept).

### Rebuild attempt (KEY LEARNING)
- Rebuilt the APK via ci-repro "Build & publish APK artifacts" workflow => **byte-identical
  APK** (107,763,940 bytes) — deterministic build. Republished tag apks-97b0877 via
  "Publish APK release from artifact".
- **Despite the identical APK, runtime behavior differed between runs (code 24 threw in
  one run, silent in the next).** This suggests the runtime difference comes from the
  EMULATOR environment (system_server/UID allocation/order) — NOT the APK bytes. NOT yet
  explained. (Possibly binding order / which GmsServiceBroker object the service returns
  per process — e.g., the "user" vs "unstable" process, or a race in BaseService init.)

### Current hypothesis (to test with a fresh init)
1. The installed broker's generated stub numbers getService **24** (positional) in the
   MINIFIED build, and its GetServiceRequest layout is OLD (pre-GetServiceRequest-era
   signature: `(IGmsCallbacks, int code, String packageName, Bundle params)`).
2. THE FIX TO TRY: probe sends OLD-STYLE envelope (callback + int + String + Bundle)
   for code 24 (plus a small range) — if the callback fires, the whole demo proceeds.
3. OR find the real tx code + layout from the APK itself via descriptor-string-based
   dexdump lookup (committed at 655fa26 + pending fix; the Python class-block extractor).
   The descriptor string "com.google.android.gms.common.internal.IGmsServiceBroker"
   survives obfuscation — grep dexdump output for it, dump that class's onTransact.

## OTHER LEARNINGS (this session + prior)
- GitHub workflow YAML: a heredoc `<?xml`/content at column 0 breaks parsing
  (name becomes file path, no workflow_dispatch trigger). Push prefs as file + `cat >` via adb.
- adb `\r` handling: use `tr -d '\r'`; `$'...'` quoting safe in YAML.
- actionlint at /root/go/bin/actionlint; run before every push.
- `adb reboot`/`wait-for-device` HANGS on aosp_atd; use `adb shell stop && start` or none.
- aidl (build-tools 34) fails (`syntax error` on android.jar) => hand-write AIDL stubs.
- javac17: -bootclasspath requires -source/-target 8. Lambdas break under
  -bootclasspath android.jar => anonymous classes. Release APKs unsigned => sign to install.
- adb push to /data/data/<pkg> fails "Is a directory" => `adb shell "cat > <path>" < file`.
- GMS_UID via `cmd package list packages -U | awk -F: '/com.google.android.gms/{print $NF; exit}'`.
- Local box is aarch64: SDK x86_64 binaries (dexdump/aapt2) can't run locally.
- microG org is blocked for digivasserver-ai account (403), PR #3750 was closed by
  maintainers; PR-body PATCHes still work. This demo is for evidence/records.

## FILES THAT MATTER
- demo-emulator: probe/src/com/digivasserver/demo/MainActivity.java (sweep), probe/build_apk.sh,
  probe/aidl/... stubs, prefs/com.google.android.gms_preferences.xml,
  droidguard-server/droidguard_multistep_server.py, .github/workflows/install-test.yml (+dexdump.yml).
- ci-repro: workflows build-artifacts.yml + publish-release.yml + build.yml; release apks-97b0877.

## NEXT STEPS (once repo is fresh/regenerated)
A. Probe: try OLD-STYLE getService envelope `(IGmsCallbacks, int, String, Bundle)` at code 24
   (and sweep 20..30) — callback fires => runDroidGuardDemo => DEMO-COMPLETE => record video.
B. Or: finish the descriptor-string dexdump to learn truth from the APK:
   `dexdump -d classes*.dex | python3` extract class whose body contains
   "com.google.android.gms.common.internal.IGmsServiceBroker"; read TRANSACTION_getService
   value from its static fields / switch in onTransact.
C. Once a code matches, PIN it (drop the sweep) for the demo video.
D. Screenrecord the emulator during the probe run for the demo footage.