package com.example.captive_portal_analyzer_kotlin.dataclasses

// Maximum length of the prompt
const val GEMINI_CHAR_LIMIT_MAX = 4096
// Maximum length of the prompt requests (leaving 300 characters for the actual prompt)
const val REQUESTS_LIMIT_MAX = GEMINI_CHAR_LIMIT_MAX - 300

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

/**
 * Converts SessionData to SessionDataDTO to prepare it for upload to AI agent server.
 * Filters screenshots for privacy or ToS relevance to include with the request to AI agent.
 * Includes an additional prompt to help the AI agent understand how to format the response.
 * @return SessionDataDTO containing prompt and relevant screenshots.
 */
fun SessionData?.toSessionDataDTO(): SessionDataDTO {
    // Check if session data is null and return empty SessionDataDTO
    if (this == null) {
        return SessionDataDTO(
            prompt = "",
            privacyOrTosRelatedScreenshots = null
        )
    }
    // Otherwise, convert session data to SessionDataDTO
    val session = this.session
    val isCaptiveLocal = session.isCaptiveLocal
    val ipAddress = session.ipAddress
    val gatewayAddress = session.gatewayAddress
    val privacyOrTosRelatedScreenshots =
        screenshots?.filter { it.isPrivacyOrTosRelated } // Get only privacy or ToS related screenshots

    // Separate POST and non-POST requests
    val postRequests = requests?.filter { it.type == "POST" } ?: emptyList()
    val nonPostRequests = requests?.filter { it.type != "POST" } ?: emptyList()

    // Convert POST requests to DTO format
    val postRequestsDTO = postRequests.joinToString(separator = "\n") {
        "request url:${it.url},request type:${it.type},request method:${it.method},request body:${it.body},request headers:${it.headers}"
    }

    // Calculate remaining character space for non-POST requests
    val remainingChars = REQUESTS_LIMIT_MAX - postRequestsDTO.length

    // Convert non-POST requests to DTO format, considering character limit
    val nonPostRequestsDTO = nonPostRequests.joinToString(separator = "\n") {
        "request url:${it.url},request type:${it.type},request method:${it.method},request body:${it.body},request headers:${it.headers}"
    }.take(remainingChars)

    // Combine POST and non-POST requests into final DTO
    val requestsDTO = postRequestsDTO + if (remainingChars > 0) "\n$nonPostRequestsDTO" else ""


    // Construct the default prompt
    val defaultPrompt = """
    The following text, images include information about the user's experience with a captive portal.
    - Screenshots and request information captured during the login process.
    - Requests made during authentication in the captive portal:
      $requestsDTO"""

    return SessionDataDTO(
        defaultPrompt,
        privacyOrTosRelatedScreenshots = privacyOrTosRelatedScreenshots
    )
}
