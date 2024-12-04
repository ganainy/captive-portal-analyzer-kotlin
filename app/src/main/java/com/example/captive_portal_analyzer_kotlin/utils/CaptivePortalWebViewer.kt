package com.example.captive_portal_analyzer_kotlin.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CaptivePortalWebViewer(private val context: Context, private val webView: WebView) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkRequest = NetworkRequest.Builder().apply {
        addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        // Set up the WebView with proper configurations
        setupWebView()
        // Launch the coroutine to monitor network connectivity
        scope.launch {
            monitorNetworkConnectivity()
        }
    }

    /**
     * Setup WebView with necessary configurations.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings

        settings.apply {
            setJavaScriptEnabled(true)
            setDomStorageEnabled(true)
            setDatabaseEnabled(true)
            setCacheMode(WebSettings.LOAD_NO_CACHE) // Disable cache for captive portals.
            setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW) // Allow mixed content.
            setUserAgentString("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
        }

        // Set a custom WebViewClient to handle URL loading within the WebView.
        webView.webViewClient = CustomWebViewClient()
    }

    /**
     * Coroutine function to monitor network connectivity and notify if network is disconnected.
     */
    private suspend fun monitorNetworkConnectivity() {
        withContext(Dispatchers.IO) {
            awaitNetworkDisconnection()
        }
    }

    /**
     * This function suspends and waits until the network is disconnected.
     */
    private suspend fun awaitNetworkDisconnection() {
        return suspendCoroutine { continuation ->
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    // When network is lost, we resume the coroutine to notify about disconnection.
                    continuation.resume(Unit)
                }

                override fun onUnavailable() {
                    // Optionally handle cases when network is unavailable
                }
            }

            // Register network callback to listen for disconnection events
            connectivityManager.registerNetworkCallback(networkRequest.build(), callback)
        }
    }

    /**
     * Load the captive portal URL into the WebView.
     */
    fun loadPortalPage(portalUrl: String) {
        webView.loadUrl(portalUrl)
    }

    /**
     * Custom WebViewClient to ensure WebView handles URLs internally.
     */
    private class CustomWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            // Let WebView handle the URL within itself
            return false
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            // Allow WebView to load resources normally
            return super.shouldInterceptRequest(view, request)
        }
    }


}