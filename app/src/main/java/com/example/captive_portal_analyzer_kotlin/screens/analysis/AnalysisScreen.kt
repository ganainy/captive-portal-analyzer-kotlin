package com.example.captive_portal_analyzer_kotlin.screens.analysis

import NetworkSessionRepository
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acsbendi.requestinspectorwebview.RequestInspectorWebViewClient
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.NeverSeeAgainAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.AlertDialogState
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator


import com.example.captive_portal_analyzer_kotlin.SharedViewModel
import com.example.captive_portal_analyzer_kotlin.components.GhostButton
import com.example.captive_portal_analyzer_kotlin.components.HintText
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
/**
 * A composable function representing the analysis screen in the app.
 *
 * @param repository The repository for managing network session data.
 * @param navigateToSessionList A lambda function to navigate to the session list screen.
 * @param navigateToManualConnect A lambda function to navigate to the manual connect screen.
 * @param sessionManager The session manager for handling network sessions.
 * @param sharedViewModel The shared view model for managing shared state between screens.
 */
@Composable
fun AnalysisScreen(
    repository: NetworkSessionRepository,
    navigateToSessionList: () -> Unit,
    navigateToManualConnect: () -> Unit,
    sessionManager: NetworkSessionManager,
    sharedViewModel: SharedViewModel,
) {
    // Initialize the AnalysisViewModel with the necessary dependencies
    val analysisViewModel: AnalysisViewModel = viewModel(
        factory = AnalysisViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            repository = repository,
            sessionManager = sessionManager,
            showToast = sharedViewModel::showToast
        )
    )

    // Collect the UI state from the analysisViewModel
    val uiState by analysisViewModel.uiState.collectAsState()

    // Retrieve the current context
    val context = LocalContext.current

    // Remember a coroutine scope for launching coroutines
    val coroutineScope = rememberCoroutineScope()

    // Collect the portal URL and WebView type state from the analysisViewModel
    val portalUrl by analysisViewModel.portalUrl.collectAsState()
    val webViewType by analysisViewModel.webViewType.collectAsState()
    val showedHint by analysisViewModel.showedHint.collectAsState()

    // a lambda function to show toast messages using the sharedViewModel
    val showToast = { message: String, style: ToastStyle ->
        sharedViewModel.showToast(
            message = message, style = style,
        )
    }
    // Request the captive portal address on screen start, providing the showToast function to show toast if needed
    analysisViewModel.getCaptivePortalAddress(showToast)

    // Remember a WebView instance with the current context to keep webView state across recompositions
    val webView = remember {
        WebView(context)
    }

    // Clean up the WebView when it is no longer needed
    DisposableEffect(webView) {
        onDispose {
            webView.destroy()
        }
    }

    Scaffold(

    ) { contentPadding ->


        when (uiState) {

            // while loading the captive portal URL, show a loading indicator
            is AnalysisUiState.Loading -> {
                LoadingIndicator(
                    message = stringResource((uiState as AnalysisUiState.Loading).messageStringResource),
                    modifier = Modifier.padding(contentPadding)
                )
            }

            // if the captive portal URL is not null, show the captive portal website content
            is AnalysisUiState.CaptiveUrlDetected -> {

                CaptivePortalWebsiteContent(
                    webViewType,
                    portalUrl,
                    webView,
                    coroutineScope,
                    analysisViewModel,
                    contentPadding,
                    showToast,
                    showedHint,
                    context,
                    sharedViewModel,
                    navigateToSessionList
                )
            }

            // if something went wrong, show an error message and a button to retry loading captive portal URL
            is AnalysisUiState.Error -> {
                AnalysisError(
                    contentPadding,
                    uiState,
                    analysisViewModel::getCaptivePortalAddress,
                    showToast,
                    navigateToManualConnect
                )
            }

            // if the analysis is complete, navigate to the session list
            AnalysisUiState.AnalysisComplete -> navigateToSessionList()
        }
    }
}
/**
 * A composable function to display the captive portal website content.
 *
 * @param webViewType The type of the WebView being used (Normal: no request body interception or Custom with request body interception).
 * @param portalUrl The URL of the portal to display in the WebView.
 * @param webView The WebView instance to display the content.
 * @param coroutineScope The CoroutineScope for launching asynchronous tasks.
 * @param analysisViewModel The ViewModel for handling analysis logic.
 * @param contentPadding The padding values for the content.
 * @param showToast A lambda function to show toast messages.
 * @param showedHint Boolean indicating if the hint has been shown.
 * @param context The context of the current state of the application.
 * @param sharedViewModel The shared ViewModel for managing shared state.
 * @param navigateToSessionList A lambda function to navigate to the session list screen once the analysis is complete.
 */
@Composable
private fun CaptivePortalWebsiteContent(
    webViewType: WebViewType,
    portalUrl: String?,
    webView: WebView,
    coroutineScope: CoroutineScope,
    analysisViewModel: AnalysisViewModel,
    contentPadding: PaddingValues,
    showToast: (String, ToastStyle) -> Unit,
    showedHint: Boolean,
    context: Context,
    sharedViewModel: SharedViewModel,
    navigateToSessionList: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // WebView container with weight to take all available space
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(1.dp)
        ) {

            when (webViewType) {
                // this is the backup webView type, it will not intercept the request body,
                // it is only used as a fallback if custom webView fails
                WebViewType.NormalWebView -> {

                    NormalWebView(
                        portalUrl = portalUrl,
                        webView = webView,
                        saveWebRequest = { request ->
                            coroutineScope.launch {
                                analysisViewModel.saveWebResourceRequest(request)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        captureAndSaveContent = { webView, content ->
                            coroutineScope.launch {
                                analysisViewModel.saveWebpageContent(
                                    webView,
                                    content,
                                    showToast
                                )
                            }
                        },
                        takeScreenshot = analysisViewModel::takeScreenshot
                    )


                }
                //this is the default webView type, it will intercept the request body, but if there is
                // an issue loading the website, it will fall back to the normal webView
                WebViewType.CustomWebView -> {
                    CustomWebView(
                        portalUrl = portalUrl,
                        webView = webView,
                        saveWebRequest = { request ->
                            coroutineScope.launch {
                                analysisViewModel.saveWebViewRequest(request)
                            }
                        },
                        takeScreenshot = analysisViewModel::takeScreenshot,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        captureAndSaveContent = { webView, content ->
                            coroutineScope.launch {
                                analysisViewModel.saveWebpageContent(
                                    webView,
                                    content,
                                    showToast
                                )
                            }
                        },
                        showedHint = showedHint,
                        updateShowedHint = analysisViewModel::updateShowedHint
                    )

                }
            }
        }


        // this column is used to display the end analysis button and switch webView type button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RoundCornerButton(
                    modifier = Modifier
                        .weight(2f)
                        .padding(start = 8.dp),
                    onClick = {
                        analysisViewModel.stopAnalysis(onUncompletedAnalysis = {
                            showUncompletedAnalysisDialog(
                                context = context,
                                hideDialog = sharedViewModel::hideDialog,
                                showDialog = sharedViewModel::showDialog,
                                navigateToSessionList = navigateToSessionList
                            )
                        })
                    },
                    buttonText = stringResource(id = R.string.end_analysis)
                )

                GhostButton(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    onClick = {
                        analysisViewModel.switchWebViewType(
                            showToast = showToast
                        )
                    },
                    text = stringResource(id = R.string.switch_browser_type)
                )
            }
        }
    }
}


/**
 * This function is used to display a warning dialog when the user wants to stop the analysis
 * before completing the login process of the captive portal.
 *
 * The dialog asks the user if they want to stop the analysis anyway.
 *
 * If the user confirms, the analysis is stopped and the user is navigated to the session list.
 *
 * If the user dismisses the dialog, the dialog is hidden.
 */
private fun showUncompletedAnalysisDialog(context: Context,hideDialog: () -> Unit,
                                          showDialog: (title: String, message: String, confirmText: String, dismissText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) -> Unit, navigateToSessionList: () -> Unit) {
    showDialog(
        context.getString(R.string.warning),
        context.getString(R.string.it_looks_like_you_still_have_no_full_internet_connection_please_complete_the_login_process_of_the_captive_portal_before_stopping_the_analysis),
        context.getString(R.string.stop_analysis_anyway),
         context.getString(R.string.dismiss),
         navigateToSessionList,
        hideDialog,
    )
}

/**
* Preview function for AnalysisError composable
* This function is used to generate a preview of the AnalysisError composable
* in the Android Studio preview panel.
*/
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(showBackground = true)
private fun AnalysisErrorPreview() {
    AppTheme {
        AnalysisError(
            contentPadding = PaddingValues(),
            uiState = AnalysisUiState.Error(AnalysisUiState.ErrorType.CannotDetectCaptiveUrl),
            getCaptivePortalAddress = {},
            showToast = { _, _ -> },
            navigateToManualConnect = {}
        )
    }
}

/**
 * A composable function that displays an error message and two buttons.
 *
 * The UI state is passed in as a parameter, which can be one of the following:
 * - [AnalysisUiState.ErrorType.CannotDetectCaptiveUrl]: The app failed to detect the captive portal URL.
 * - [AnalysisUiState.ErrorType.Unknown]: An unknown error occurred.
 *
 * The [getCaptivePortalAddress] function is called when the "Retry" button is clicked.
 * The [showToast] function is called to display a toast message if the retry button is clicked.
 * The [navigateToManualConnect] function is called when the "Connect to another network" button is clicked.
 *
 * The content padding is used to add padding to the outside of the Box.
 */
@Composable
private fun AnalysisError(
    contentPadding: PaddingValues,
    uiState: AnalysisUiState,
    getCaptivePortalAddress: (showToast: (String, ToastStyle) -> Unit) -> Unit,
    showToast: (String, ToastStyle) -> Unit,
    navigateToManualConnect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val error = uiState as AnalysisUiState.Error
            var text = ""
            var hint = ""
            when (error.type) {
                AnalysisUiState.ErrorType.CannotDetectCaptiveUrl -> {
                    text = stringResource(R.string.couldnt_detect_captive_url)
                    hint = stringResource(R.string.are_you_sure_current_network_has_captive_portal)
                }

                AnalysisUiState.ErrorType.Unknown -> {
                    text = stringResource(R.string.something_went_wrong)
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            HintText(
                hint = hint,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp), // Optional padding for better UI spacing
                horizontalArrangement = Arrangement.SpaceBetween // Ensures buttons are spaced apart
            ) {
                RoundCornerButton(
                    modifier = Modifier
                        .weight(2f)
                        .padding(start = 8.dp), // Add spacing between buttons
                    onClick = {
                        navigateToManualConnect()
                    },
                    buttonText = stringResource(id = R.string.connect_to_another_network)
                )

                GhostButton(
                    modifier = Modifier
                        .weight(1f) // Distribute available space equally
                        .padding(end = 8.dp), // Add spacing between buttons
                    onClick = {
                        getCaptivePortalAddress(showToast)
                    },
                    text = stringResource(id = R.string.retry)
                )

            }

        }
    }
}
/**
 * A composable function to render a normal WebView, this is used as a backup in case the custom WebView fails
 * since it doesn't have the ability to access the request body.
 *
 * @param portalUrl The URL of the portal to test.
 * @param webView The WebView to use to show the website.
 * @param saveWebRequest A function to save the custom WebView request.
 * @param captureAndSaveContent A function to capture and save the webpage content (HTML + JS).
 * @param takeScreenshot A function to capture a screenshot of the webpage.
 * @param modifier The modifier to be applied to the [Box] that contains the [WebView].
 */
@Composable
private fun NormalWebView(
    portalUrl: String?,
    webView: WebView,
    saveWebRequest: (WebResourceRequest?) -> Unit,
    captureAndSaveContent: (WebView, String) -> Unit,
    takeScreenshot: (WebView, String) -> Unit,
    modifier: Modifier
) {

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                webView.apply {
                    webViewClient = object : WebViewClient() {
                        // Intercept requests to save them
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            saveWebRequest(request)
                            return super.shouldInterceptRequest(view, request)
                        }

                        // Called when a page finishes loading
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            takeScreenshot(
                                view,
                                url,
                            ) // Capture screenshot when a new page finishes loading
                            captureAndSaveContent(
                                view,
                                url
                            ) //capture and save webpage content (HTML + JS)
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        loadWithOverviewMode = true
                        domStorageEnabled = true
                        loadsImagesAutomatically = true

                    }
                    setWebContentsDebuggingEnabled(true)
                    webView
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (portalUrl != null) {
                    view.loadUrl(portalUrl)
                }
            }
        )

    }
}

/**
 * A composable function to render a custom WebView which intercepts the request body using JS injection.
 *
 * @param portalUrl The URL of the portal to test.
 * @param webView The WebView to use to show the website.
 * @param saveWebRequest A function to save the custom WebView request.
 * @param captureAndSaveContent A function to capture and save the webpage content (HTML + JS).
 * @param takeScreenshot A function to capture a screenshot of the webpage.
 * @param updateShowedHint A callback to update the state of the "Never see again" preference for the hint box.
 * @param showedHint A boolean indicating whether the hint has been shown.
 * @param modifier The modifier to be applied to the Box.
 */
@Composable
private fun CustomWebView(
    portalUrl: String?,
    webView: WebView,
    saveWebRequest: (WebViewRequest) -> Unit,
    captureAndSaveContent: (WebView, String) -> Unit,
    takeScreenshot: (WebView, String) -> Unit,
    updateShowedHint: (Boolean) -> Unit,
    showedHint: Boolean,
    modifier: Modifier,
) {

    Box(
        modifier = modifier
    ) {

        WebViewWithCustomClient(
            portalUrl = portalUrl,
            webView = webView,
            saveWebRequest = saveWebRequest,
            modifier = Modifier.fillMaxSize(),
            captureAndSaveContent = captureAndSaveContent,
            takeScreenshot = takeScreenshot,
        )

        HintInfoBox(
            context = LocalContext.current,
            modifier = Modifier.align(Alignment.Center),
            showedHint = showedHint,
            updateShowedHint =updateShowedHint
        )
    }
}

/**
 * A composable function to display a hint info box.
 *
 * This function checks the "Never see again" state for the info box and displays
 * a `NeverSeeAgainAlertDialog` if the box should be shown and the hint hasn't been shown yet.
 *
 * The hint includes information on how to login to the captive portal and end the analysis process.
 *
 * @param context The Android context to use for retrieving the preferences.
 * @param modifier The modifier to be applied to the AlertDialog.
 * @param updateShowedHint A callback to update the hint state.
 * @param showedHint A boolean indicating whether the hint has been shown.
 */
@Composable
private fun HintInfoBox(
    context: Context,
    modifier: Modifier,
    updateShowedHint: (Boolean) -> Unit = {},
    showedHint: Boolean
) {
    var showInfoBox1 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AlertDialogState.getNeverSeeAgainState(context, "info_box_1")
            .collect { neverSeeAgain ->
                showInfoBox1 = !neverSeeAgain
            }
    }

    if (showInfoBox1 && !showedHint) {
        NeverSeeAgainAlertDialog(
            title = stringResource(R.string.hint),
            message = stringResource(R.string.login_to_captive_then_click_end_analysis),
            preferenceKey = "info_box_1",
            onDismiss = {
                showInfoBox1 = false
                updateShowedHint(true)
            },
            modifier = modifier
        )
    }
}

/**
 * A composable that takes a [WebView] and a [String] portal url.
 * It sets up a [RequestInspectorWebViewClient] that will capture all web requests made by the [WebView]. including any request body.
 * It will also be notified when a page finishes loading and will take a screenshot of the [WebView] and capture the HTML and JS content.
 * It will also save all web requests made by the [WebView] to the local db.
 *
 * @param portalUrl The url of the captive portal.
 * @param webView The [WebView] that will be used to load the captive portal.
 * @param saveWebRequest A function that will be called with each web request made by the [WebView].
 * @param captureAndSaveContent A function that will be called when a page finishes loading. It will be passed the [WebView] and the url of the page.
 * @param takeScreenshot A function that will be called when a page finishes loading. It will be passed the [WebView] and the url of the page.
 * @param modifier The modifier to be applied to the [Box] that contains the [WebView].
 *
 * Note: the [RequestInspectorWebViewClient] can cause the [WebView] to malfunction sometimes, this is why another [CustomWebView] is used in function
 * private fun CustomWebView as a backup to load the captive portal.
 */
@Composable
private fun WebViewWithCustomClient(
    portalUrl: String?,
    webView: WebView,
    saveWebRequest: (WebViewRequest) -> Unit,
    captureAndSaveContent: (WebView, String) -> Unit,
    takeScreenshot: (WebView, String) -> Unit,
    modifier: Modifier
) {

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                webView.apply {
                    webViewClient = object : RequestInspectorWebViewClient(webView) {

                        // Called when a page finishes loading
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            takeScreenshot(
                                view,
                                url,
                            ) // Capture screenshot when a new page finishes loading
                            captureAndSaveContent(
                                view,
                                url
                            ) //capture and save webpage content (HTML + JS)
                        }

                        override fun shouldInterceptRequest(
                            view: WebView,
                            webViewRequest: WebViewRequest
                        ): WebResourceResponse? {
                            Log.i(
                                "RequestInspectorWebView",
                                "Sending request from WebView: $webViewRequest"
                            )
                            saveWebRequest(webViewRequest)
                            return null
                        }

                    }

                    settings.apply {
                        javaScriptEnabled = true
                        loadWithOverviewMode = true
                        domStorageEnabled = true
                        loadsImagesAutomatically = true
                    }

                    setWebContentsDebuggingEnabled(true)
                    webView
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (portalUrl != null) {
                    view.loadUrl(portalUrl)
                }
            }
        )

    }
}