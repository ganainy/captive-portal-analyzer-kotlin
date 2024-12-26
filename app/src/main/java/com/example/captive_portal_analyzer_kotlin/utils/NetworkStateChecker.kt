package com.example.captive_portal_analyzer_kotlin.utils

import android.content.Context
import android.util.Log
import com.fabricethilaw.sonarnet.ConnectivityCallback
import com.fabricethilaw.sonarnet.ConnectivityResult
import com.fabricethilaw.sonarnet.InternetStatus
import com.fabricethilaw.sonarnet.SonarNet
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI


/**
 * Checks if there is an internet connection to the base domain of a given URL.
 *
 * This method extracts the base domain name from the input string (stripping any
 * extra parameters, paths, etc.) and then attempts to resolve its IP address
 * and establish a connection on a standard port (80 for HTTP or 443 for HTTPS).
 *
 * @param url The URL or domain name to check (e.g., "www.google.com/search?q=test", "example.com").
 * @param timeoutMs The timeout in milliseconds for the connection attempt.
 * @param useHttps Whether to attempt connection on port 443 (HTTPS) or 80 (HTTP).
 * @return `true` if a connection to the base domain can be established within the timeout, `false` otherwise.
 */
fun hasInternetConnectionToBaseDomain(url: String, timeoutMs: Int = 10000, useHttps: Boolean = true): Boolean {
    return try {
        // Extract the base domain name using URI
        val uri = URI(url)
        val host = uri.host ?: run {
            // If URI parsing fails to get a host, assume the input is already a domain
            if (url.contains("/") || url.contains("?") || url.contains("#")) {
                // If it looks like a URL but no host, it's likely invalid for our purpose
                println("Warning: Could not extract base domain from '$url'.")
                return false
            }
            url // Treat the input as the domain
        }

        val inetAddress = InetAddress.getByName(host) // Resolve the base domain to an IP address
        if (inetAddress.isAnyLocalAddress || inetAddress.isLoopbackAddress) {
            // Domain resolves to a local address, which doesn't indicate internet connectivity
            return false
        }

        val socket = Socket()
        val port = if (useHttps) 443 else 80
        val socketAddress = InetSocketAddress(inetAddress, port)
        socket.connect(socketAddress, timeoutMs)
        socket.isConnected
    } catch (e: Exception) {
        // Handle exceptions like URISyntaxException, UnknownHostException,
        // SocketTimeoutException, and IOException
        println("Error checking internet connection to base domain of '$url': ${e.message}")
        false
    }
}

/**
 * Retrieves and monitors the network state of the device.
 *
 * This function checks the internet status when the application is first launched
 * and registers a callback to be notified whenever the network status changes.
 *
 * @param context The application context used to access system services.
 */
private fun getNetworkState(context: Context) {

    // Get status first time on app open
    SonarNet.ping { result ->
        when (result) {
            InternetStatus.INTERNET -> {}
            InternetStatus.NO_INTERNET -> {}
            InternetStatus.CAPTIVE_PORTAL -> {}
        }
    }

    // Get notified every time network changes
    val connectivityCallback = object : ConnectivityCallback {
        override fun onConnectionChanged(result: ConnectivityResult) {
            // Check the result, see the Using Results section
            when (result.internetStatus) {
                InternetStatus.INTERNET -> {
                    Log.d("LandingViewModel", "INTERNET")
                }
                InternetStatus.NO_INTERNET -> {
                    Log.d("LandingViewModel", "NO INTERNET")
                }
                InternetStatus.CAPTIVE_PORTAL -> {
                    Log.d("LandingViewModel", "CAPTIVE")
                }
            }
        }
    }
    SonarNet(context).registerConnectivityCallback(connectivityCallback)
}