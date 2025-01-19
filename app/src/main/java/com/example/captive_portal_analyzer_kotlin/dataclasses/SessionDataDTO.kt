package com.example.captive_portal_analyzer_kotlin.dataclasses


/**
 * Data class containing only the fields of the SessionData that will be passed to the AI model, to
 * comply with the prompt length limit of the AI model
 */
data class SessionDataDTO(
    /**
     * the prompt that will be passed to the AI model, which contains the session headers, requests,
     * request body, url parameters, etc..
     */
    val prompt: String,
    /**
     * the screenshots that are privacy or ToS related, which will be passed to the AI model
     */
    val privacyOrTosRelatedScreenshots: List<ScreenshotEntity>?
)