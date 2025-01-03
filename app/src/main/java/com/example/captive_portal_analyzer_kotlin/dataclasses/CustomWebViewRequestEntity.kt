package com.example.captive_portal_analyzer_kotlin.dataclasses

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_webview_request")
data class CustomWebViewRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val customWebViewRequestId: Int = 0,
    val sessionId: String?,
    val type: String?= null,
    val url: String?,
    val method: String?,
    val body: String?= null,
    val headers: String? // Stored as JSON string
)