package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.components.CustomProgressIndicator
import com.example.captive_portal_analyzer_kotlin.components.CustomSnackBar
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.dataclasses.CaptivePortalReport
import com.example.captive_portal_analyzer_kotlin.repository.IDataRepository
import com.example.captive_portal_analyzer_kotlin.utils.SecurityHeaderService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AnalysisScreen(
    dataRepository: IDataRepository,
    navigateToReport: (Any?) -> Unit,
    navigateBack: () -> Unit
) {
    val viewModel: AnalysisViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()


    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()


    Scaffold(
        topBar = {
            ToolbarWithMenu(
                openMenu = {
                    scope.launch {
                        drawerState.open() // Open the drawer when menu icon is clicked
                    }
                }
            )
        },
    ) { paddingValues ->
        val portalUrl = viewModel.portalUrl.value // Observe the URL from ViewModel
        // AndroidView allows you to integrate the WebView with Compose
        var webView by remember { mutableStateOf<WebView?>(null) }

        // Collect the security header result and interceptor
        val (headerCollectionResult, headerInterceptor) = remember {
            runBlocking {
                SecurityHeaderService().collectSecurityHeaders(portalUrl)
                //todo add the call result of collectSecurityHeaders to the report
            }
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                    }

                    // Create JavaScript interface that calls ViewModel methods
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun analyzeFormFields(formDataJson: String) {
                                viewModel.analyzeFormFields(formDataJson)
                            }
                        },
                        "AndroidAnalyzer"
                    )

                    webViewClient = object : WebViewClient() {
                        // Detect when a new URL has completely finished loading
                        override fun onPageFinished(view: WebView, url: String) {
                            // Analyze forms using JavaScript
                            view.loadUrl(
                                "javascript:(function() {" +
                                        "var forms = document.getElementsByTagName('form');" +
                                        "var formData = [];" +
                                        "for(var i = 0; i < forms.length; i++) {" +
                                        "  var inputs = forms[i].getElementsByTagName('input');" +
                                        "  for(var j = 0; j < inputs.length; j++) {" +
                                        "    formData.push({" +
                                        "      'name': inputs[j].name," +
                                        "      'type': inputs[j].type," +
                                        "      'id': inputs[j].id" +
                                        "    });" +
                                        "  }" +
                                        "}" +
                                        "AndroidAnalyzer.analyzeFormFields(JSON.stringify(formData));" +
                                        "})();"
                            )

                            // Notify ViewModel about page finish
                            viewModel.onWebViewPageFinished(url)
                        }


                    }

                    // Store reference to WebView if needed
                    webView = this
                }
            },
            update = { view ->
                // Any updates to the WebView can be handled here
                if(portalUrl != ""){
                    view.loadUrl(portalUrl)
                    println("Updating WebView with URL: ${view.url}")
                }
            }
        )

    }



    when (uiState) {
        is AnalysisUiState.Initial -> {
            // Show initial state (empty form)
        }

        is AnalysisUiState.Loading -> {
            // Show loading indicator
            CustomProgressIndicator()
        }

        is AnalysisUiState.Error -> {
            // Show error message
            CustomSnackBar(message = stringResource((uiState as LandingUiState.Error).messageStringResource)) {
            }
        }

        is AnalysisUiState.Analyzed -> {
            // Show analysis result
            // val analysisResult = (uiState as AnalysisUiState.Analyzed).captivePortalReport
            // navigateToReport(analysisResult)
        }
    }

    }




