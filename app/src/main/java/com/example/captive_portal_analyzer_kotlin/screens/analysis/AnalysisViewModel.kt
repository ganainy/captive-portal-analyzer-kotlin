package com.example.captive_portal_analyzer_kotlin.screens.analysis

import NetworkSessionRepository
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.utils.LocalOrRemoteCaptiveChecker
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import detectCaptivePortal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.reflect.KFunction4

/**
 * A sealed class representing the different UI states for analysis.
 */
sealed class AnalysisUiState {

    /**
     * Represents the loading state with a message resource ID.
     *
     * @property messageStringResource The resource ID for the loading message.
     */
    data class Loading(val messageStringResource: Int) : AnalysisUiState()

    /**
     * Represents the state when a captive URL has been detected.
     */
    object CaptiveUrlDetected : AnalysisUiState()

    /**
     * Represents the state when analysis is complete.
     */
    object AnalysisComplete : AnalysisUiState()

    /**
     * Enum class for error types during analysis.
     */
    enum class ErrorType {
        CannotDetectCaptiveUrl,
        Unknown,
    }

    /**
     * Represents an error state with a specific error type.
     *
     * @property type The type of error that occurred.
     */
    data class Error(val type: ErrorType) : AnalysisUiState()
}
/**
 * Enum class to represent the type of the WebView. The WebView can be of type
 * NormalWebView which is the default WebView type or CustomWebView which is
 * a custom WebView that can intercept network requests body.
 */
enum class WebViewType {
    NormalWebView,
    CustomWebView
}
/**
 * The ViewModel for the analysis screen.
 *
 * @property application The application context.
 * @property sessionManager The manager for the network sessions.
 * @property repository The repository for the network sessions.
 * @property showToast A function to show a toast message. The function takes the title of the toast,
 * the message to display, the style of the toast, and the duration of the toast as parameters.
 */
class AnalysisViewModel(
    application: Application,
    private val sessionManager: NetworkSessionManager,
    private val repository: NetworkSessionRepository,
    private val showToast: (title:String?, message:String, style:ToastStyle, duration:Long?) -> Unit,
) : AndroidViewModel(application) {
    /**
     * Gets the application context.
     */
    private val context: Context get() = getApplication<Application>().applicationContext

    /**
     * The mutable state flow to hold the UI state of the analysis screen.
     *
     * The UI state can be an instance of Loading, CaptiveUrlDetected, AnalysisComplete, or Error.
     */
    private val _uiState =
        MutableStateFlow<AnalysisUiState>(AnalysisUiState.Loading(R.string.detecting_captive_portal_page))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    /**
     * The mutable state flow to hold the detected captive portal URL.
     */
    private val _portalUrl = MutableStateFlow<String?>(null)
    val portalUrl: StateFlow<String?> get() = _portalUrl.asStateFlow()

    /**
     * The mutable state flow to hold the type of the WebView.
     *
     * The type of the WebView can be either NormalWebView or CustomWebView.
     */
    private val _webViewType = MutableStateFlow<WebViewType>(WebViewType.CustomWebView)
    val webViewType = _webViewType.asStateFlow()

    /**
     * The mutable state flow to hold a boolean flag indicating whether the hint has been shown or not.
     *
     * The hint is a dialog box that provides information on how to login to the captive portal and end the analysis process.
     */
    private val _showedHint = MutableStateFlow<Boolean>(false)
    val showedHint = _showedHint.asStateFlow()

    /**
     * Gets the captive portal address.
     *
     * If the address is detected, the UI state is updated to [AnalysisUiState.CaptiveUrlDetected], the
     * detected address is saved to the current session, and the local or remote captive portal is detected.
     *
     * If the address cannot be detected, the UI state is updated to [AnalysisUiState.Error] with
     * error type [AnalysisUiState.ErrorType.CannotDetectCaptiveUrl].
     *
     * @param showToast a lambda to show a toast message.
     */
    fun getCaptivePortalAddress(showToast: (message: String, style: ToastStyle) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val portalUrl = detectCaptivePortal(context)
                if (portalUrl != null) {
                    _uiState.value = AnalysisUiState.CaptiveUrlDetected
                    updateUrl(portalUrl)
                    val sessionId = sessionManager.getCurrentSessionId()
                    if (sessionId != null) {
                        sessionManager.savePortalUrl(
                            portalUrl = portalUrl,
                            sessionId = sessionId
                        )
                    } else {
                        showToast(context.getString(R.string.unknown_session_id), ToastStyle.ERROR)
                    }
                    detectLocalOrRemoteCaptivePortal(
                        context,
                    )

                } else {
                    _uiState.value =
                        AnalysisUiState.Error(AnalysisUiState.ErrorType.CannotDetectCaptiveUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value =
                        AnalysisUiState.Error(AnalysisUiState.ErrorType.CannotDetectCaptiveUrl)
                }
            }
        }
    }
    /**
     * Detects whether the captive portal is local or remote.
     *
     * The analysis is done by [LocalOrRemoteCaptiveChecker] and the result is saved to the current session.
     * If the session ID is unknown, an error toast is shown.
     *
     * @param context the context of the app.
     */
    private fun detectLocalOrRemoteCaptivePortal(
        context: Context,
    ) {
        val analyzer = LocalOrRemoteCaptiveChecker(context)

        viewModelScope.launch {
            try {
                val result = analyzer.analyzePortal()

                val sessionId = sessionManager.getCurrentSessionId()
                if (sessionId != null) {
                    sessionManager.saveIsCaptiveLocal(
                        value = result.isLocal,
                        sessionId = sessionId
                    )
                } else {
                    showToast(null,context.getString(R.string.unknown_session_id), ToastStyle.ERROR,null)
                }
            } catch (e: IOException) {
                println("Failed to analyze portal: ${e.message}")
            }
        }

    }

    /**
     * Updates the URL, which will be passed to the UI to show in the WebView.
     *
     * The URL is stored in [_portalUrl] and is exposed to the UI through the [portalUrl]
     * state flow.
     *
     * @param newUrl the new URL to show in the WebView.
     */
    fun updateUrl(newUrl: String) {
        _portalUrl.value = newUrl
    }

    /**
     * Saves the WebView request, including the body, to the local database.
     *
     * This function first retrieves the current session ID and then converts the WebViewRequest
     * to a CustomWebViewRequestEntity using the current session ID. The resulting entity is
     * then inserted into the repository.
     *
     * @param request The WebViewRequest to be saved.
     */
    suspend fun saveWebViewRequest(request: WebViewRequest) {
        val currentSessionId = sessionManager.getCurrentSessionId()
        val customWebViewRequest =
            getCustomWebViewRequestFromWebViewRequest(request, currentSessionId)
        repository.insertRequest(
            customWebViewRequest
        )
    }
    /**
     * Converts a [WebViewRequest] to a [CustomWebViewRequestEntity].
     *
     * This function takes a [WebViewRequest] and a session ID and converts it to a
     * [CustomWebViewRequestEntity] that can be stored in the Room database.
     *
     * @param request the [WebViewRequest] to be converted.
     * @param sessionId the session ID of the current session.
     * @return a [CustomWebViewRequestEntity] containing the information from the
     * [WebViewRequest] and the session ID.
     */
    private fun getCustomWebViewRequestFromWebViewRequest(
        request: WebViewRequest,
        sessionId: String?
    ): CustomWebViewRequestEntity {
        return CustomWebViewRequestEntity(
            sessionId = sessionId,
            url = request.url,
            method = request.method,
            headers = request.headers.toString(),
            body = request.body,
            type = request.type.name,
        )
    }

    /**
     * Saves a [WebResourceRequest] to the local database.
     *
     * This function takes a [WebResourceRequest] and saves it to the local database. If the
     * request is null, nothing is done.
     *
     * @param request the [WebResourceRequest] to be saved.
     */
    suspend fun saveWebResourceRequest(request: WebResourceRequest?) {
        if (request != null) {
            val currentSessionId = sessionManager.getCurrentSessionId()
            val customWebViewRequest =
                getCustomWebViewRequestFromWebResourceRequest(request, currentSessionId)
            repository.insertRequest(
                customWebViewRequest
            )
        }
    }

    /**
     * Converts a [WebResourceRequest] to a [CustomWebViewRequestEntity].
     *
     * This function takes a [WebResourceRequest] and a session ID and converts it to a
     * [CustomWebViewRequestEntity] that can be stored in the Room database.
     *
     * @param request the [WebResourceRequest] to be converted.
     * @param sessionId the session ID of the current session.
     * @return a [CustomWebViewRequestEntity] containing the information from the
     * [WebResourceRequest] and the session ID.
     */
    private fun getCustomWebViewRequestFromWebResourceRequest(
        request: WebResourceRequest,
        sessionId: String?
    ): CustomWebViewRequestEntity {
        return CustomWebViewRequestEntity(
            sessionId = sessionId,
            url = request.url.toString(),
            method = request.method,
            headers = request.requestHeaders.toString(),
        )
    }
    /**
     * Switches the type of the WebView used in the analysis screen.
     *
     * This function takes a [showToast] function that is used to display a toast
     * message when the WebView type is switched.
     *
     * The [showToast] function takes a message and a [ToastStyle] as parameters.
     *
     * The WebView type is switched between [WebViewType.NormalWebView] and
     * [WebViewType.CustomWebView].
     *
     * When the WebView type is switched, a toast message is displayed indicating
     * that the detection method has been switched.
     *
     * @param showToast the function used to display a toast message.
     */
    fun switchWebViewType(
        showToast: (message: String, style: ToastStyle) -> Unit
    ) {
        when (_webViewType.value) {
            WebViewType.NormalWebView -> _webViewType.value = WebViewType.CustomWebView
            WebViewType.CustomWebView -> _webViewType.value = WebViewType.NormalWebView
        }
        showToast(context.getString(R.string.switched_detection_method), ToastStyle.SUCCESS)
    }
    /**
     * Saves the content of the given [WebView] to the local database.
     *
     * This function takes a [WebView], a URL, and a [showToast] function. It first
     * checks if the session ID is known. If not, it displays an error toast.
     * Otherwise, it captures the HTML and JavaScript content of the WebView and
     * saves it to the local database.
     *
     * The [showToast] function takes a message and a [ToastStyle] as parameters.
     *
     * @param webView the WebView from which to capture the content.
     * @param url the URL of the webpage to be saved.
     * @param showToast a function used to display a toast message.
     */
    fun saveWebpageContent(
        webView: WebView,
        url: String,
        showToast: (message: String, style: ToastStyle) -> Unit
    ) {
        viewModelScope.launch {
            val currentSessionId = sessionManager.getCurrentSessionId()
            if (currentSessionId == null) {
                showToast(context.getString(R.string.unknown_session_id), ToastStyle.ERROR)
            } else {
                // Capture HTML content
                webView.evaluateJavascript(
                    "(function() { return document.documentElement.outerHTML; })();",
                ) { html ->
                    // Capture JavaScript content
                    webView.evaluateJavascript(
                        """
                (function() {
                    var scripts = document.getElementsByTagName('script');
                    var jsContent = '';
                    for(var i = 0; i < scripts.length; i++) {
                        if(scripts[i].src) {
                            jsContent += '// External Script: ' + scripts[i].src + '\n';
                        } else {
                            jsContent += scripts[i].innerHTML + '\n';
                        }
                    }
                    return jsContent;
                })();
                """.trimIndent()
                    ) { javascript ->
                        // Save to database
                        viewModelScope.launch(Dispatchers.IO) {
                            val content = WebpageContentEntity(
                                url = url,
                                htmlContent = html.unescapeJsonString(),
                                jsContent = javascript.unescapeJsonString(),
                                sessionId = currentSessionId,
                                timestamp = System.currentTimeMillis(),
                            )
                            repository.insertWebpageContent(content)
                        }
                    }
                }
            }
        }
    }
    /**
     * Checks if the device has a full internet connection.
     * This method checks if the device is connected to a network and if that network has internet access.
     * This is done by checking if the device can ping a reliable website (Google).
     * If the device can ping the website successfully, it is considered to have a full internet connection.
     *
     * @return true if the device has a full internet connection, false otherwise
     */
    private suspend fun hasFullInternetAccess(context: Context): Boolean {
        // Check network connectivity first
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val isNetworkConnected = networkCapabilities?.run {
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || hasTransport(
                        NetworkCapabilities.TRANSPORT_CELLULAR
                    ))
        } == true

        // If not connected, return false
        if (!isNetworkConnected) {
            return false
        }

        // Try to ping a website to check if internet is accessible
        return try {
            val url = URL("https://www.google.com") // Use any reliable website
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = 2000 // Set timeout to 2 seconds
                readTimeout = 2000
            }

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode == HttpURLConnection.HTTP_OK // Check if we received a successful response
        } catch (e: Exception) {
            false // If any error occurs (no internet, timeout, etc.), return false
        }
    }
    /**
     * This function is triggered when the user clicks the stop analysis button.
     *
     * if device has full internet connection, it will complete the analysis, otherwise it will
     *  call the [onUncompletedAnalysis] function which will show a dialog warning the user that the
     *  analysis is not completed since they have no full internet connection which they should have
     *  if they successfully logged into the captive portal network.
     *
     * @param onUncompletedAnalysis a lambda that will be called if the device is not
     * connected to the internet.
     */
    fun stopAnalysis(onUncompletedAnalysis: () -> Unit) {
        _uiState.value = AnalysisUiState.Loading(R.string.processing_request)
        viewModelScope.launch(Dispatchers.IO) {
            if (hasFullInternetAccess(context)) {
                withContext(Dispatchers.Main) {
                    _uiState.value = AnalysisUiState.AnalysisComplete
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.value = AnalysisUiState.CaptiveUrlDetected
                    onUncompletedAnalysis()
                }
            }
        }

    }

    /**
     * Take a screenshot of the web view and save it to the file system,
     * also insert the screenshot details into the database.
     *
     * @param webView The web view to capture
     * @param url The current URL of the web view
     *
     * This function will capture the bitmap of the web view using the
     * capturePicture() method or the isDrawingCacheEnabled property for older
     * versions of Android. If the bitmap is valid, it will be saved to the file
     * system using the current session ID as the directory name and the
     * timestamp as the file name. The screenshot will also be inserted into
     * the database using the
     * [ScreenshotEntity] data class.
     *
     *  If the file saving operation fails, an error message will be shown with the error message.
     */
    fun takeScreenshot(webView: WebView, url: String) {
        // Launch a coroutine on the Main dispatcher for WebView operations
        viewModelScope.launch(Dispatchers.Main) {
            // Get session ID on IO thread
            val currentSessionId = withContext(Dispatchers.IO) {
                sessionManager.getCurrentSessionId()
            }

            // Capture bitmap on Main thread since it involves WebView
            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android 8.0 (API 26) or later
                webView.capturePicture()?.let { picture ->
                    if (picture.width > 0 && picture.height > 0) {
                        Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
                            .apply { picture.draw(Canvas(this)) }
                    } else null
                }
            } else {
                // Fallback for older versions
                webView.isDrawingCacheEnabled = true
                val drawingCache = webView.drawingCache
                if (drawingCache != null && drawingCache.width > 0 && drawingCache.height > 0) {
                    Bitmap.createBitmap(drawingCache)
                } else null
            }

            // Switch to IO thread for file operations and database insertion
            withContext(Dispatchers.IO) {
                bitmap?.let {
                    val directory =
                        File(webView.context.getExternalFilesDir(null), "$currentSessionId/screenshots")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }

                    val fileName = "${System.currentTimeMillis()}.png"
                    val file = File(directory, fileName)

                    try {
                        FileOutputStream(file).use { out ->
                            it.compress(Bitmap.CompressFormat.PNG, 100, out)
                            Log.i("WebViewScreenshot", "Screenshot saved to: ${file.absolutePath}")
                        }
                        if(currentSessionId != null) {

                        // Insert into the database
                        val screenshotEntity = ScreenshotEntity(
                            sessionId = currentSessionId,
                            timestamp = System.currentTimeMillis(),
                            path = file.absolutePath,
                            size = "${file.length()} bytes",
                            url = url
                        )
                        repository.insertScreenshot(screenshotEntity) // Already on IO thread
                        }else{
                            showToast(null,"Error saving screenshot: Session Id is null", ToastStyle.ERROR,null)
                            Log.e("WebViewScreenshot", "currentSessionId is null")
                        }

                    } catch (e: IOException) {
                        Log.e("WebViewScreenshot", "Error saving screenshot: ${e.message}")
                    }
                } ?: Log.e("WebViewScreenshot", "Invalid screenshot bitmap dimensions")
            }
        }
    }

    /**
     * Updates the state of whether the hint has been shown.
     *
     * @param showedHint A boolean indicating the new state of the hint visibility.
     */
    fun updateShowedHint(showedHint: Boolean) {
        _showedHint.value = showedHint
    }


}
/**
 * Unescape a string that was escaped for JSON to make the HTML and JS content stored more readable.
 *
 * For example, if the input is a string that was escaped like this:
 * "\"Hello, \\\"world\\\"!\""
 * This function will return: "Hello, \"world\"!"
 *
 * If the string is not properly escaped for JSON, this function will return the original string.
 *
 * @param s The string to unescape.
 * @return The unescaped string.
 */
private fun String.unescapeJsonString(): String {
    return if (this.startsWith("\"") && this.endsWith("\"")) {
        this.substring(1, this.length - 1)
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    } else {
        this
    }
}

/**
 * A factory for creating instances of [AnalysisViewModel].
 *
 * @param application The application context.
 * @param sessionManager The manager for the network sessions.
 * @param repository The repository for the network sessions.
 * @param showToast A function to show a toast message. The function takes the title of the toast,
 * the message to display, the style of the toast, and the duration of the toast as parameters.
 */
class AnalysisViewModelFactory(
    private val application: Application,
    private val sessionManager: NetworkSessionManager,
    private val repository: NetworkSessionRepository,
    private val showToast: KFunction4<String?, String, ToastStyle, Long?, Unit>,
) : ViewModelProvider.Factory {
    /**
     * Creates an instance of a ViewModel.
     *
     * @param modelClass The class of the ViewModel to create.
     * @return An instance of the ViewModel.
     * @throws IllegalArgumentException If the modelClass is not a subclass of [AnalysisViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalysisViewModel(
                application,
                sessionManager,
                repository,
                showToast,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


