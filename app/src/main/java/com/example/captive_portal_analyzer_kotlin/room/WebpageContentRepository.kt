package com.example.captive_portal_analyzer_kotlin.room


import kotlinx.coroutines.flow.Flow

// Repository interface
interface WebpageContentRepository {
    fun getAllContentStream(): Flow<List<WebpageContent>>
    fun getContentForUrlStream(url: String): Flow<WebpageContent?>
    fun getLatestContentStream(): Flow<List<WebpageContent>>
    suspend fun insertContent(content: WebpageContent)
    suspend fun deleteContent(content: WebpageContent)
    suspend fun deleteContentForUrl(url: String)
}

class OfflineWebpageContentRepository(private val webpageContentDao: WebpageContentDao) :
    WebpageContentRepository {
    override fun getAllContentStream(): Flow<List<WebpageContent>> =
        webpageContentDao.getAllContentStream()

    override fun getContentForUrlStream(url: String): Flow<WebpageContent?> =
        webpageContentDao.getContentForUrlStream(url)

    override fun getLatestContentStream(): Flow<List<WebpageContent>> =
        webpageContentDao.getLatestContentStream()

    override suspend fun insertContent(content: WebpageContent) =
        webpageContentDao.insert(content)

    override suspend fun deleteContent(content: WebpageContent) =
        webpageContentDao.delete(content)

    override suspend fun deleteContentForUrl(url: String) =
        webpageContentDao.deleteByUrl(url)
}

