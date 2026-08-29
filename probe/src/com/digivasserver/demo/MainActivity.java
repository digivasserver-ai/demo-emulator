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

    private volatile IGmsServiceBroker broker;
    private volatile boolean done = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            log("binder: " + name.flattenToShortString());
            broker = IGmsServiceBroker.Stub.asInterface(binder);
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
                log("onPostInitComplete status=" + statusCode);
                finishDemo("FAILED pre-init " + statusCode);
                return;
            }
            IDroidGuardService svc = IDroidGuardService.Stub.asInterface(binder);
            runDroidGuardDemo(svc);
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
            GetServiceRequest request = new GetServiceRequest(DROID_GUARD_SERVICE_ID);
            request.packageName = getPackageName();
            request.gmsVersion = 0;
            log("broker.getService(serviceId=" + DROID_GUARD_SERVICE_ID + ")");
            broker.getService(callbacks, request);
        } catch (Exception e) {
            log("getService threw: " + e);
            finishDemo("FAILED getService " + e);
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