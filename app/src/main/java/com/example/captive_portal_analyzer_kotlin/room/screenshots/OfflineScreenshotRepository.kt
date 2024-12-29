package com.example.captive_portal_analyzer_kotlin.room.screenshots

import kotlinx.coroutines.flow.Flow


interface IScreenshotRepository {
    suspend fun insertScreenshot(screenshot: ScreenshotEntity)
    suspend fun updateScreenshot(screenshot: ScreenshotEntity)
    suspend fun deleteScreenshot(screenshot: ScreenshotEntity)
    suspend fun getScreenshot(screenshotId: String) : Flow<ScreenshotEntity?>
    suspend fun getAllScreenshotsForSession(sessionId: String): Flow<List<ScreenshotEntity>>
}

class OfflineScreenshotRepository(private val screenshotDao: ScreenshotDao) :
    IScreenshotRepository {

    override suspend fun insertScreenshot(screenshot: ScreenshotEntity) {
        screenshotDao.insert(screenshot)
    }

    override suspend fun updateScreenshot(screenshot: ScreenshotEntity) {
        screenshotDao.update(screenshot)
    }

    override suspend fun deleteScreenshot(screenshot: ScreenshotEntity) {
        screenshotDao.delete(screenshot)
    }

    override suspend fun getScreenshot(screenshotId: String) : Flow<ScreenshotEntity?>  {
        return screenshotDao.getScreenshot(screenshotId)
    }

    override suspend fun getAllScreenshotsForSession(sessionId: String): Flow<List<ScreenshotEntity>> {
        return screenshotDao.getAllScreenshotsForSession(sessionId)
    }
}

