package com.example.captive_portal_analyzer_kotlin.screens.analysis

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast


class TestingWebViewClient : WebViewClient() {
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest?,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)

        // Handle the error appropriately
        val errorMessage = "Error: " + error.getDescription()
        Toast.makeText(view.getContext(), errorMessage, Toast.LENGTH_LONG).show()

        // Load an error page or display a message
      //  view.loadData("<h1>An error occurred</h1><p>" + errorMessage + "</p>", "text/html", "UTF-8")
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse
    ) {
        super.onReceivedHttpError(view, request, errorResponse)

        // Handle HTTP errors (e.g., 404, 500)
        val statusCode = errorResponse.getStatusCode()
        val errorMessage = "HTTP Error: " + statusCode + " - " + errorResponse.getReasonPhrase()
        Toast.makeText(view.getContext(), errorMessage, Toast.LENGTH_LONG).show()

        // Optionally, load an error page or display a message.
      //  view.loadData("<h1>HTTP Error</h1><p>" + errorMessage + "</p>", "text/html", "UTF-8")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        // Page has finished loading
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return false // Let the WebView load the URL
    }
}
