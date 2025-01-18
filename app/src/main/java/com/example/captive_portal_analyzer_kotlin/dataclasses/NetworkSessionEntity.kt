package com.example.captive_portal_analyzer_kotlin.dataclasses

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single network session with its associated data.
 * This is the entity that is used to store data in the room database.
 *
 * @property networkId a unique identifier for the network session gotten by hashing the network ssid and gateway
 * @property ssid the name of the network
 * @property bssid the MAC address of the network
 * @property timestamp the timestamp when the network session was started
 * @property captivePortalUrl the url of the captive portal if present
 * @property ipAddress the ip address that the device received from the dhcp server
 * @property gatewayAddress the ip address of the gateway of the network
 * @property securityType the type of security used by the network , TODO STILL NOT IMPLEMENTED
 * @property isCaptiveLocal whether the captive portal is hosted only locally or has remote auth server
 * @property isUploadedToRemoteServer whether the session data has been uploaded to the remote server
 * or only exist on user device
 */
@Entity(tableName = "network_sessions")
data class NetworkSessionEntity(
    @PrimaryKey val networkId: String,
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