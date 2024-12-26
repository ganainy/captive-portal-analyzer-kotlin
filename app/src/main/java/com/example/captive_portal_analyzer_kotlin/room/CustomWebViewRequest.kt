package com.example.captive_portal_analyzer_kotlin.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.acsbendi.requestinspectorwebview.WebViewRequestType
@Entity(tableName = "custom_webview_request")
data class CustomWebViewRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: WebViewRequestType,
    val url: String,
    val method: String,
    val body: String,
    val trace: String
) {
    @Ignore
    var formParameters: Map<String, String> = emptyMap()

    @Ignore
    var headers: Map<String, String> = emptyMap()

    @Ignore
    var enctype: String? = null

    @Ignore
    var isForMainFrame: Boolean = false

    @Ignore
    var isRedirect: Boolean = false

    @Ignore
    var hasGesture: Boolean = false

}


fun CustomWebViewRequest.toWebViewRequest(): WebViewRequest {
    return WebViewRequest(
        type = this.type,
        url = this.url,
        method = this.method,
        body = this.body,
        trace = this.trace,
        formParameters = this.formParameters,
        headers = this.headers,
        enctype = this.enctype,
        isForMainFrame = this.isForMainFrame,
        isRedirect = this.isRedirect,
        hasGesture = this.hasGesture
    )
}

fun WebViewRequest.toCustomWebViewRequest(): CustomWebViewRequest {
    return CustomWebViewRequest(
        type = this.type,
        url = this.url,
        method = this.method,
        body = this.body,
        trace = this.trace
    ).apply {
        formParameters = this@toCustomWebViewRequest.formParameters
        headers = this@toCustomWebViewRequest.headers
        enctype = this@toCustomWebViewRequest.enctype
        isForMainFrame = this@toCustomWebViewRequest.isForMainFrame
        isRedirect = this@toCustomWebViewRequest.isRedirect
        hasGesture = this@toCustomWebViewRequest.hasGesture
    }
}
