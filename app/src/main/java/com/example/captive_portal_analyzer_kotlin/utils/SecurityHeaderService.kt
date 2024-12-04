package com.example.captive_portal_analyzer_kotlin.utils

import com.example.captive_portal_analyzer_kotlin.dataclasses.SecurityHeadersAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class HeaderCollectionResult(
    val directHeaders: Map<String, List<String>>,
    val requestHeaders: Map<String, List<String>>
)

class SecurityHeaderService {
    suspend fun collectSecurityHeaders(url: String): Pair<HeaderCollectionResult, Any> {
        // 1. Collect headers directly
        val directHeaders = fetchHeadersDirectly(url)

        // 2. Create a channel for collecting additional request headers
        val requestHeadersChannel = Channel<Pair<String, Map<String, String>>>(Channel.BUFFERED)

        // 3. Provide a way to intercept headers without directly accessing WebView
        val headerInterceptor = object {
            fun onRequestHeadersCollected(requestUrl: String, headers: Map<String, String>) {
                // Launch a coroutine to send headers to the channel
                GlobalScope.launch {
                    requestHeadersChannel.send(requestUrl to headers)
                }
            }
        }

        // 4. Return a result that can be used to configure WebView in the Composable
        return HeaderCollectionResult(
            directHeaders = directHeaders,
            requestHeaders = emptyMap()
        ) to headerInterceptor
    }

    private suspend fun fetchHeadersDirectly(url: String): Map<String, List<String>> {
        return withContext(Dispatchers.IO) {
            val collectedHeaders = mutableMapOf<String, List<String>>()

            try {
                val urlObj = URL(url)
                val connection: HttpURLConnection = if (url.startsWith("https")) {
                    urlObj.openConnection() as HttpsURLConnection
                } else {
                    urlObj.openConnection() as HttpURLConnection
                }

                // Set common browser headers
                connection.apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                    )
                    setRequestProperty("Accept-Language", "en-US,en;q=0.5")
                    connect()
                }

                // Collect security-related headers
                val responseHeaders = connection.headerFields
                responseHeaders.forEach { (key, value) ->
                    if (isSecurityHeader(key)) {
                        collectedHeaders[key] = value
                    }
                }

                // Add additional security headers
                val additionalHeaders = listOf(
                    "Content-Security-Policy-Report-Only",
                    "Permissions-Policy",
                    "Cross-Origin-Embedder-Policy",
                    "Cross-Origin-Opener-Policy",
                    "Cross-Origin-Resource-Policy"
                )
                for (header in additionalHeaders) {
                    connection.getHeaderField(header)?.let {
                        collectedHeaders[header] = listOf(it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            collectedHeaders
        }
    }

    private fun isSecurityHeader(headerName: String?): Boolean {
        if (headerName.isNullOrEmpty()) return false

        val headerLower = headerName.lowercase()
        return listOf(
            "security",
            "content-security",
            "strict-transport",
            "x-frame",
            "x-content-type",
            "x-xss",
            "referrer",
            "permissions",
            "cross-origin",
            "access-control"
        ).any { headerLower.contains(it) }
    }

    private fun analyzeSecurityHeaders(collectedHeaders: Map<String, List<String>>): SecurityHeadersAnalysis {
        val analysis = SecurityHeadersAnalysis()
        val presentHeaders = mutableMapOf<String, String>()
        val missingHeaders = mutableListOf<String>()
        val weakHeaders = mutableListOf<String>()

        // Define security headers to check
        val requiredHeaders = listOf(
            "Strict-Transport-Security",
            "Content-Security-Policy",
            "X-Content-Type-Options",
            "X-Frame-Options",
            "X-XSS-Protection"
        )

        // Check each required header
        for (header in requiredHeaders) {
            val headerValue = collectedHeaders[header]?.joinToString(", ")
            if (headerValue != null) {
                presentHeaders[header] = headerValue
            } else {
                missingHeaders.add(header)
            }
        }

        // Define weak headers to flag if the values are less secure
        val weakHeaderPatterns = mapOf(
            "Strict-Transport-Security" to "max-age=0", // Example condition for weak header
            "Content-Security-Policy" to "unsafe-inline" // Example condition for weak header
        )

        for ((header, weakValue) in weakHeaderPatterns) {
            val headerValue = collectedHeaders[header]?.joinToString(", ")
            if (headerValue != null && headerValue.contains(weakValue)) {
                weakHeaders.add(header)
            }
        }

        // Score the headers based on their presence and strength
        val score = calculateSecurityHeaderScore(presentHeaders, missingHeaders, weakHeaders)

        // Set analysis results
        analysis.presentHeaders = presentHeaders
        analysis.missingHeaders = missingHeaders
        analysis.weakHeaders = weakHeaders
        analysis.score = score

        return analysis
    }

    private fun calculateSecurityHeaderScore(
        presentHeaders: Map<String, String>,
        missingHeaders: List<String>,
        weakHeaders: List<String>
    ): SecurityHeadersAnalysis.SecurityHeaderScore {
        val score = SecurityHeadersAnalysis.SecurityHeaderScore()

        // Calculate score based on missing and weak headers
        val totalHeaders = presentHeaders.size + missingHeaders.size + weakHeaders.size
        val missingPercentage = (missingHeaders.size.toDouble() / totalHeaders) * 100
        val weakPercentage = (weakHeaders.size.toDouble() / totalHeaders) * 100

        score.total = (100 - missingPercentage - weakPercentage).toInt()

        // Assign letter grade based on score
        score.grade = when {
            score.total >= 90 -> "A+"
            score.total >= 80 -> "A"
            score.total >= 70 -> "B"
            score.total >= 60 -> "C"
            score.total >= 50 -> "D"
            else -> "F"
        }

        // Provide recommendations based on weak or missing headers
        score.recommendations = mutableListOf<String>().apply {
            if (missingHeaders.isNotEmpty()) add("Consider adding missing security headers: ${missingHeaders.joinToString()}")
            if (weakHeaders.isNotEmpty()) add("Strengthen weak security headers: ${weakHeaders.joinToString()}")
        }

        return score
    }

}