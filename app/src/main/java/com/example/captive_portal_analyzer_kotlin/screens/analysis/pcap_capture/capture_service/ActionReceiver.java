package com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.example.captive_portal_analyzer_kotlin.CaptivePortalApp;
import com.example.captive_portal_analyzer_kotlin.R;
import com.example.captive_portal_analyzer_kotlin.screens.pcap_setup.Utils;

public class ActionReceiver extends BroadcastReceiver {
    public static final String EXTRA_UNBLOCK_APP = "unblock_app";
    private static final String TAG = "TAG";

    @Override
    public void onReceive(Context context, Intent intent) {
        String unblock_app = intent.getStringExtra(EXTRA_UNBLOCK_APP);

        if((unblock_app != null) && !unblock_app.isEmpty()) {
            Log.d(TAG, "unblock_app: " + unblock_app);
            Blocklist blocklist = CaptivePortalApp.getInstance().getBlocklist();
            blocklist.removeApp(unblock_app);
            blocklist.saveAndReload();

            // remove notification
            NotificationManagerCompat man = NotificationManagerCompat.from(context);
            man.cancel(CaptureService.NOTIFY_ID_APP_BLOCKED);

            AppDescriptor app = AppsResolver.resolveInstalledApp(context.getPackageManager(), unblock_app, 0);
            String label = (app != null) ? app.getName() : unblock_app;
            Utils.showToastLong(context, R.string.app_unblocked, label);
        }
    }
}