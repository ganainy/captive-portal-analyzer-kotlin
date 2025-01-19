package com.example.captive_portal_analyzer_kotlin.screens.webpage_content

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.SharedViewModel
import com.example.captive_portal_analyzer_kotlin.components.ErrorComponent
import com.example.captive_portal_analyzer_kotlin.components.HintTextWithIcon
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity

/**
 * Composable function for displaying the HTML content of a certain page of the captive portal
 *
 * @param sharedViewModel The shared view model containing the WebpageContentEntity which containts
 * the HTML content to be displayed.
 */
@Composable
fun WebpageContentScreen(
    sharedViewModel: SharedViewModel,
) {


    // Collect the clicked WebpageContentEntity from sharedViewModel
    val clickedWebpageContent by sharedViewModel.clickedWebpageContent.collectAsState()


    Scaffold(

    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            ClickedWebpageContentParser(clickedWebpageContent)
        }
    }
}

/**
 * Composable function for displaying the WebpageContent HTML content
 */
@Composable
fun ClickedWebpageContentParser(clickedWebpageContent: WebpageContentEntity?) {
    if (clickedWebpageContent != null) {

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
                        // Convert escaped characters and load the HTML
                        val decodedHtml = clickedWebpageContent.htmlContent
                            .replace("\\u003C", "<")
                            .replace("\\u003E", ">")
                            .replace("\\u003Ca", "<a")
                            .replace("\\u003C/a", "</a")
                            .replace("\\u003Cbr", "<br")
                            .replace("\\u003C/br", "</br")
                            .replace("\\u003Ch4", "<h4")
                            .replace("\\u003C/h4", "</h4")
                            .replace("\\u003Cdiv", "<div")
                            .replace("\\u003C/div", "</div")
                            .replace("\\u003Cp", "<p")
                            .replace("\\u003C/p", "</p")
                            .replace("\\u003Cform", "<form")
                            .replace("\\u003C/form", "</form")
                            .replace("\\u003Cinput", "<input")
                            .replace("\\u003Clabel", "<label")
                            .replace("\\u003C/label", "</label")
                            .replace("\\u003C/body", "</body")
                            .replace("\\u003C/html", "</html")

                        // Wrap the content in proper HTML structure
                        val formattedHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body {
                                    font-family: Arial, sans-serif;
                                    padding: 16px;
                                    margin: 0;
                                }
                                .bienvenido {
                                    font-size: 24px;
                                    font-weight: bold;
                                    margin-bottom: 16px;
                                }
                                .texto_inicial {
                                    font-size: 18px;
                                    margin-bottom: 24px;
                                }
                                .separadorSimple {
                                    margin: 12px 0;
                                }
                                input {
                                    width: 100%;
                                    padding: 8px;
                                    margin: 4px 0;
                                    border: 1px solid #ccc;
                                    border-radius: 4px;
                                }
                                .boton {
                                    background-color: #007bff;
                                    color: white;
                                    padding: 10px 20px;
                                    border: none;
                                    border-radius: 4px;
                                    cursor: pointer;
                                }
                            </style>
                        </head>
                        <body>
                            $decodedHtml
                        </body>
                        </html>
                    """.trimIndent()

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

    } else {
        ErrorComponent(error = stringResource(R.string.error_parsing_the_webpage_content))
    }

}

