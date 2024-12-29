package com.example.captive_portal_analyzer_kotlin.room

import kotlinx.coroutines.flow.Flow

/**
 * Repository that provides insert, update, delete, and retrieve of [CustomWebViewRequest] from a given data source.
 */
interface CustomWebViewRequestsRepository {
    /**
     * Retrieve all the items from the the given data source.
     */
    fun getAllItemsStream(): Flow<List<CustomWebViewRequest>>
    /**
     * Retrieve all the items from the the given data source that matches with the [bssid].
     */
    fun getAllDomainCustomWebViewRequest(bssid:String): Flow<List<CustomWebViewRequest>>

    /**
     * Retrieve an item from the given data source that matches with the [id].
     */
    fun getItemStream(id: Int): Flow<CustomWebViewRequest?>

    /**
     * Insert item in the data source
     */
    suspend fun insertItem(item: CustomWebViewRequest)

    /**
     * Delete item from the data source
     */
    suspend fun deleteItem(item: CustomWebViewRequest)

    /**
     * Update item in the data source
     */
    suspend fun updateItem(item: CustomWebViewRequest)
}


class OfflineCustomWebViewRequestsRepository(private val customWebViewRequestDao: CustomWebViewRequestDao) : CustomWebViewRequestsRepository {
    override fun getAllItemsStream(): Flow<List<CustomWebViewRequest>> = customWebViewRequestDao.getAllCustomWebViewRequest()

    override fun getAllDomainCustomWebViewRequest(bssid: String): Flow<List<CustomWebViewRequest>> = customWebViewRequestDao.getAllDomainCustomWebViewRequest(bssid)

    override fun getItemStream(id: Int): Flow<CustomWebViewRequest?> = customWebViewRequestDao.getCustomWebViewRequest(id)

    override suspend fun insertItem(item: CustomWebViewRequest) = customWebViewRequestDao.insert(item)

    override suspend fun deleteItem(item: CustomWebViewRequest) = customWebViewRequestDao.delete(item)

    override suspend fun updateItem(item: CustomWebViewRequest) = customWebViewRequestDao.update(item)
}


