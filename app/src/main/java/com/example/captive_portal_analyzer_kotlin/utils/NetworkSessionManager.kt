package com.example.captive_portal_analyzer_kotlin.utils

import NetworkSessionRepository
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NetworkSessionManager(private val context: Context, private val repository: NetworkSessionRepository) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Add mutex to prevent concurrent session creation
    private val sessionMutex = Mutex()

    // Keep track of current session to avoid unnecessary database queries
    private var currentSession: NetworkSessionEntity? = null

    suspend fun getCurrentSessionId(): String? {
        return getCurrentSession()?.networkId
    }

    private fun isValidWifiConnection(ssid: String, bssid: String): Boolean {
        val isValid = bssid.isNotEmpty() &&
                bssid != "02:00:00:00:00:00" &&
                ssid.isNotEmpty() &&
                ssid != "<unknown ssid>" &&
                ssid != "0x" &&
                ssid != "null" &&
                bssid.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"))

        if (!isValid) {
            Log.d("WiFiSession", "Invalid WiFi connection: SSID=$ssid, BSSID=$bssid")
        }

        return isValid
    }

    private suspend fun getCurrentSession(): NetworkSessionEntity? {
        return sessionMutex.withLock {
                              try {
                if (!hasRequiredPermissions()) {
                    Log.d("WiFiSession", "Missing required permissions")
                    return null
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isLocationEnabled()) {
                    Log.d("WiFiSession", "Location services disabled")
                    return null
                }

                val networkCapabilities = connectivityManager.getNetworkCapabilities(
                    connectivityManager.activeNetwork
                ) ?: return null

                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    val wifiInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // Android 12 and above
                        val network = connectivityManager.activeNetwork
                        network?.let {
                            (context.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                                .getConnectionInfo()
                        }
                    } else {
                        // Below Android 12
                        @Suppress("DEPRECATION")
                        wifiManager.connectionInfo
                    } ?: return null

                    // Get SSID based on Android version
                    val ssid = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                            wifiInfo.ssid.removeSurrounding("\"")
                        }
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                            context.mainExecutor.let { executor ->
                                suspendCoroutine { continuation ->
                                    wifiManager.connectionInfo?.let { info ->
                                        continuation.resume(info.ssid.removeSurrounding("\""))
                                    } ?: continuation.resume("<unknown ssid>")
                                }
                            }
                        }
                        else -> {
                            @Suppress("DEPRECATION")
                            wifiInfo.ssid.removeSurrounding("\"")
                        }
                    }

                    val bssid = wifiInfo.bssid

                    Log.d("WiFiSession", "SSID: $ssid, BSSID: $bssid")

                    if (!isValidWifiConnection(ssid, bssid)) {
                        Log.d("WiFiSession", "Invalid WiFi connection")
                        return null
                    }

                    val dhcpInfo = wifiManager.dhcpInfo
                    val gatewayAddress = intToIpAddress(dhcpInfo.gateway)
                    val dhcpServerAddress = intToIpAddress(dhcpInfo.serverAddress)

                    val networkId = generateNetworkIdentifier(ssid, bssid, gatewayAddress, dhcpServerAddress)

                    // Check cached session
                    if (currentSession?.networkId == networkId) {
                        return currentSession
                    }

                    // Check database
                    val existingSession = repository.getSessionByNetworkId(networkId)
                    if (existingSession != null) {
                        currentSession = existingSession
                        return existingSession
                    }

                    // Create new session
                    val newSession = NetworkSessionEntity(
                        ssid = ssid,
                        bssid = bssid,
                        networkId = networkId,
                        timestamp = System.currentTimeMillis(),
                        ipAddress = intToIpAddress(dhcpInfo.ipAddress),
                        gatewayAddress = gatewayAddress
                    )

                    repository.insertSession(newSession)
                    currentSession = newSession
                    return newSession
                }
                null
            } catch (e: Exception) {
                Log.e("WiFiSession", "Error getting current session", e)
                null
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                checkPermission(Manifest.permission.NEARBY_WIFI_DEVICES) &&
                        checkPermission(Manifest.permission.ACCESS_WIFI_STATE)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                        checkPermission(Manifest.permission.ACCESS_WIFI_STATE)
            }
            else -> {
                checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                        checkPermission(Manifest.permission.ACCESS_WIFI_STATE)
            }
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Helper function to check if location services are enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun generateNetworkIdentifier(ssid: String, bssid: String, gatewayAddress: String?, dhcpServerAddress: String?): String {
        val rawIdentifier = "$ssid-$bssid-$gatewayAddress-$dhcpServerAddress"
        return rawIdentifier.hashCode().toString()
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

}
