package com.example.captive_portal_analyzer_kotlin.dataclasses

import com.example.captive_portal_analyzer_kotlin.room.custom_webview_request.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.room.network_session.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.room.screenshots.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.room.webpage_content.WebpageContentEntity

data class Report(
    val session: NetworkSessionEntity,
    val requests: List<CustomWebViewRequestEntity>,
    val screenshots: List<ScreenshotEntity>,
    val webpageContent: List<WebpageContentEntity>
)