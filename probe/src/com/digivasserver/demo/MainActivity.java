package com.digivasserver.demo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.common.internal.IGmsServiceBroker;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;
import com.google.android.gms.droidguard.internal.IDroidGuardHandle;
import com.google.android.gms.droidguard.internal.IDroidGuardService;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal probe: binds to microG's DroidGuard service through the standard
 * GmsCore broker protocol, then drives the multi-step request-backed flow
 * (initWithRequest -> snapshot -> close) exactly like a Play Integrity
 * client would.
 */
public class MainActivity extends Activity {

    private static final String TAG = "DGProbe";
    private static final int DROID_GUARD_SERVICE_ID = 25; // GmsService.DROID_GUARD

    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView logView;
    private StringBuilder logBuf = new StringBuilder();

    private volatile IBinder rawBinder;
    private volatile boolean done = false;

    // sweep state: what the most recent getService attempt produced
    private volatile java.util.concurrent.CountDownLatch cbLatch = new java.util.concurrent.CountDownLatch(1);
    private volatile Object cbResult; // null | "status:N" | IDroidGuardService

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            log("binder: " + name.flattenToShortString());
            rawBinder = binder;
            requestDroidGuardService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("service disconnected");
        }
    };

    private final IGmsCallbacks callbacks = new IGmsCallbacks.Stub() {
        @Override
        public void onPostInitComplete(int statusCode, IBinder binder, Bundle params) {
            if (statusCode != 0) {
                log("callback status=" + statusCode);
                cbResult = "status:" + statusCode;
                cbLatch.countDown();
                return;
            }
            log("callback onPostInitComplete(0) @ " + now());
            cbResult = IDroidGuardService.Stub.asInterface(binder);
            cbLatch.countDown();
        }

        @Override
        public void onAccountValidationComplete(int statusCode, Bundle params) {
        }

        @Override
        public void onPostInitCompleteWithConnectionInfo(int statusCode, IBinder binder,
                com.google.android.gms.common.internal.ConnectionInfo info) {
            onPostInitComplete(statusCode, binder, info == null ? null : info.params);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        log("microG DroidGuard remote demo");
        log("mode=Network (prefs set by root)");
        log("binding to droidguard.service.START ...");
        Intent intent = new Intent("com.google.android.gms.droidguard.service.START");
        intent.setPackage("com.google.android.gms");
        boolean ok = bindService(intent, connection, Context.BIND_AUTO_CREATE);
        log("bindService -> " + ok);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!done) {
            try {
                unbindService(connection);
            } catch (Exception ignored) {
            }
        }
    }

    private void requestDroidGuardService() {
        try {
            // The source AIDL has getDroidGuardService=12, but the R8-minified
            // installed APK has it at 13 (all codes shifted +1).  The AIDL proxy
            // would send code 12 → wrong method → silent failure.  Instead, go
            // straight to raw transact with the CONFIRMED installed codes.
            //
            // Installed APK confirmed codes (from smali constants):
            //   getDroidGuardService = 13
            //   getService           = 46
            //   validateAccount      = 47
            //   getWalletServiceWithPackageName = 42
            //
            // Parcel format for getDroidGuardService(IGmsCallbacks, int, String, Bundle):
            //   writeInterfaceToken(IGmsServiceBroker descriptor)
            //   writeStrongBinder(callbacks.asBinder())
            //   writeInt(serviceId)
            //   writeString(packageName)
            //   writeBundle(params)

            log("raw transact: getDroidGuardService (code 13)");

            // --- Attempt 1: code 13 with old-style envelope ---
            cbResult = null;
            cbLatch = new java.util.concurrent.CountDownLatch(1);
            android.os.Parcel data = android.os.Parcel.obtain();
            android.os.Parcel reply = android.os.Parcel.obtain();
            boolean transacted = false;
            try {
                data.writeInterfaceToken("com.google.android.gms.common.internal.IGmsServiceBroker");
                data.writeStrongBinder(callbacks.asBinder());
                data.writeInt(DROID_GUARD_SERVICE_ID);
                data.writeString(getPackageName());
                data.writeBundle(null);
                transacted = rawBinder.transact(13, data, reply, 0);
                reply.readException();
                log("code 13 transact=" + transacted + " @ " + now());
            } catch (Exception e) {
                log("code 13 threw: " + e);
            } finally {
                data.recycle();
                reply.recycle();
            }
            try {
                boolean fired = cbLatch.await(8, java.util.concurrent.TimeUnit.SECONDS);
                log("code 13 callbackFired=" + fired);
            } catch (InterruptedException ie) {
                log("interrupted: " + ie);
            }
            Object got = cbResult;
            if (got instanceof IDroidGuardService) {
                log("MATCHED code 13 -> IDroidGuardService");
                runDroidGuardDemo((IDroidGuardService) got);
                return;
            }
            if (got != null) {
                log("code 13 produced " + got + " (not droidguard)");
            }

            // --- Attempt 2: sweep nearby codes (±2 around 13) ---
            log("sweeping codes around 13: 11..15");
            final int[] nearby = {11, 12, 14, 15};
            for (final int code : nearby) {
                cbResult = null;
                cbLatch = new java.util.concurrent.CountDownLatch(1);
                data = android.os.Parcel.obtain();
                reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.google.android.gms.common.internal.IGmsServiceBroker");
                    data.writeStrongBinder(callbacks.asBinder());
                    data.writeInt(DROID_GUARD_SERVICE_ID);
                    data.writeString(getPackageName());
                    data.writeBundle(null);
                    transacted = rawBinder.transact(code, data, reply, 0);
                    reply.readException();
                    log("code " + code + " transact=" + transacted);
                } catch (Exception e) {
                    log("code " + code + " threw: " + e);
                } finally {
                    data.recycle();
                    reply.recycle();
                }
                try {
                    boolean fired = cbLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
                    log("code " + code + " callbackFired=" + fired);
                } catch (InterruptedException ie) {
                    log("sweep interrupted: " + ie);
                }
                got = cbResult;
                if (got instanceof IDroidGuardService) {
                    log("MATCHED code " + code + " -> IDroidGuardService");
                    runDroidGuardDemo((IDroidGuardService) got);
                    return;
                }
                if (got != null) {
                    log("code " + code + " produced " + got + " (not droidguard)");
                }
            }

            // --- Attempt 3: getService (code 46) with GetServiceRequest ---
            log("trying getService (code 46) with GetServiceRequest");
            cbResult = null;
            cbLatch = new java.util.concurrent.CountDownLatch(1);
            data = android.os.Parcel.obtain();
            reply = android.os.Parcel.obtain();
            try {
                data.writeInterfaceToken("com.google.android.gms.common.internal.IGmsServiceBroker");
                data.writeStrongBinder(callbacks.asBinder());
                // GetServiceRequest: write serviceId + packageName + extras
                data.writeInt(DROID_GUARD_SERVICE_ID);
                data.writeString(getPackageName());
                data.writeInt(0); // callingUid
                data.writeString(null); // accountName
                data.writeString(null); // authType
                data.writeStrongBinder(null); // sessionInfo
                data.writeInt(0); // extraFlags
                data.writeBundle(null); // extras
                transacted = rawBinder.transact(46, data, reply, 0);
                reply.readException();
                log("code 46 transact=" + transacted);
            } catch (Exception e) {
                log("code 46 threw: " + e);
            } finally {
                data.recycle();
                reply.recycle();
            }
            try {
                boolean fired = cbLatch.await(8, java.util.concurrent.TimeUnit.SECONDS);
                log("code 46 callbackFired=" + fired);
            } catch (InterruptedException ie) {
                log("interrupted: " + ie);
            }
            got = cbResult;
            if (got instanceof IDroidGuardService) {
                log("MATCHED code 46 -> IDroidGuardService");
                runDroidGuardDemo((IDroidGuardService) got);
                return;
            }
            if (got != null) {
                log("code 46 produced " + got + " (not droidguard)");
            }

            finishDemo("FAILED no broker code matched", false);
        } catch (Exception e) {
            log("sweep threw: " + e);
            for (StackTraceElement el : e.getStackTrace()) {
                log("  at " + el.getClassName() + "." + el.getMethodName() + ":" + el.getLineNumber());
            }
            finishDemo("FAILED sweep " + e, false);
        }
    }

    // Use the app's own internal files dir — always writable by the app process.
    // Resolves to /data/data/com.digivasserver.demo/files/demo_result.txt
    private String getMarkerPath() {
        return new java.io.File(getFilesDir(), "demo_result.txt").getAbsolutePath();
    }

    private void runDroidGuardDemo(final IDroidGuardService svc) {
        // Run on a background thread so we can pace the steps with sleeps while the UI
        // thread keeps rendering each log line -> the screenrecord shows progressive flow.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    showIntro();
                    step(900);
                    log("IDroidGuardService obtained");
                    step(500);
                    IDroidGuardHandle handle = svc.getHandle();
                    log("handle: " + handle.getClass().getSimpleName());
                    step(600);

                    DroidGuardResultsRequest request = new DroidGuardResultsRequest();
                    request.bundle.putString("nonce", "demo-2026");
                    log("initWithRequest(flow=playintegrity)");
                    step(500);
                    handle.initWithRequest("playintegrity", request);
                    step(700);

                    Map<Object, Object> data = new HashMap<>();
                    data.put("app", "com.digivasserver.demo");
                    data.put("step", "1");
                    log("snapshot(...) -> remote server begin+snapshot");
                    step(500);
                    byte[] blob = handle.snapshot(data);
                    log("snapshot bytes=" + blob.length);
                    log("token(text)=" + new String(blob, StandardCharsets.UTF_8));
                    step(700);

                    log("close()");
                    handle.close();
                    step(800);
                    finishDemo("DEMO-COMPLETE", true);
                } catch (Exception e) {
                    log("demo threw: " + e);
                    for (StackTraceElement el : e.getStackTrace()) {
                        log("  at " + el.getClassName() + "." + el.getMethodName() + ":" + el.getLineNumber());
                    }
                    finishDemo("FAILED " + e.getClass().getSimpleName(), false);
                }
            }
        }, "demo-run").start();
    }

    private void step(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private void showIntro() {
        log("microG :: remote DroidGuard (Network mode)");
        log("--------------------------------------------");
        log("client proxy -> GmsCore broker -> IDroidGuardService");
        log("NetworkHandleFactory -> RemoteHandleImpl -> mock backend");
        log("session: begin -> snapshot -> close");
        log("--------------------------------------------");
    }

    private void finishDemo(final String marker, final boolean success) {
        done = true;
        log("=== " + marker + " ===");
        // Blink the result colour a few times so the recording isn't a static frame.
        final int good = Color.rgb(0x2e, 0x7d, 0x32);
        final int bad = Color.rgb(0xb3, 0x1b, 0x1b);
        for (int i = 0; i < 3; i++) {
            ui.post(new Runnable() {
                @Override
                public void run() {
                    logView.setTextColor(success ? good : bad);
                }
            });
            step(180);
            ui.post(new Runnable() {
                @Override
                public void run() {
                    logView.setTextColor(Color.BLACK);
                }
            });
            step(180);
        }
        ui.post(new Runnable() {
            @Override
            public void run() {
                logView.setTextColor(success ? good : bad);
            }
        });
        // Hold the success screen so the recording clearly captures the final state.
        step(4500);
                writeMarker(getMarkerPath(), marker, ui);
        try {
            unbindService(connection);
        } catch (Exception ignored) {
        }
    }

    private void writeMarker(final String path, final String content, Handler h) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    java.io.File f = new java.io.File(path);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                    fos.write((content + "\n").getBytes(StandardCharsets.UTF_8));
                    fos.close();
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            log("wrote " + path);
                        }
                    });
                } catch (Exception e) {
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            log("marker write failed: " + e);
                        }
                    });
                }
            }
        }).start();
    }

    private void log(final String line) {
        android.util.Log.i(TAG, line);
        ui.post(new Runnable() {
            @Override
            public void run() {
                logBuf.append(line).append("\n");
                logView.setText(logBuf.toString());
            }
        });
    }

    private static String now() {
        return String.valueOf(android.os.SystemClock.elapsedRealtime());
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        logView = new TextView(this);
        logView.setTextSize(14);
        logView.setTypeface(android.graphics.Typeface.MONOSPACE);
        logView.setTextColor(Color.DKGRAY);
        root.addView(logView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        scroll.addView(root);
        setContentView(scroll);
    }
}