package com.example.captive_portal_analyzer_kotlin.room.custom_webview_request

import kotlinx.coroutines.flow.Flow

/**
 * Repository that provides insert, update, delete, and retrieve of [CustomWebViewRequestEntity] from a given data source.
 */
interface CustomWebViewRequestsRepository {
    /**
     * Retrieve all the items from the the given data source.
     */
    fun getAllItemsStream(): Flow<List<CustomWebViewRequestEntity>>
    /**
     * Retrieve all the items from the the given data source that matches with the [bssid].
     */
    fun getAllDomainCustomWebViewRequest(bssid:String): Flow<List<CustomWebViewRequestEntity>>

    /**
     * Retrieve an item from the given data source that matches with the [id].
     */
    fun getItemStream(id: Int): Flow<CustomWebViewRequestEntity?>

    /**
     * Insert item in the data source
     */
    suspend fun insertItem(item: CustomWebViewRequestEntity)

    /**
     * Delete item from the data source
     */
    suspend fun deleteItem(item: CustomWebViewRequestEntity)

    /**
     * Update item in the data source
     */
    suspend fun updateItem(item: CustomWebViewRequestEntity)
}


class OfflineCustomWebViewRequestsRepository(private val customWebViewRequestDao: CustomWebViewRequestDao) :
    CustomWebViewRequestsRepository {
    override fun getAllItemsStream(): Flow<List<CustomWebViewRequestEntity>> = customWebViewRequestDao.getAllCustomWebViewRequest()

    override fun getAllDomainCustomWebViewRequest(bssid: String): Flow<List<CustomWebViewRequestEntity>> = customWebViewRequestDao.getSessionCustomWebViewRequest(bssid)

    override fun getItemStream(id: Int): Flow<CustomWebViewRequestEntity?> = customWebViewRequestDao.getCustomWebViewRequest(id)

    override suspend fun insertItem(item: CustomWebViewRequestEntity) = customWebViewRequestDao.insert(item)

    override suspend fun deleteItem(item: CustomWebViewRequestEntity) = customWebViewRequestDao.delete(item)

    override suspend fun updateItem(item: CustomWebViewRequestEntity) = customWebViewRequestDao.update(item)
}


