package com.example.captive_portal_analyzer_kotlin.room

import android.webkit.WebResourceRequest
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.acsbendi.requestinspectorwebview.WebViewRequestType
import java.net.URI

@Entity(tableName = "custom_webview_request")
data class CustomWebViewRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,
    val url: String,
    val method: String,
    val body: String,
    val domain: String,
    val bssid: String,
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

    @Ignore
    var trace: String = ""


}


fun WebViewRequest.toCustomWebViewRequest(bssid: String): CustomWebViewRequest {
    return CustomWebViewRequest(
        type = this.type.name,
        url = this.url,
        method = this.method,
        body = this.body,
        domain = extractDomain(this.url),
        bssid = bssid
    ).apply {
        formParameters = this@toCustomWebViewRequest.formParameters
        headers = this@toCustomWebViewRequest.headers
        enctype = this@toCustomWebViewRequest.enctype
        isForMainFrame = this@toCustomWebViewRequest.isForMainFrame
        isRedirect = this@toCustomWebViewRequest.isRedirect
        hasGesture = this@toCustomWebViewRequest.hasGesture
        trace = this@toCustomWebViewRequest.trace
    }
}


fun WebResourceRequest.toCustomWebViewRequest(bssid: String): CustomWebViewRequest {
    return CustomWebViewRequest(
        url = this.url.toString(),
        method = this.method,
        domain = extractDomain(this.url.toString()),
        bssid = bssid,
        type = "",
        body = "",
    ).apply {
        isForMainFrame = this@toCustomWebViewRequest.isForMainFrame
        isRedirect = this@toCustomWebViewRequest.isRedirect
        headers = this@toCustomWebViewRequest.requestHeaders
    }
}





fun extractDomain(url: String): String {
    val uri = URI(url)
    val host = uri.host ?: ""
    return host
}