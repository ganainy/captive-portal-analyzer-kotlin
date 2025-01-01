package com.example.captive_portal_analyzer_kotlin.room.network_session

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_sessions")
data class NetworkSessionEntity(
    @PrimaryKey val sessionId: String,
    val ssid: String? = null,
    val bssid: String? = null,
    val timestamp: Long = 0,
    val captivePortalUrl: String? = null,
    val ipAddress: String? = null,
    val gatewayAddress: String? = null,
    val securityType: String? = null,
    val isCaptiveLocal: Boolean? = null,
    val isUploadedToRemoteServer: Boolean = false
)