package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.models

import kotlinx.serialization.Serializable

@Serializable
sealed interface PcapProcessingState {
    @Serializable data object Idle : PcapProcessingState
    @Serializable data class Uploading(val progress: Float) :
        PcapProcessingState
    @Serializable data class Processing(val jobId: String) :
        PcapProcessingState
    @Serializable data class Polling(val jobId: String, val attempt: Int) :
        PcapProcessingState
    @Serializable data class DownloadingJson(val jobId: String, val url: String) :
        PcapProcessingState // Include URL
    @Serializable data class Success(val jobId: String, val jsonContent: String) :
        PcapProcessingState
    @Serializable data class Error(val message: String, val jobId: String? = null) :
        PcapProcessingState
}