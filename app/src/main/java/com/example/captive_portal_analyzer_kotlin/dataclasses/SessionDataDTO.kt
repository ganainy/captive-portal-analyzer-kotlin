package com.example.captive_portal_analyzer_kotlin.dataclasses


//only the fields of the SessionData that will be passed to the AI model
data class SessionDataDTO(val prompt: String, val privacyOrTosRelatedScreenshots: List<ScreenshotEntity>)