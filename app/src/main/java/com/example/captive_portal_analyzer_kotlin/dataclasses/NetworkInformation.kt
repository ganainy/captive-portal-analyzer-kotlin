package com.example.captive_portal_analyzer_kotlin.dataclasses

data class NetworkInformation(
    val ssid: String?,
    val bssid: String?,
    val ipAddress: String?,
    val gatewayAddress: String?,
    val linkSpeed: Int?,
    val rssi: Int?,
    val macAddress: String?,
    val frequency: Int?,
    val channel: Int?,
    val securityType: String?,
    val dnsServers: List<String>?,
    val isSecure: Boolean
)