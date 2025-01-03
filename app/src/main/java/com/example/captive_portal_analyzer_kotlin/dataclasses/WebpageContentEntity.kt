package com.example.captive_portal_analyzer_kotlin.dataclasses

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "webpage_content")
data class WebpageContentEntity(
    @PrimaryKey(autoGenerate = true) val contentId: Int = 0,
    val sessionId: String,
    val url: String,
    val htmlContent: String,
    val jsContent: String,
    val timestamp: Long,
)