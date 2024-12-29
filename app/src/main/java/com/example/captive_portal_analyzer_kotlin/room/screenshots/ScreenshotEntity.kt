package com.example.captive_portal_analyzer_kotlin.room.screenshots

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey val screenshotId: String,
    val sessionId: String? = null,
    val timestamp: Long = 0,
    val path: String = "unknown",
    val size: String? = null,
    val url: String? = null,
)