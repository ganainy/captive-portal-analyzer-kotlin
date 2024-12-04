package com.example.captive_portal_analyzer_kotlin.dataclasses

import android.net.wifi.ScanResult

data class NetworkItem(
    val scanResult: ScanResult,
    val isSecured: Boolean,
    val securityIcon: Int // Resource ID for the icon
)
