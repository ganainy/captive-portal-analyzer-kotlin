package com.example.captive_portal_analyzer_kotlin.dataclasses

// Data class to hold complete session data
data class SessionData(
    val session: NetworkSessionEntity,
    val requests: List<CustomWebViewRequestEntity>,
    val screenshots: List<ScreenshotEntity>,
    val webpageContent: List<WebpageContentEntity>
)

/**
 * Converts SessionData to SessionDataDTO to prepare it for upload to AI agent server.
 * Filters screenshots for privacy or ToS relevance to include with the request to AI agent.
 * Includes a additional prompt to include with the request to AI agent,
 * to help the AI agent understand how to format the response.
 * @return SessionDataDTO containing prompt and relevant screenshots.
 */
fun SessionData.toSessionDataDTO(): SessionDataDTO {
    val isCaptiveLocal = session.isCaptiveLocal
    val ipAddress = session.ipAddress
    val gatewayAddress = session.gatewayAddress
    val privacyOrTosRelatedScreenshots = screenshots.filter { it.isPrivacyOrTosRelated } // get only privacy or tos related screenshots

    val requestsDTO =
        requests.map { "request url:" + it.url + ",request type:" + it.type + ",request method: " + it.method + ",request body: " + it.body + ",request headers: " + it.headers }
    return SessionDataDTO(
        prompt = "is captive portal hosted only locally: $isCaptiveLocal ," +
                " my ip address on captive portal:" +
                " $ipAddress,captive portal gateway address: $gatewayAddress, requests made while authenticating in the captive portal: $requestsDTO",
        privacyOrTosRelatedScreenshots = privacyOrTosRelatedScreenshots
    )
}