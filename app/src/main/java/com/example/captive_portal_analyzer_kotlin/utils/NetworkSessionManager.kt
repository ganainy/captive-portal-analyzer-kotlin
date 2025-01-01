package com.example.captive_portal_analyzer_kotlin.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import com.example.captive_portal_analyzer_kotlin.room.network_session.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.room.network_session.OfflineNetworkSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
class NetworkSessionManager(private val context: Context, private val repository: OfflineNetworkSessionRepository) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Add mutex to prevent concurrent session creation
    private val sessionMutex = Mutex()

    // Keep track of current session to avoid unnecessary database queries
    private var currentSession: NetworkSessionEntity? = null

    private fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }

    suspend fun getCurrentSessionId(): String? {
        return getCurrentSession()?.sessionId
    }

    private fun isValidWifiConnection(ssid: String, bssid: String): Boolean {
        return bssid.isNotEmpty() &&
                bssid != "02:00:00:00:00:00" &&
                ssid.isNotEmpty() &&
                ssid != "<unknown ssid>" &&
                ssid != "0x" &&
                ssid != "null" &&
                bssid.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) // Valid MAC address format
    }

    private suspend fun getCurrentSession(): NetworkSessionEntity? {
        // Use mutex to ensure thread-safety when checking/creating sessions
        return sessionMutex.withLock {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(
                connectivityManager.activeNetwork
            ) ?: return null

            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiInfo = wifiManager.connectionInfo
                val bssid = wifiInfo.bssid
                val ssid = wifiInfo.ssid.removeSurrounding("\"")

                if (!isValidWifiConnection(ssid, bssid)) {
                    return null
                }

                // First check our cached session
                if (currentSession?.bssid == bssid) {
                    return currentSession
                }

                // Then check the database
                val existingSession = repository.getSessionByBssid(bssid)
                if (existingSession != null) {
                    currentSession = existingSession
                    return existingSession
                }

                // Only create new session if none exists
                val dhcpInfo = wifiManager.dhcpInfo
                val newSession = NetworkSessionEntity(
                    sessionId = generateSessionId(),
                    ssid = ssid,
                    bssid = bssid,
                    timestamp = System.currentTimeMillis(),
                    ipAddress = intToIpAddress(dhcpInfo.ipAddress),
                    gatewayAddress = intToIpAddress(dhcpInfo.gateway),
                )

                // Save new session to database
                repository.insertSession(newSession)
                currentSession = newSession
                return newSession
            }
            null
        }
    }

    private fun intToIpAddress(ip: Int): String {
        return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
    }

    suspend fun savePortalUrl(sessionId: String, portalUrl: String) {
        repository.updatePortalUrl(sessionId, portalUrl)
    }

    suspend fun saveIsCaptiveLocal(sessionId: String, value: Boolean) {
        repository.updateIsCaptiveLocal(sessionId, value)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            handleNetworkChange()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            handleNetworkChange()
            // Clear current session when network is lost
            currentSession = null
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            handleNetworkChange()
        }
    }

    // Consolidated network change handling
    private fun handleNetworkChange() {
        scope.launch {
            getCurrentSession()
        }
    }

    fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        scope.launch {
            getCurrentSession()
        }
    }

    // Add cleanup method
    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            // Callback was not registered or already unregistered
        }
        scope.cancel()
        currentSession = null
    }
}
