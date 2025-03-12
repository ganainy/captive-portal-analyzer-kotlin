package com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture

fun interface CaptureStartListener {
    fun onCaptureStartResult(success: Boolean)
}