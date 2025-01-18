package com.example.captive_portal_analyzer_kotlin.dataclasses

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a screenshot taken during a network session.
 *
 * @property sessionId Unique identifier of the session the screenshot belongs to.
 * @property timestamp Timestamp when the screenshot was taken.
 * @property path Path to the file where the screenshot is stored on device.
 * @property size Size of the screenshot in bytes, as a string.
 * @property url URL of the webpage the screenshot shows, if any.
 * @property isPrivacyOrTosRelated Whether the screenshot is related to privacy or terms of service,
 * user sets this manually to make it easier to filter privacy related screenshots
 */
@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val screenshotId: Int = 0,
    val sessionId: String,
    val timestamp: Long,
    val path: String,
    val size: String?,
    val url: String?,
    val isPrivacyOrTosRelated: Boolean=false,
)
