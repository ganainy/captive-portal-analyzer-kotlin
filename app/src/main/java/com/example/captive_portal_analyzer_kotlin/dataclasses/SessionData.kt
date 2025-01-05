package com.example.captive_portal_analyzer_kotlin.dataclasses

// Data class to hold complete session data
data class SessionData(
    val session: NetworkSessionEntity,
    val requests: List<CustomWebViewRequestEntity>,
    val screenshots: List<ScreenshotEntity>,
    val webpageContent: List<WebpageContentEntity>
)


fun SessionData.toSessionDataDTO(): SessionDataDTO {
    val isCaptiveLocal = session.isCaptiveLocal
    val ipAddress = session.ipAddress
    val gatewayAddress = session.gatewayAddress
    val privacyOrTosRelatedScreenshots = screenshots.filter { it.isPrivacyOrTosRelated } //get only privacy or tos related screenshots

    val requestsDTO =
        requests.map { "request url:" + it.url + ",request type:" + it.type + ",request method: " + it.method + ",request body: " + it.body + ",request headers: " + it.headers }
    return SessionDataDTO(
        prompt = "is captive portal hosted only locally: $isCaptiveLocal ," +
                " my ip address on captive portal:" +
                " $ipAddress,captive portal gateway address: $gatewayAddress, requests made while authenticating in the captive portal: $requestsDTO",
        privacyOrTosRelatedScreenshots = privacyOrTosRelatedScreenshots
    )
}

