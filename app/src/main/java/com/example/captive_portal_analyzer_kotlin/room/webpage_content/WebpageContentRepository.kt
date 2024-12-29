package com.example.captive_portal_analyzer_kotlin.room.webpage_content


import kotlinx.coroutines.flow.Flow

// Repository interface
interface WebpageContentRepository {
    fun getAllContentStream(): Flow<List<WebpageContentEntity>>
    fun getContentForUrlStream(url: String): Flow<WebpageContentEntity?>
    fun getLatestContentStream(): Flow<List<WebpageContentEntity>>
    suspend fun insertContent(content: WebpageContentEntity)
    suspend fun deleteContent(content: WebpageContentEntity)
    suspend fun deleteContentForUrl(url: String)
}

class OfflineWebpageContentRepository(private val webpageContentDao: WebpageContentDao) :
    WebpageContentRepository {
    override fun getAllContentStream(): Flow<List<WebpageContentEntity>> =
        webpageContentDao.getAllContentStream()

    override fun getContentForUrlStream(url: String): Flow<WebpageContentEntity?> =
        webpageContentDao.getContentForUrlStream(url)

    override fun getLatestContentStream(): Flow<List<WebpageContentEntity>> =
        webpageContentDao.getLatestContentStream()

    override suspend fun insertContent(content: WebpageContentEntity) =
        webpageContentDao.insert(content)

    override suspend fun deleteContent(content: WebpageContentEntity) =
        webpageContentDao.delete(content)

    override suspend fun deleteContentForUrl(url: String) =
        webpageContentDao.deleteByUrl(url)
}

