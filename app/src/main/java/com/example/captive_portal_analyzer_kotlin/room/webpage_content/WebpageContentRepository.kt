package com.example.captive_portal_analyzer_kotlin.room.webpage_content


import kotlinx.coroutines.flow.Flow

// Repository interface
interface WebpageContentRepository {
    fun getAllContentStream(): Flow<List<WebpageContentEntity>>
    suspend fun insertContent(content: WebpageContentEntity)
    suspend fun deleteContent(content: WebpageContentEntity)
    suspend fun deleteContentForUrl(url: String)
    fun getAllContentForSessionId(sessionId: String): Flow<List<WebpageContentEntity>>
}

class OfflineWebpageContentRepository(private val webpageContentDao: WebpageContentDao) :
    WebpageContentRepository {
    override fun getAllContentStream(): Flow<List<WebpageContentEntity>> =
        webpageContentDao.getAllContentStream()

    override fun getAllContentForSessionId(sessionId: String): Flow<List<WebpageContentEntity>> =
        webpageContentDao.getAllContentForSessionId(sessionId)

    override suspend fun insertContent(content: WebpageContentEntity) =
        webpageContentDao.insert(content)

    override suspend fun deleteContent(content: WebpageContentEntity) =
        webpageContentDao.delete(content)

    override suspend fun deleteContentForUrl(url: String) =
        webpageContentDao.deleteByUrl(url)
}

