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
            // DroidGuardChimeraService.onBind() returns a DroidGuardServiceBroker
            // (IGmsServiceBroker), NOT the IDroidGuardService directly. So rawBinder
            // is the broker; drive the standard getService protocol through it.

            GetServiceRequest request = new GetServiceRequest(DROID_GUARD_SERVICE_ID);
            request.packageName = getPackageName();
            request.gmsVersion = 0;

            // ground truth from the installed ci-repro GmsCore DEX:
            //   IGmsServiceBroker$Stub.onTransact -> getService = 42
            //   (validateAccount = 46, getWalletServiceWithPackageName = 47,
            //    INTERFACE_TRANSACTION = 0x5f4e5446)
            // The client compiled getService=45 (mismatch) so try 42 first, then the
            // rest; treat the CALLBACK firing (status 0 + service binder) as success.
            final int[] codes = {42, 45, 41, 25, 26, 24, 23, 46, 47, 28};
            log("broker code sweep: " + java.util.Arrays.toString(codes));
            for (final int code : codes) {
                cbResult = null;
                cbLatch = new java.util.concurrent.CountDownLatch(1);
                log("try code " + code + " @ " + now());
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                boolean transacted = false;
                try {
                    data.writeInterfaceToken("com.google.android.gms.common.internal.IGmsServiceBroker");
                    data.writeStrongBinder(callbacks.asBinder());
                    request.writeToParcel(data, 0);
                    transacted = rawBinder.transact(code, data, reply, 0);
                    reply.readException();
                    log("code " + code + " transact=" + transacted + " @ " + now());
                } catch (Exception e) {
                    log("code " + code + " threw: " + e);
                } finally {
                    data.recycle();
                    reply.recycle();
                }
                try {
                    boolean fired = cbLatch.await(6, java.util.concurrent.TimeUnit.SECONDS);
                    log("code " + code + " callbackFired=" + fired);
                } catch (InterruptedException ie) {
                    log("sweep interrupted: " + ie);
                }
                Object got = cbResult;
                if (got instanceof IDroidGuardService) {
                    log("MATCHED code " + code + " -> IDroidGuardService");
                    runDroidGuardDemo((IDroidGuardService) got);
                    return;
                }
                if (got != null) {
                    log("code " + code + " produced " + got + " (not droidguard)");
                }
            }
            finishDemo("FAILED no broker code matched");
        } catch (Exception e) {
            log("sweep threw: " + e);
            for (StackTraceElement el : e.getStackTrace()) {
                log("  at " + el.getClassName() + "." + el.getMethodName() + ":" + el.getLineNumber());
            }
            finishDemo("FAILED sweep " + e);
        }
    }

    private void runDroidGuardDemo(IDroidGuardService svc) {
        try {
            log("IDroidGuardService obtained");
            IDroidGuardHandle handle = svc.getHandle();
            log("handle: " + handle.getClass().getSimpleName());

            DroidGuardResultsRequest request = new DroidGuardResultsRequest();
            request.bundle.putString("nonce", "demo-2026");
            log("initWithRequest(flow=playintegrity)");
            handle.initWithRequest("playintegrity", request);

            Map<Object, Object> data = new HashMap<>();
            data.put("app", "com.digivasserver.demo");
            data.put("step", "1");
            log("snapshot(...) -> remote server begin+snapshot");
            Bundle args = new Bundle();
            for (Map.Entry<Object, Object> e : data.entrySet()) {
                args.putString(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
            byte[] blob = handle.snapshot(args);
            log("snapshot bytes=" + blob.length);
            log("token(text)=" + new String(blob, StandardCharsets.UTF_8));

            log("close()");
            handle.close();
            finishDemo("DEMO-COMPLETE");
        } catch (Exception e) {
            log("demo threw: " + e);
            for (StackTraceElement el : e.getStackTrace()) {
                log("  at " + el.getClassName() + "." + el.getMethodName() + ":" + el.getLineNumber());
            }
            finishDemo("FAILED " + e.getClass().getSimpleName());
        }
    }

    private void finishDemo(String marker) {
        done = true;
        log("=== " + marker + " ===");
        logView.setTextColor(marker.startsWith("DEMO") ? Color.rgb(0x2e, 0x7d, 0x32)
                : Color.rgb(0xb3, 0x1b, 0x1b));
        writeMarker(getApplicationInfo().dataDir + "/demo_result.txt", marker, ui);
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