package com.example.captive_portal_analyzer_kotlin.utils

import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ProtocolException
import java.net.Socket
import java.net.URL
import javax.net.SocketFactory

/**
 * Utility class for detecting captive portals by resolving URLs to specific IP addresses
 * and analyzing the HTTP response.
 */
object CaptivePortalDetector {
    /**
     * Detects a captive portal by sending an HTTP GET request to a given test URL,
     * resolving it to a fixed IP address, and checking for redirect responses.
     *
     * @param testUrl The URL to test.
     * @return The redirect URL if found, otherwise a message indicating no redirect.
     * @throws IOException If an I/O error occurs during detection.
     */
    @Throws(IOException::class)
    fun detectPortalURL(testUrl: String): String {
        val url = URL(testUrl)
        val host = url.host

        // Manually resolve the host to a specific IP address (e.g., 8.8.8.8).
        val conn = CustomURLConnection(url, "8.8.8.8")

        // Configure the connection.
        conn.requestMethod = "GET"
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 5000
        conn.readTimeout = 5000

        // Add headers similar to those used by curl.
        conn.setRequestProperty("Host", host) // Important! Set the original host.
        conn.setRequestProperty("Cache-Control", "no-cache")
        conn.setRequestProperty("Pragma", "no-cache")

        try {
            // Get the HTTP response code.
            val responseCode = conn.responseCode
            println("Response Code: $responseCode")

            // Check for redirect status codes.
            if (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308) {
                // Retrieve the 'Location' header, which specifies the redirect URL.
                val redirectUrl = conn.getHeaderField("Location")
                if (redirectUrl != null) {
                    println("Found redirect URL: $redirectUrl")
                    return redirectUrl
                }
            }

            // Additional check for network authentication status codes.
            if (responseCode == 511) {
                println("Network Authentication Required (511). Captive portal may be present.")
                val redirectUrl = conn.getHeaderField("Location")
                if (redirectUrl != null) {
                    println("Found redirect URL: $redirectUrl")
                    return redirectUrl
                }
            }

            // Handle unauthorized or forbidden responses that might indicate a captive portal.
            if (responseCode == 401 || responseCode == 403) {
                println("Access Denied (401 or 403). Captive portal may require login.")
                val redirectUrl = conn.getHeaderField("Location")
                if (redirectUrl != null) {
                    println("Found redirect URL: $redirectUrl")
                    return redirectUrl
                }
            }

            return "No redirect found"
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Custom SocketFactory implementation that allows socket creation with a manually
     * resolved IP address instead of relying on DNS resolution.
     */
    internal class CustomSocketFactory
    /**
     * Constructor for the custom socket factory.
     *
     * @param host      The original host name.
     * @param resolvedIp The manually resolved IP address.
     */(private val host: String, private val resolvedIp: String) : SocketFactory() {
        @Throws(IOException::class)
        override fun createSocket(): Socket {
            val socket = Socket()
            socket.connect(InetSocketAddress(resolvedIp, 80))
            return socket
        }

        @Throws(IOException::class)
        override fun createSocket(host: String, port: Int): Socket {
            return createSocket()
        }

        @Throws(IOException::class)
        override fun createSocket(host: InetAddress, port: Int): Socket {
            return createSocket()
        }

        @Throws(IOException::class)
        override fun createSocket(
            host: String,
            port: Int,
            localHost: InetAddress,
            localPort: Int
        ): Socket {
            return createSocket()
        }

        @Throws(IOException::class)
        override fun createSocket(
            address: InetAddress,
            port: Int,
            localAddress: InetAddress,
            localPort: Int
        ): Socket {
            return createSocket()
        }
    }

    /**
     * Custom HttpURLConnection implementation that delegates HTTP requests
     * to a connection established with a manually resolved IP address.
     */
    class CustomURLConnection(url: URL, resolvedIp: String?) : HttpURLConnection(url) {
        private val delegate: HttpURLConnection

        /**
         * Constructor for the custom URL connection.
         *
         * @param url        The original URL.
         * @param resolvedIp The manually resolved IP address.
         * @throws IOException If an I/O error occurs.
         */
        init {
            val actualUrl = URL(url.protocol, resolvedIp, url.port, url.file)
            delegate = actualUrl.openConnection() as HttpURLConnection
        }

        override fun disconnect() {
            delegate.disconnect()
        }

        override fun usingProxy(): Boolean {
            return delegate.usingProxy()
        }

        @Throws(IOException::class)
        override fun connect() {
            delegate.connect()
        }

        // Override all necessary methods to delegate to the actual connection
        @Throws(IOException::class)
        override fun getResponseCode(): Int {
            return delegate.responseCode
        }

        override fun getHeaderField(name: String): String {
            return delegate.getHeaderField(name)
        }

        override fun setRequestProperty(key: String, value: String) {
            delegate.setRequestProperty(key, value)
        }

        @Throws(ProtocolException::class)
        override fun setRequestMethod(method: String) {
            delegate.requestMethod = method
        }

        override fun setInstanceFollowRedirects(followRedirects: Boolean) {
            delegate.instanceFollowRedirects = followRedirects
        }

        override fun setConnectTimeout(timeout: Int) {
            delegate.connectTimeout = timeout
        }

        override fun setReadTimeout(timeout: Int) {
            delegate.readTimeout = timeout
        }
    }
}