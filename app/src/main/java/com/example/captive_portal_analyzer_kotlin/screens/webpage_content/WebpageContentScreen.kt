package com.example.captive_portal_analyzer_kotlin.screens.webpage_content

import android.app.Application
import android.content.res.Configuration
import android.webkit.WebView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.components.LoadingIndicator
import com.example.captive_portal_analyzer_kotlin.components.MockWebView

/**
 * Composable function for displaying the HTML content of a certain page of the captive portal
 *
 * @param mainViewModel The shared view model containing the WebpageContentEntity which containts
 * the HTML content to be displayed.
 */
@Composable
fun WebpageContentScreen(
    mainViewModel: MainViewModel,
) {


    // Collect the clicked WebpageContentEntity from sharedViewModel
    val clickedWebpageContent by mainViewModel.clickedWebpageContent.collectAsState()

    // Initialize the WebpageContentViewModel with the necessary dependencies
    val webpageContentViewModel: WebpageContentViewModel = viewModel(
        factory = WebpageContentViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            clickedWebpageContent = clickedWebpageContent,
        )
    )


    // Collect the UI state from the webpageContentViewModel
    val uiState by webpageContentViewModel.uiState.collectAsState()


    Scaffold(

    ) { paddingValues ->
        WebpageContent(uiState,modifier = Modifier.fillMaxSize().padding(paddingValues))
    }
}

@Composable
private fun WebpageContent(uiState: WebpageContentUiState, modifier: Modifier=Modifier) {
    Box(
        modifier = modifier
    ) {
        when (uiState) {
            is WebpageContentUiState.Error -> {
                // Display the error
                val errorMessage = (uiState as WebpageContentUiState.Error).message
                ErrorComponent(errorMessage)
            }

            is WebpageContentUiState.Success -> {
                // Display the HTML content in a webView
                val parsedHtml = (uiState as WebpageContentUiState.Success).parsedHtmlContent
                ClickedWebpageContentParser(parsedHtml)
            }

            is WebpageContentUiState.Loading -> {
                // Show loading indicator
                LoadingIndicator(stringResource(R.string.parsing_html))
            }
        }
    }
}

/**
 * Composable function for displaying the Webpage HTML content
 */
@Composable
fun ClickedWebpageContentParser(formattedHtml: String) {

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            HintTextWithIcon(
                hint = stringResource(R.string.hint_parse_html),
            )
            HintTextWithIcon(
                hint = stringResource(R.string.hint_parse_html2),
            )

            if (LocalInspectionMode.current) {
                // Use a mock WebView placeholder for the preview function since WebView cannot be previewed
                MockWebView()
            }else {
                //this is a webview to try to parse the html content of the clicked webpage
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.primary) // Adds colored border
                        .padding(8.dp) // Adds padding around the WebView
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                }
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(
                                null,
                                formattedHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    )
                }
            }
        }
    }


}


@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun WebpageContentScreenPreview_Success() {
    WebpageContent(
        uiState = WebpageContentUiState.Success("html code"),
    )
}

@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun WebpageContentScreenPreview_Error() {
    WebpageContent(
        uiState = WebpageContentUiState.Loading,
    )
}

@Composable
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", name = "phone")
@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    name = "tablet",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
fun WebpageContentScreenPreview_Loading() {
    WebpageContent(
        uiState = WebpageContentUiState.Error("Failed to load HTML content"),
    )
}

