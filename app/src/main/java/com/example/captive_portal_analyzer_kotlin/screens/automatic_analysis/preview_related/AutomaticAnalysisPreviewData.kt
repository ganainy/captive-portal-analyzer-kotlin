package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related

import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.AutomaticAnalysisUiState

import com.example.captive_portal_analyzer_kotlin.dataclasses.*

// --- Mock Data ---

val mockSession = NetworkSessionEntity(
    networkId = "previewNetworkId",
    ssid = "PreviewWiFi",
    bssid = "AA:BB:CC:DD:EE:FF",
    timestamp = System.currentTimeMillis(),
    captivePortalUrl = "http://preview.portal.com",
    ipAddress = "192.168.1.100",
    gatewayAddress = "192.168.1.1",
    isCaptiveLocal = false,
    isUploadedToRemoteServer = false,
    pcapFilePath = null,
    pcapFileUrl = null
)

val mockRequest1 = CustomWebViewRequestEntity(
    customWebViewRequestId = 1,
    sessionId = "preview",
    type = "GET",
    url = "http://preview.portal.com/style.css",
    method = RequestMethod.GET,
    body = null,
    headers = "Accept: text/css"
)
val mockRequest2 = CustomWebViewRequestEntity(
    customWebViewRequestId = 2,
    sessionId = "preview",
    type = "POST",
    url = "http://preview.portal.com/login",
    method = RequestMethod.POST,
    body = "user=preview&pass=test",
    headers = "Content-Type: application/x-www-form-urlencoded"
)
val mockRequest3 = CustomWebViewRequestEntity(
    customWebViewRequestId = 3,
    sessionId = "preview",
    type = "GET",
    url = "http://very.long.url.that.needs.to.be.truncated.for.display.purposes/image.jpg",
    method = RequestMethod.GET,
    body = null,
    headers = "Accept: image/jpeg"
)

val mockScreenshot1 = ScreenshotEntity(
    screenshotId = 1,
    sessionId = "preview",
    path = "/path/to/privacy_policy.png",
    url = "http://preview.portal.com/privacy",
    size = "150KB",
    isPrivacyOrTosRelated = true,
    timestamp = System.currentTimeMillis() - 10000
)
val mockScreenshot2 = ScreenshotEntity(
    screenshotId = 2,
    sessionId = "preview",
    path = "/path/to/login_page.png",
    url = "http://preview.portal.com/login",
    size = "120KB",
    isPrivacyOrTosRelated = false, // Not relevant for selection list
    timestamp = System.currentTimeMillis() - 20000
)
val mockScreenshot3 = ScreenshotEntity(
    screenshotId = 3,
    sessionId = "preview",
    path = "/path/to/terms_of_service.png",
    url = "http://preview.portal.com/tos",
    size = "180KB",
    isPrivacyOrTosRelated = true,
    timestamp = System.currentTimeMillis() - 5000
)


val mockWebpageContent1 = WebpageContentEntity(
    contentId = 1,
    sessionId = "preview",
    htmlContentPath = "/path/to/page.html",
    jsContentPath = "/path/to/script.js",
    url = "http://preview.portal.com/login",
    timestamp = System.currentTimeMillis() - 15000
)
val mockWebpageContent2 = WebpageContentEntity(
    contentId = 2,
    sessionId = "preview",
    htmlContentPath = "/path/to/script.js",
    jsContentPath = "/path/to/script.js",
    url = "http://preview.portal.com/script.js",
    timestamp = System.currentTimeMillis() - 14000
)


val mockSessionDataFull = SessionData(
    session = mockSession,
    requests = listOf(mockRequest1, mockRequest2, mockRequest3),
    screenshots = listOf(mockScreenshot1, mockScreenshot2, mockScreenshot3),
    webpageContent = listOf(mockWebpageContent1, mockWebpageContent2)
)

val mockSessionDataEmpty = SessionData(
    session = mockSession,
    requests = emptyList(),
    screenshots = emptyList(),
    webpageContent = emptyList()
)

// --- Mock UI States ---

val baseUiState = AutomaticAnalysisUiState(
    sessionData = mockSessionDataFull,
    totalRequestsCount = mockSessionDataFull.requests?.size ?: 0,
    totalScreenshotsCount = mockSessionDataFull.screenshots?.count { it.isPrivacyOrTosRelated } ?: 0,
    totalWebpageContentCount = mockSessionDataFull.webpageContent?.size ?: 0,
    // Default selections for loaded state preview
    selectedRequestIds = setOf(mockRequest1.customWebViewRequestId, mockRequest2.customWebViewRequestId),
    selectedScreenshotIds = setOf(mockScreenshot1.screenshotId),
    selectedWebpageContentIds = setOf(mockWebpageContent1.contentId),
    inputText = "Preview prompt: Analyze privacy implications."
)

val mockUiStateInputLoading = baseUiState.copy(
    isLoading = true,
    sessionData = null, // No data yet
    totalRequestsCount = 0,
    totalScreenshotsCount = 0,
    totalWebpageContentCount = 0,
    selectedRequestIds = emptySet(),
    selectedScreenshotIds = emptySet(),
    selectedWebpageContentIds = emptySet()
)

val mockUiStateInputError = baseUiState.copy(
    isLoading = false,
    sessionData = null, // No data
    error = "Failed to load session data from database.",
    totalRequestsCount = 0,
    totalScreenshotsCount = 0,
    totalWebpageContentCount = 0,
    selectedRequestIds = emptySet(),
    selectedScreenshotIds = emptySet(),
    selectedWebpageContentIds = emptySet()
)

val mockUiStateInputLoaded = baseUiState // Use base state as loaded

val mockUiStateInputLoadedEmpty = baseUiState.copy(
    sessionData = mockSessionDataEmpty, // Empty data
    totalRequestsCount = 0,
    totalScreenshotsCount = 0,
    totalWebpageContentCount = 0,
    selectedRequestIds = emptySet(),
    selectedScreenshotIds = emptySet(),
    selectedWebpageContentIds = emptySet()
)


val mockUiStateOutputLoading = baseUiState.copy(
    isLoading = true, // Analysis loading
    outputText = null,
    error = null
)

val mockUiStateOutputStreaming = baseUiState.copy(
    isLoading = true, // Still loading
    outputText = "## Partial Results\n*   Finding 1...\n*   Finding 2...\n\nGenerating more insights...",
    error = null
)


val mockUiStateOutputSuccess = baseUiState.copy(
    isLoading = false,
    outputText = """
    ## AI Analysis Report

    Based on the provided data (2 requests, 1 screenshot, 1 webpage content):

    **Privacy Concerns:**
    *   The POST request to `/login` sends credentials (`user`, `pass`) potentially over HTTP if not secured.
    *   The included privacy policy screenshot (privacy_policy.png) should be reviewed for data collection details.

    **Terms of Service:**
    *   No specific ToS screenshot was selected for analysis.

    **Recommendations:**
    1.  Verify if the login request uses HTTPS.
    2.  Thoroughly read the privacy policy mentioned in the screenshot.
    3.  Consider capturing and selecting a ToS screenshot if available.

    **Rating:** Moderate Concern (due to potential plaintext login).
    """.trimIndent(),
    error = null
)

val mockUiStateOutputError = baseUiState.copy(
    isLoading = false,
    outputText = null,
    error = "AI analysis failed: API key invalid or quota exceeded."
)
