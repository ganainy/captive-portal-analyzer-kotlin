package com.example.captive_portal_analyzer_kotlin.utils

import android.webkit.JavascriptInterface

public class JavaScriptInterface {

    private val formFields: MutableList<String> = ArrayList()
    private val javascriptAnalysis = StringBuilder()

    @JavascriptInterface
    fun analyzeFormFields(formDataJson: String?) {
        // Process form fields data
        if (formDataJson != null) {
            formFields.add(formDataJson)
        }
    }

    @JavascriptInterface
    fun reportInsecureForms(insecureFormsJson: String?) {
        javascriptAnalysis.append("Insecure Forms: ").append(insecureFormsJson).append("\n")
    }

    @JavascriptInterface
    fun reportMixedContent(mixedContentJson: String?) {
        javascriptAnalysis.append("Mixed Content: ").append(mixedContentJson).append("\n")
    }
}