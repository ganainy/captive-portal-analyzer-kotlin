import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

sealed class CaptivePortalResult {
    data class Portal(val url: String): CaptivePortalResult()
    object NoPortal: CaptivePortalResult()
    data class Error(val type: ErrorType, val message: String): CaptivePortalResult()

    enum class ErrorType {
        NO_INTERNET,
        TIMEOUT,
        NETWORK_ERROR,
        UNKNOWN
    }
}

class CaptivePortalDetector(private val context: Context) {
    companion object {
        private const val TIMEOUT_MS = 5000
        private val TEST_URLS = listOf(
            "http://clients3.google.com/generate_204",
            "http://connectivitycheck.gstatic.com/generate_204",
            "http://www.google.com/generate_204"
        )
    }

    fun detectCaptivePortal(): CaptivePortalResult {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: Network = connectivityManager.activeNetwork
            ?: return CaptivePortalResult.Error(
                CaptivePortalResult.ErrorType.NO_INTERNET,
                "No active network connection"
            )

        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (networkCapabilities == null || !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return CaptivePortalResult.Error(
                CaptivePortalResult.ErrorType.NO_INTERNET,
                "No internet capability"
            )
        }

        // Try each URL in sequence until one works
        for (testUrl in TEST_URLS) {
            try {
                val result = checkUrl(testUrl, activeNetwork)
                if (result != null) {
                    return result
                }
            } catch (e: Exception) {
                // Continue to next URL if one fails
                continue
            }
        }

        // If all URLs failed, return the last error
        return CaptivePortalResult.Error(
            CaptivePortalResult.ErrorType.NETWORK_ERROR,
            "All portal detection attempts failed"
        )
    }

    private fun checkUrl(urlString: String, network: Network): CaptivePortalResult? {
        return try {
            val url = URL(urlString)
            val connection = network.openConnection(url) as HttpURLConnection

            connection.apply {
                setRequestProperty("User-Agent", "Android")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = false

                // Add additional headers that some captive portals might expect
                setRequestProperty("Connection", "close")
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            }

            try {
                val responseCode = connection.responseCode
                val locationHeader = connection.getHeaderField("Location")

                when {
                    responseCode == 204 -> CaptivePortalResult.NoPortal
                    responseCode in 300..399 && !locationHeader.isNullOrBlank() ->
                        CaptivePortalResult.Portal(locationHeader)
                    responseCode != 204 && !locationHeader.isNullOrBlank() ->
                        CaptivePortalResult.Portal(locationHeader)
                    responseCode != 204 ->
                        CaptivePortalResult.Portal(urlString)
                    else -> null
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: SocketTimeoutException) {
            CaptivePortalResult.Error(
                CaptivePortalResult.ErrorType.TIMEOUT,
                "Connection timed out: ${e.localizedMessage}"
            )
        } catch (e: IOException) {
            CaptivePortalResult.Error(
                CaptivePortalResult.ErrorType.NETWORK_ERROR,
                "Network error: ${e.localizedMessage}"
            )
        } catch (e: Exception) {
            CaptivePortalResult.Error(
                CaptivePortalResult.ErrorType.UNKNOWN,
                "Unknown error: ${e.localizedMessage}"
            )
        }
    }
}