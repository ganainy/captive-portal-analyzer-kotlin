package com.example.captive_portal_analyzer_kotlin.dataclasses

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val screenshotId: Int = 0,
    val sessionId: String,
    val timestamp: Long,
    val path: String,
    val size: String?,
    val url: String?
)
