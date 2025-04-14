package com.example.captive_portal_analyzer_kotlin.dataclasses
// Maximum length of the prompt
const val GEMINI_CHAR_LIMIT_MAX = 1000000
// Maximum length of the prompt requests (leaving 3000 characters for the actual prompt)
const val REQUESTS_LIMIT_MAX = GEMINI_CHAR_LIMIT_MAX - 3000

// Data class to hold complete session data
data class SessionData(
    val session: NetworkSessionEntity,
    val requests: List<CustomWebViewRequestEntity>? = null,
    val screenshots: List<ScreenshotEntity>? = null,
    val webpageContent: List<WebpageContentEntity>? = null,
    val requestsCount: Int = 0,
    val screenshotsCount: Int = 0,
    val webpageContentCount: Int = 0
)
