package com.example.captive_portal_analyzer_kotlin.screens.webpage_content

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File


/**
 * UI state for the WebpageContentScreen
 */
sealed class WebpageContentUiState {
    object Loading : WebpageContentUiState()
    data class Success(val parsedHtmlContent: String) : WebpageContentUiState()
    data class Error(val message: String) : WebpageContentUiState()
}


class WebpageContentViewModel(
    private val application: Application,
    private val clickedWebpageContent: WebpageContentEntity?
) : ViewModel() {

    /**
     * Gets the application context.
     */
    private val context: Context get() = application.applicationContext

    /**
     * The mutable state flow to hold the UI state of the WebpageContent screen.
     *
     * The UI state can be an instance of Loading, Loaded or Error.
     */
    private val _uiState =
        MutableStateFlow<WebpageContentUiState>(WebpageContentUiState.Loading)
    val uiState: StateFlow<WebpageContentUiState> = _uiState.asStateFlow()


    // load the HTML content of the webpage that user clicked and parse it to show in the webView
    init {
        if (clickedWebpageContent == null) {
            _uiState.value =
                WebpageContentUiState.Error(context.getString(R.string.failed_to_load_html_content))
        } else {
            val html = loadWebpageHtml(clickedWebpageContent.htmlContentPath)
            if (html != null) {
                clickedWebpageContentParser(html)
            } else {
                _uiState.value =
                    WebpageContentUiState.Error(context.getString(R.string.failed_to_load_html_content))
            }
        }
    }

    /**
     * Loads the HTML content of a webpage from a file.
     *
     * @param htmlContentPath the path of the file containing the HTML content
     * @return the HTML content of the webpage as a string
     */
    private fun loadWebpageHtml(htmlContentPath: String): String? {
        return try {
            File(htmlContentPath).readText()
        } catch (e: Exception) {
            _uiState.value = WebpageContentUiState.Error(
                e.message ?: context.getString(R.string.failed_to_load_html_content)
            )
            null
        }
    }

    private fun clickedWebpageContentParser(html: String) {
        // Convert escaped characters and load the HTML
        val decodedHtml = html
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

        // Set the parsed HTML so the listener in the UI gets notified
        _uiState.value = WebpageContentUiState.Success(formattedHtml)
    }
}


/**
 * A factory for creating instances of [WebpageContentViewModel].
 *
 * @param application The application context.
 * @param clickedWebpageContent The clicked webpage content.
 * the message to display, the style of the toast, and the duration of the toast as parameters.
 */
class WebpageContentViewModelFactory(
    private val application: Application,
    private val clickedWebpageContent: WebpageContentEntity?,
) : ViewModelProvider.Factory {
    /**
     * Creates an instance of a ViewModel.
     *
     * @param modelClass The class of the ViewModel to create.
     * @return An instance of the ViewModel.
     * @throws IllegalArgumentException If the modelClass is not a subclass of [WebpageContentViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WebpageContentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WebpageContentViewModel(
                application,
                clickedWebpageContent,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


