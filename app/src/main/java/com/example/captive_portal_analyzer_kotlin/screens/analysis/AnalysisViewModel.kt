package com.example.captive_portal_analyzer_kotlin.screens.analysis

import CaptivePortalDetector
import CaptivePortalResult
import NetworkSessionRepository
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.NetworkOnMainThreadException
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.convertMethodStringToEnum
import com.example.captive_portal_analyzer_kotlin.utils.LocalOrRemoteCaptiveChecker
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * A sealed class representing the different UI states for analysis.
 */
sealed class AnalysisUiState {


    object PreferenceSetup :
        AnalysisUiState()// initial setup to let user choose to continue with or without PCAPDroid packet capture

    data class Loading(val messageStringResource: Int) : AnalysisUiState()
    object CaptiveUrlDetected : AnalysisUiState()
    object AnalysisComplete : AnalysisUiState()

    /**
     * Enum class for error types during analysis.
     */
    enum class ErrorType {
        CannotDetectCaptiveUrl,
        Unknown,
        NoInternet,
        Timeout,
        NetworkError,
        PermissionDenied,
        InvalidState
    }

    data class Error(val type: ErrorType) : AnalysisUiState()
}



data class AnalysisUiData
    (
    val portalUrl: String? = null,
    val webViewType: WebViewType = WebViewType.CustomWebView,
    val showedHint: Boolean = false,
    val analysisStatus: AnalysisStatus = AnalysisStatus.INITIAL, // initially is at Initial then
    // when user click end analysis the function stopAnalysis
    // checks for internet connection if true this means the registration to the captive
    // portal is Completed is analysis is most likely done otherwise this is set to NotCompleted
    // to warn user analysis might not be completed
)

enum class AnalysisStatus {
    INITIAL,
    COMPLETED,
    NOT_COMPLETED
}

/**
 * Enum class to represent the type of the WebView. The WebView can be of type
 * NormalWebView which is the default WebView type or CustomWebView which is
 * a custom WebView that can intercept network requests body.
 */
enum class WebViewType {
    NormalWebView,
    CustomWebView,
    TestingWebView // for testing purposes only
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
    private val showToast: (message: String, style: ToastStyle) -> Unit,
    private val mainViewModel: MainViewModel
) : AndroidViewModel(application) {


    companion object {
        private const val TAG = "AnalysisViewModel"
    }

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
        MutableStateFlow<AnalysisUiState>(AnalysisUiState.PreferenceSetup)

    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    /**
     * The mutable state flow to hold the ui data of the screen.
     */
    private val _uiData = MutableStateFlow(AnalysisUiData())
    val uiData: StateFlow<AnalysisUiData> get() = _uiData.asStateFlow()


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

        _uiState.update {
            (AnalysisUiState.Loading(R.string.detecting_captive_portal_page))
        }

        // Launch coroutine in IO dispatcher for network operations
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val detector = CaptivePortalDetector(context)
                // Attempt to detect captive portal and handle different results
                when (val result = detector.detectCaptivePortal()) {
                    is CaptivePortalResult.Portal -> handlePortalDetected(result.url, showToast)
                    is CaptivePortalResult.NoPortal -> handleNoPortal()
                    is CaptivePortalResult.Error -> handlePortalError(result)
                    else -> handleUnknownError()
                }
            } catch (e: Exception) {
                handleException(e, showToast)
            }
        }
    }

    // Handle successful portal detection
    private suspend fun handlePortalDetected(
        portalUrl: String,
        showToast: (message: String, style: ToastStyle) -> Unit
    ) {
        _uiState.value = AnalysisUiState.CaptiveUrlDetected
        updateUrl(portalUrl)

        // Save portal URL to current session
        sessionManager.getCurrentSessionId()?.let { sessionId ->
            sessionManager.savePortalUrl(
                portalUrl = portalUrl,
                sessionId = sessionId
            )
        } ?: run {
            // Show error if session ID is missing
            withContext(Dispatchers.Main) {
                showToast(
                    context.getString(R.string.unknown_session_id),
                    ToastStyle.ERROR
                )
            }
        }

        detectLocalOrRemoteCaptivePortal(context)
    }

    // Handle case when no portal is detected
    private fun handleNoPortal() {
        _uiState.value = AnalysisUiState.Error(AnalysisUiState.ErrorType.CannotDetectCaptiveUrl)
    }

    // Handle different types of portal detection errors
    private fun handlePortalError(result: CaptivePortalResult.Error) {
        _uiState.value = when (result.type) {
            CaptivePortalResult.ErrorType.NO_INTERNET -> AnalysisUiState.Error(
                AnalysisUiState.ErrorType.NoInternet
            ).also { Log.e("AnalysisViewModel", "No internet connection detected") }

            CaptivePortalResult.ErrorType.TIMEOUT -> AnalysisUiState.Error(
                AnalysisUiState.ErrorType.Timeout
            ).also { Log.e("AnalysisViewModel", "Connection timed out: ${result.message}") }

            CaptivePortalResult.ErrorType.NETWORK_ERROR -> AnalysisUiState.Error(
                AnalysisUiState.ErrorType.NetworkError
            ).also { Log.e("AnalysisViewModel", "Network error occurred: ${result.message}") }

            CaptivePortalResult.ErrorType.UNKNOWN -> AnalysisUiState.Error(
                AnalysisUiState.ErrorType.Unknown
            ).also { Log.e("AnalysisViewModel", "Unknown error occurred: ${result.message}") }
        }
    }

    // Handle unknown portal detection result
    private fun handleUnknownError() {
        _uiState.value = AnalysisUiState.Error(AnalysisUiState.ErrorType.Unknown)
        Log.e("AnalysisViewModel", "Received unexpected portal detection result")
    }

    // Handle exceptions during portal detection
    private suspend fun handleException(
        exception: Exception,
        showToast: (message: String, style: ToastStyle) -> Unit
    ) {
        // Log the full stack trace
        Log.e("AnalysisViewModel", "Exception during portal detection: ${exception.message}")

        withContext(Dispatchers.Main) {
            // Show appropriate error message based on exception type
            when (exception) {
                is SecurityException -> {
                    _uiState.value =
                        AnalysisUiState.Error(AnalysisUiState.ErrorType.PermissionDenied)
                    showToast(
                        context.getString(R.string.permission_denied_error),
                        ToastStyle.ERROR
                    )
                }

                is NetworkOnMainThreadException -> {
                    _uiState.value = AnalysisUiState.Error(AnalysisUiState.ErrorType.NetworkError)
                    Log.e("AnalysisViewModel", "Network operation attempted on main thread")
                }

                is IllegalStateException -> {
                    _uiState.value = AnalysisUiState.Error(AnalysisUiState.ErrorType.InvalidState)
                    Log.e("AnalysisViewModel", "Invalid state: ${exception.message}")
                }

                else -> {
                    _uiState.value =
                        AnalysisUiState.Error(AnalysisUiState.ErrorType.CannotDetectCaptiveUrl)
                    Log.e("AnalysisViewModel", "Unhandled exception: ${exception.message}")
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
                    showToast(
                        context.getString(R.string.unknown_session_id),
                        ToastStyle.ERROR,
                    )
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
        _uiData.value = _uiData.value.copy().copy(portalUrl = newUrl)
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
    private suspend fun getCustomWebViewRequestFromWebViewRequest(
        request: WebViewRequest,
        sessionId: String?
    ): CustomWebViewRequestEntity {
        val hasFullInternet = hasFullInternetAccess(context)
        return CustomWebViewRequestEntity(
            sessionId = sessionId,
            url = request.url,
            method = convertMethodStringToEnum(request.method),
            headers = request.headers.toString(),
            body = request.body,
            type = request.type.name,
            hasFullInternetAccess = hasFullInternet,
            timestamp = System.currentTimeMillis()
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
    private suspend fun getCustomWebViewRequestFromWebResourceRequest(
        request: WebResourceRequest,
        sessionId: String?
    ): CustomWebViewRequestEntity {
        val hasFullInternet = hasFullInternetAccess(context)
        return CustomWebViewRequestEntity(
            sessionId = sessionId,
            url = request.url.toString(),
            method = convertMethodStringToEnum(request.method),
            headers = request.requestHeaders.toString(),
            hasFullInternetAccess = hasFullInternet,
            timestamp = System.currentTimeMillis()
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
        when (_uiData.value.webViewType) {
            WebViewType.CustomWebView -> _uiData.update {
                it.copy(webViewType = WebViewType.CustomWebView)
            }

            WebViewType.NormalWebView -> _uiData.update {
                it.copy(webViewType = WebViewType.NormalWebView)
            }

            WebViewType.TestingWebView -> _uiData.update {
                it.copy(webViewType = WebViewType.TestingWebView)
            }
        }
        showToast(context.getString(R.string.switched_detection_method), ToastStyle.SUCCESS)
    }

    /**
     * Saves the content of the given [WebView] HTML, JS as files and store those file path in the local database.
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
        showToast: (String, ToastStyle) -> Unit
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
                            val sanitizedUrl = url.replace(Regex("[^a-zA-Z0-9]"), "_")
                            val htmlFilePath = saveContentToFile(
                                context,
                                "webpage_${sanitizedUrl}.html",
                                html.unescapeJsonString()
                            )
                            val jsFilePath = saveContentToFile(
                                context,
                                "webpage_${sanitizedUrl}.js",
                                javascript.unescapeJsonString()
                            )
                            if (htmlFilePath != null && jsFilePath != null) {
                                val webpageContent = WebpageContentEntity(
                                    url = url,
                                    htmlContentPath = htmlFilePath, // Store file path of the file containing the HTML content
                                    jsContentPath = jsFilePath,     // Store file path of the file containing the JavaScript content
                                    sessionId = currentSessionId,
                                    timestamp = System.currentTimeMillis(),
                                )
                                repository.insertWebpageContent(webpageContent)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Saves the given content to a file in internal storage with the given filename.
     * @param context the context used to access the internal storage
     * @param filename the name of the file to be saved
     * @param content the content to be written to the file
     * @return the path of the saved file if successful, null if not
     */
    private fun saveContentToFile(context: Context, filename: String, content: String): String? {
        return try {
            // Create a new file in internal storage
            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray())
            }
            file.absolutePath // Return the file path
        } catch (e: IOException) {
            e.printStackTrace()
            null
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
    fun stopAnalysis() {
        _uiState.value = AnalysisUiState.Loading(R.string.processing_request)
        viewModelScope.launch(Dispatchers.IO) {
            if (hasFullInternetAccess(context)) {
                withContext(Dispatchers.Main) {
                    _uiState.value = AnalysisUiState.AnalysisComplete
                    _uiData.update { it.copy(analysisStatus = AnalysisStatus.COMPLETED) }
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.value = AnalysisUiState.CaptiveUrlDetected
                    _uiData.update { it.copy(analysisStatus = AnalysisStatus.NOT_COMPLETED) }
                }
            }

        }

    }

    //end analysis even if not completed
    fun forceStopAnalysis() {
        _uiState.value = AnalysisUiState.Loading(R.string.processing_request)
        viewModelScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    _uiState.value = AnalysisUiState.AnalysisComplete
                    _uiData.update { it.copy(analysisStatus = AnalysisStatus.COMPLETED) }
                }
        }
    }

    fun isPcapDroidAppInstalled(): Boolean {
        val packageName = "com.emanuelef.remote_capture" // The correct package name
        val packageManager: PackageManager = context.packageManager
        return try {
            // Attempt to get package info. If this succeeds, the app is installed.
            // The '0' flag means get basic info without any special flags.
            // On Android 11+ (API 30+), you need the <queries> element in AndroidManifest.xml (see note below)
            packageManager.getPackageInfo(packageName, 0)
            Log.d("AppCheck", "$packageName is installed.")
            true // Package info found, app is installed
        } catch (e: PackageManager.NameNotFoundException) {
            // The package was not found.
            Log.d("AppCheck", "$packageName is not installed.")
            false // App is not installed
        } catch (e: Exception) {
            // Catch other potential exceptions during package manager interaction
            Log.e("AppCheck", "Error checking if $packageName is installed", e)
            false // Treat other errors as "not installed" or unable to verify
        }
    }

    // Add the .pcap file path to the session object of the network
fun storePcapFilePathInTheSession() {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            sessionManager.getCurrentSessionId()?.let { sessionId ->
                if (mainViewModel.copiedPcapFileUri.value != null) {
                    val session = repository.getSessionBySessionId(sessionId)
                    if (session != null) {
                        repository.updateSession(session.copy(pcapFilePath =
                            mainViewModel.copiedPcapFileUri.value.toString()))
                    }
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    showToast(
                        context.getString(R.string.error_saving_pcap_to_session),
                        ToastStyle.ERROR
                    )
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showToast(
                    context.getString(R.string.error_saving_pcap_to_session),
                    ToastStyle.ERROR
                )
            }
            Log.e(TAG, "Error saving pcap to session: ${e.message}")
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
    //todo fix sometimes screenshot is taken before page is fully loaded
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
                        File(
                            webView.context.getExternalFilesDir(null),
                            "$currentSessionId/screenshots"
                        )
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
                        if (currentSessionId != null) {

                            // Insert into the database
                            val screenshotEntity = ScreenshotEntity(
                                sessionId = currentSessionId,
                                timestamp = System.currentTimeMillis(),
                                path = file.absolutePath,
                                size = "${file.length()} bytes",
                                url = url
                            )
                            repository.insertScreenshot(screenshotEntity) // Already on IO thread
                        } else {
                            showToast(
                                "Error saving screenshot: Session Id is null",
                                ToastStyle.ERROR,
                            )
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
        _uiData.update {
            it.copy().copy(showedHint = showedHint)
        }
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
    private val showToast: (
        message: String,
        style: ToastStyle
    ) -> Unit,
    private val mainViewModel: MainViewModel

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
                mainViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


