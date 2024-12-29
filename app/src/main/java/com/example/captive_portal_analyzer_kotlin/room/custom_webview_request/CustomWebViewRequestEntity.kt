package com.example.captive_portal_analyzer_kotlin.room.custom_webview_request

import android.webkit.WebResourceRequest
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.acsbendi.requestinspectorwebview.WebViewRequest
import java.net.URI

@Entity(tableName = "custom_webview_request")
data class CustomWebViewRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val customWebViewRequestId: Int = 0,
    val sessionId:String?=null,
    val type: String?=null,
    val url: String?=null,
    val method: String?=null,
    val body: String?=null,
    val domain: String?=null,
    val headers: String?=null,
) {
    @Ignore
    var formParameters: Map<String, String> = emptyMap()

    @Ignore
    var enctype: String? = null

    @Ignore
    var isForMainFrame: Boolean? = null

    @Ignore
    var isRedirect: Boolean? = null

    @Ignore
    var hasGesture: Boolean? = null

    @Ignore
    var trace: String? = null


}


fun WebViewRequest.toCustomWebViewRequest(sessionId: String?): CustomWebViewRequestEntity {
    return CustomWebViewRequestEntity(
        type = this.type.name,
        url = this.url,
        method = this.method,
        body = this.body,
        domain = extractDomain(this.url),
        headers =this.headers.toString(),
        sessionId = sessionId
    )
}


fun WebResourceRequest.toCustomWebViewRequest(sessionId: String?): CustomWebViewRequestEntity {
    return CustomWebViewRequestEntity(
        url = this.url.toString(),
        method = this.method,
        domain = extractDomain(this.url.toString()),
        sessionId = sessionId,
        headers = this.requestHeaders.toString()
    )
}





fun extractDomain(url: String): String {
    val uri = URI(url)
    val host = uri.host ?: ""
    return host
}