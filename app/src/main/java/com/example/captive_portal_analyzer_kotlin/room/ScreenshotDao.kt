package com.example.captive_portal_analyzer_kotlin.room

import com.example.captive_portal_analyzer_kotlin.dataclasses.ScreenshotEntity


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {

    /**
     * Inserts a screenshot into the database.
     * If a conflict occurs, the insert operation is ignored.
     *
     * @param screenshot The screenshot entity to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(screenshot: ScreenshotEntity)

    /**
     * Updates an existing screenshot in the database.
     *
     * @param screenshot The screenshot entity with updated information.
     */
    @Update
    suspend fun update(screenshot: ScreenshotEntity)

    /**
     * Deletes a screenshot from the database.
     *
     * @param screenshot The screenshot entity to be deleted.
     */
    @Delete
    suspend fun delete(screenshot: ScreenshotEntity)

    /**
     * Retrieves a specific screenshot from the database by its ID.
     *
     * @param screenshotId The ID of the screenshot to retrieve.
     * @return A Flow emitting the screenshot entity.
     */
    @Query("SELECT * from screenshots WHERE screenshotId = :screenshotId")
    fun getScreenshot(screenshotId: String): Flow<ScreenshotEntity>

    /**
     * Retrieves a list of screenshots associated with a specific session, ordered by timestamp in descending order.
     *
     * @param sessionId The ID of the session for which screenshots are to be retrieved.
     * @return A Flow emitting a list of screenshot entities.
     */
    @Query("SELECT * from screenshots WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getSessionScreenshotsList(sessionId: String): Flow<List<ScreenshotEntity>>

    /**
     * Checks if a screenshot is unique in the database by comparing its URL, size, and sessionId.
     *
     * @param url The URL of the screenshot.
     * @param size The size of the screenshot.
     * @param sessionId The session ID associated with the screenshot.
     * @return The count of screenshots matching the criteria.
     */
    @Query(
        "SELECT COUNT(*) FROM screenshots WHERE url = :url AND size = :size AND sessionId = :sessionId"
    )
    abstract fun isScreenshotUnique(url: String?, size: String?, sessionId: String): Any
}