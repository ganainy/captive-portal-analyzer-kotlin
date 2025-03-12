package com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service;

import androidx.annotation.NonNull;

public class AppStats implements Cloneable {
    private final int uid;
    public long sentBytes;
    public long rcvdBytes;
    public int numConnections;
    public int numBlockedConnections;

    public AppStats(int _uid) {
        uid = _uid;
    }

    public int getUid() {
        return uid;
    }

    @NonNull
    public AppStats clone() {
        AppStats rv = new AppStats(uid);
        rv.sentBytes = sentBytes;
        rv.rcvdBytes = rcvdBytes;
        rv.numConnections = numConnections;
        rv.numBlockedConnections = numBlockedConnections;

        return rv;
    }
}
