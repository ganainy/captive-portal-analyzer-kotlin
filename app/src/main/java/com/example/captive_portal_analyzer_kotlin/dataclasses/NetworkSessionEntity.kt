package com.example.captive_portal_analyzer_kotlin.dataclasses

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_sessions")
data class NetworkSessionEntity(
    @PrimaryKey val sessionId: String,
    val ssid: String?,
    val bssid: String?,
    val timestamp: Long,
    val captivePortalUrl: String?=null,
    val ipAddress: String?,
    val gatewayAddress: String?,
    val securityType: String?=null,
    val isCaptiveLocal: Boolean?=null,
    val isUploadedToRemoteServer: Boolean=false
)

