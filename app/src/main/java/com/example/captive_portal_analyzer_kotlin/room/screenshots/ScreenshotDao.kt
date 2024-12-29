package com.example.captive_portal_analyzer_kotlin.room.screenshots

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(screenshot: ScreenshotEntity)

    @Update
    suspend fun update(screenshot: ScreenshotEntity)

    @Delete
    suspend fun delete(screenshot: ScreenshotEntity)


    @Query("SELECT * from screenshots WHERE screenshotId = :screenshotId")
    fun getScreenshot(screenshotId: String): Flow<ScreenshotEntity>


    @Query("SELECT * from screenshots WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getAllScreenshotsForSession(sessionId: String): Flow<List<ScreenshotEntity>>
}