package com.example.captive_portal_analyzer_kotlin.dataclasses

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Relation

// Define relationships between entities
@Entity(
    tableName = "session_screenshot_cross_ref",
    primaryKeys = ["sessionId", "screenshotId"],
    foreignKeys = [
        ForeignKey(
            entity = NetworkSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SessionScreenshotCrossRef(
    val sessionId: String,
    val screenshotId: String
)

// Room relationships
data class SessionWithDetails(
    @Embedded val session: NetworkSessionEntity,
    @Relation(
        parentColumn = "sessionId",
        entityColumn = "sessionId"
    )
    val requests: List<CustomWebViewRequestEntity>,
    @Relation(
        parentColumn = "sessionId",
        entityColumn = "sessionId"
    )
    val webpageContent: List<WebpageContentEntity>,
    @Relation(
        parentColumn = "sessionId",
        entityColumn = "sessionId"
    )
    val screenshots: List<ScreenshotEntity>
)