package com.example.captive_portal_analyzer_kotlin.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

// DataStore instance
val Context.sessionDataStore by preferencesDataStore(name = "network_sessions")

class NetworkSessionManager(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    data class NetworkSessionInfo(
        val sessionId: String,
        val ssid: String?,
        val bssid: String?,
        val timestamp: Long
    )

    companion object {
        private val SESSION_ID_KEY = stringPreferencesKey("current_session_id")
        private val NETWORK_SSID_KEY = stringPreferencesKey("current_network_ssid")
        private val NETWORK_BSSID_KEY = stringPreferencesKey("current_network_bssid")
        private val SESSION_TIMESTAMP_KEY = stringPreferencesKey("session_timestamp")
        private val SESSION_CAPTIVE_URL_KEY = stringPreferencesKey("captive_url")
        private val IS_CAPTIVE_LOCAL_KEY = booleanPreferencesKey("is_captive_local")
    }

    // Generate a unique session ID
    private fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }

    // Get current network information
    private fun getCurrentNetworkInfo(): NetworkSessionInfo? {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork
        )

        // Check if we're on WiFi
        if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            val wifiInfo = wifiManager.connectionInfo
            return NetworkSessionInfo(
                sessionId = generateSessionId(),
                ssid = wifiInfo.ssid.removeSurrounding("\""),
                bssid = wifiInfo.bssid,
                timestamp = System.currentTimeMillis()
            )
        }
        return null
    }

    // Save session information to DataStore
    suspend fun saveSessionInfo(sessionInfo: NetworkSessionInfo) {
        context.sessionDataStore.edit { preferences ->
            preferences[SESSION_ID_KEY] = sessionInfo.sessionId
            preferences[NETWORK_SSID_KEY] = sessionInfo.ssid ?: ""
            preferences[NETWORK_BSSID_KEY] = sessionInfo.bssid ?: ""
            preferences[SESSION_TIMESTAMP_KEY] = sessionInfo.timestamp.toString()
        }
    }

    //save portal url to DataStore
    suspend fun savePortalUrl(portalUrl:String) {
        context.sessionDataStore.edit { preferences ->
            preferences[SESSION_CAPTIVE_URL_KEY] = portalUrl
        }
    }

    //save if portal is local or remote  to DataStore
    suspend fun saveIsCaptiveLocal( value: Boolean) {
        context.sessionDataStore.edit { preferences ->
            preferences[IS_CAPTIVE_LOCAL_KEY] = value
        }
    }

    // Get current session information from DataStore
    fun getSessionInfo(): Flow<NetworkSessionInfo?> {
        return context.sessionDataStore.data.map { preferences ->
            val sessionId = preferences[SESSION_ID_KEY]
            val ssid = preferences[NETWORK_SSID_KEY]
            val bssid = preferences[NETWORK_BSSID_KEY]
            val timestamp = preferences[SESSION_TIMESTAMP_KEY]?.toLongOrNull()

            if (sessionId != null && ssid != null && bssid != null && timestamp != null) {
                NetworkSessionInfo(sessionId, ssid, bssid, timestamp)
            } else {
                null
            }
        }
    }

    // Network callback to monitor network changes
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            updateSessionIfNeeded()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            updateSessionIfNeeded()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateSessionIfNeeded()
        }
    }

    // Update session if network has changed
    private fun updateSessionIfNeeded() {
        val currentNetworkInfo = getCurrentNetworkInfo()

        kotlinx.coroutines.GlobalScope.launch {
            val existingSession = getSessionInfo().map { it }.collect { existingSession ->
                if (shouldUpdateSession(existingSession, currentNetworkInfo)) {
                    currentNetworkInfo?.let { saveSessionInfo(it) }
                }
            }
        }
    }

    private fun shouldUpdateSession(
        existingSession: NetworkSessionInfo?,
        currentNetwork: NetworkSessionInfo?
    ): Boolean {
        if (existingSession == null || currentNetwork == null) return true

        // Compare network identifiers
        return existingSession.ssid != currentNetwork.ssid ||
                existingSession.bssid != currentNetwork.bssid
    }

    // Register network callback
    fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        updateSessionIfNeeded() // Initial check
    }

    // Unregister network callback
    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            // Callback was not registered
        }
    }
}