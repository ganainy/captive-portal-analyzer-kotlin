package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.models

import kotlinx.serialization.Serializable

@Serializable
data class JobStatusResponse(
    val status: String,
    val url: String? = null, // Nullable if status is not 'completed'
    val error: String? = null // Nullable if status is not 'failed'
)