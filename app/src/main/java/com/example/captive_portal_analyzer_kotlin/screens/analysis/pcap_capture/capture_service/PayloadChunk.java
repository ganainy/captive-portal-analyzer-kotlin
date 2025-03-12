package com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service;


import java.io.Serializable;

// A piece of payload. It may or may not correspond to a packet
public class PayloadChunk implements Serializable {
    public byte[] payload;
    public boolean is_sent;
    public long timestamp;
    public ChunkType type;
    public String contentType;
    public String path;

    // Serializable need in ConnectionPayload fragment
    public enum ChunkType implements Serializable {
        RAW,
        HTTP,
        WEBSOCKET
    }

    public PayloadChunk(byte[] _payload, ChunkType _type, boolean _is_sent, long _timestamp) {
        payload = _payload;
        type = _type;
        is_sent = _is_sent;
        timestamp = _timestamp;
    }

    public PayloadChunk subchunk(int start, int size) {
        byte[] subarr = new byte[size];
        System.arraycopy(payload, start, subarr, 0, size);
        return new PayloadChunk(subarr, type, is_sent, timestamp);
    }

    public PayloadChunk withPayload(byte[] the_payload) {
        return new PayloadChunk(the_payload, type, is_sent, timestamp);
    }
}
