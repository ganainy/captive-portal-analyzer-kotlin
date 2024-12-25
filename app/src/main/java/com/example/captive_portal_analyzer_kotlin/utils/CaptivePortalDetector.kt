import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.HttpURLConnection
import java.net.URL

fun detectCaptivePortal(context: Context): String? {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork: Network = connectivityManager.activeNetwork ?: return null

    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    if (networkCapabilities == null || !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
        return null
    }

    try {
        // URL commonly used to detect captive portals
        val url = URL("http://clients3.google.com/generate_204") //http://connectivitycheck.gstatic.com/generate_204
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Android")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.instanceFollowRedirects = false // Do not follow redirects

        val responseCode = connection.responseCode
        val locationHeader = connection.getHeaderField("Location")

        // Check if the response code is not 204 (no content)
        if (responseCode != 204) {
            // Captive portal detected
            // Return the URL to which the request was redirected
            return locationHeader ?: "Unknown Captive Portal URL"
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // No captive portal detected
    return null
}