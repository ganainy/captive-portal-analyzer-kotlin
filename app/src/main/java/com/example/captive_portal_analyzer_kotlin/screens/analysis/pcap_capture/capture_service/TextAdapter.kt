package com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service

interface TextAdapter {
    fun getItemText(pos: Int): String?
    fun getCount(): Int
}