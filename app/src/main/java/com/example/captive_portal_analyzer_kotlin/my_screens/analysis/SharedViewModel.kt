package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import androidx.lifecycle.ViewModel
import com.example.captive_portal_analyzer_kotlin.components.ActionAlertDialogData
import com.example.captive_portal_analyzer_kotlin.room.custom_webview_request.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.room.network_session.NetworkSessionEntity
import com.example.captive_portal_analyzer_kotlin.room.screenshots.ScreenshotEntity
import com.example.captive_portal_analyzer_kotlin.room.webpage_content.WebpageContentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedViewModel : ViewModel() {

    /*show action alert dialogs from anywhere in the app*/
    private val _actionAlertDialogData = MutableStateFlow<ActionAlertDialogData?>(null)
    val actionAlertDialogData: StateFlow<ActionAlertDialogData?> = _actionAlertDialogData


    private val _clickedSession = MutableStateFlow<NetworkSessionEntity?>(null)
    val clickedSession: StateFlow<NetworkSessionEntity?> = _clickedSession

    private val _clickedSessionRequests = MutableStateFlow<List<CustomWebViewRequestEntity>>(emptyList())
    val clickedSessionRequests: StateFlow<List<CustomWebViewRequestEntity>> = _clickedSessionRequests

    private val _clickedSessionContentList = MutableStateFlow<List<WebpageContentEntity>>(emptyList())
    val clickedSessionContentList: StateFlow<List<WebpageContentEntity>> = _clickedSessionContentList

    private val _clickedSessionScreenshotList = MutableStateFlow<List<ScreenshotEntity>>(emptyList())
    val clickedSessionScreenshotList: StateFlow<List<ScreenshotEntity>> = _clickedSessionScreenshotList


    fun showDialog(actionAlertDialogData: ActionAlertDialogData) {
        _actionAlertDialogData.value = actionAlertDialogData
    }

    fun hideDialog() {
        _actionAlertDialogData.value = _actionAlertDialogData.value?.copy(showDialog = false)
    }


    fun clickedSession(
        session: NetworkSessionEntity,
        requests: List<CustomWebViewRequestEntity>,
        contentList: List<WebpageContentEntity>,
        screenshotList: List<ScreenshotEntity>
    ) {
        _clickedSession
            .value = session
        _clickedSessionRequests
            .value = requests
        _clickedSessionContentList
            .value = contentList
        _clickedSessionScreenshotList
            .value = screenshotList

    }
}
