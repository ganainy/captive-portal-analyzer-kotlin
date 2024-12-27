package com.example.captive_portal_analyzer_kotlin.room

import kotlinx.coroutines.flow.Flow


class OfflineCustomWebViewRequestsRepository(private val customWebViewRequestDao: CustomWebViewRequestDao) : CustomWebViewRequestsRepository {
    override fun getAllItemsStream(): Flow<List<CustomWebViewRequest>> = customWebViewRequestDao.getAllCustomWebViewRequest()

    override fun getAllDomainCustomWebViewRequest(bssid: String): Flow<List<CustomWebViewRequest>> = customWebViewRequestDao.getAllDomainCustomWebViewRequest(bssid)

    override fun getItemStream(id: Int): Flow<CustomWebViewRequest?> = customWebViewRequestDao.getCustomWebViewRequest(id)

    override suspend fun insertItem(item: CustomWebViewRequest) = customWebViewRequestDao.insert(item)

    override suspend fun deleteItem(item: CustomWebViewRequest) = customWebViewRequestDao.delete(item)

    override suspend fun updateItem(item: CustomWebViewRequest) = customWebViewRequestDao.update(item)
}

