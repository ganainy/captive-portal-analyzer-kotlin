package com.example.captive_portal_analyzer_kotlin.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException

class LocalOrRemoteCaptiveChecker(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    data class PortalAnalysisResult(
        val isLocal: Boolean,
        val portalUrl: String?,
        val ipAddress: String?,
        val isPrivateIP: Boolean,
        val responseTime: Long,
        val dnsResolutionTime: Long
    )

    suspend fun analyzePortal(testUrl: String = "http://connectivitycheck.gstatic.com/generate_204"): PortalAnalysisResult {
        return withContext(Dispatchers.IO) {
            try {
                // Get current network
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)

                if (network == null || capabilities == null) {
                    throw IOException("No active network connection")
                }

                // Start DNS resolution timing
                val dnsStart = System.currentTimeMillis()
                val inetAddress = InetAddress.getByName(URL(testUrl).host)
                val dnsTime = System.currentTimeMillis() - dnsStart

                val ipAddress = inetAddress.hostAddress
                val isPrivateIP = isPrivateIPAddress(ipAddress)

                // Measure response time
                val startTime = System.currentTimeMillis()
                val connection = network.openConnection(URL(testUrl)) as HttpURLConnection

                try {
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.requestMethod = "GET"

                    val responseCode = connection.responseCode
                    val responseTime = System.currentTimeMillis() - startTime

                    // If we get a redirect, analyze the redirect location
                    val redirectUrl = if (responseCode in 300..399) {
                        connection.getHeaderField("Location")
                    } else null

                    // Determine if portal is local based on multiple factors
                    val isLocal = determineIfLocal(
                        isPrivateIP = isPrivateIP,
                        responseTime = responseTime,
                        redirectUrl = redirectUrl,
                        originalUrl = testUrl
                    )

                    PortalAnalysisResult(
                        isLocal = isLocal,
                        portalUrl = redirectUrl,
                        ipAddress = ipAddress,
                        isPrivateIP = isPrivateIP,
                        responseTime = responseTime,
                        dnsResolutionTime = dnsTime
                    )
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                throw IOException("Failed to analyze portal: ${e.message}")
            }
        }
    }

    private fun isPrivateIPAddress(ipAddress: String?): Boolean {
        if (ipAddress == null) return false

        return try {
            val parts = ipAddress.split(".").map { it.toInt() }
            when {
                // 10.0.0.0 - 10.255.255.255
                parts[0] == 10 -> true
                // 172.16.0.0 - 172.31.255.255
                parts[0] == 172 && parts[1] in 16..31 -> true
                // 192.168.0.0 - 192.168.255.255
                parts[0] == 192 && parts[1] == 168 -> true
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun determineIfLocal(
        isPrivateIP: Boolean,
        responseTime: Long,
        redirectUrl: String?,
        originalUrl: String
    ): Boolean {
        // Multiple factors to consider:
        // 1. Private IP address
        // 2. Very fast response time (likely local)
        // 3. Redirect to a local domain/IP

        if (isPrivateIP) return true

        // If response time is very low (< 20ms), likely local
        if (responseTime < 20) return true

        // Check if redirect URL points to a local address
        redirectUrl?.let {
            try {
                val redirectHost = URL(it).host
                if (redirectHost.endsWith(".local") ||
                    redirectHost.endsWith(".lan") ||
                    redirectHost.contains("192.168.") ||
                    redirectHost.contains("10.") ||
                    redirectHost.startsWith("172.")) {
                    return true
                }
            } catch (e: Exception) {
                // Invalid URL, ignore
            }
        }

        return false
    }

    // Additional utility method to monitor network traffic
    fun startTrafficMonitoring(network: Network): NetworkTrafficInfo {
        val uid = android.os.Process.myUid()
        val initialRx = TrafficStats.getUidRxBytes(uid)
        val initialTx = TrafficStats.getUidTxBytes(uid)

        return NetworkTrafficInfo(initialRx, initialTx)
    }

    data class NetworkTrafficInfo(
        val initialRxBytes: Long,
        val initialTxBytes: Long
    ) {
        fun getTrafficDelta(): Pair<Long, Long> {
            val uid = android.os.Process.myUid()
            val currentRx = TrafficStats.getUidRxBytes(uid)
            val currentTx = TrafficStats.getUidTxBytes(uid)

            return Pair(
                currentRx - initialRxBytes,
                currentTx - initialTxBytes
            )
        }
    }
}