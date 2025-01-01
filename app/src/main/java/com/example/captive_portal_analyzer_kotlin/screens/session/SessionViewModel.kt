package com.example.captive_portal_analyzer_kotlin.screens.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.Report
import com.example.captive_portal_analyzer_kotlin.firebase.OnlineRepository
import com.example.captive_portal_analyzer_kotlin.room.network_session.OfflineNetworkSessionRepository
import com.example.captive_portal_analyzer_kotlin.room.screenshots.ScreenshotEntity
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class SessionViewModel(
    application: Application,
    private val onlineRepository: OnlineRepository,
    private val offlineNetworkSessionRepository: OfflineNetworkSessionRepository,
) : AndroidViewModel(application) {


    private val _isUploading = MutableStateFlow<Boolean>(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()


    init {
        viewModelScope.launch(Dispatchers.IO) {

        }
    }

    fun uploadReport(report: Report,showToast:(message: String,style: ToastStyle) -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            onlineRepository.uploadReport(report)
                .onSuccess {
                    _isUploading.value = false
                    uploadImages(report.screenshots, report.session.sessionId,showToast)
                }
                .onFailure { apiResponse ->
                    _isUploading.value = false
                    apiResponse.message?.let { it1 -> showToast(it1,ToastStyle.ERROR) }
                }
        }
    }


    private fun uploadImages(screenshots: List<ScreenshotEntity>?, sessionId: String,showToast:(message: String,style: ToastStyle) -> Unit) {

        viewModelScope.launch(Dispatchers.IO) {

            if (screenshots.isNullOrEmpty()) {
                offlineNetworkSessionRepository.updateIsUploadedToRemoteServer(sessionId, true)
                _isUploading.value = false
            } else {
                screenshots.forEach { screenshot ->
                    _isUploading.value = true
                    screenshot.sessionId?.let {
                        onlineRepository.uploadImage(
                            imagePath = screenshot.path,
                            screenshotId = screenshot.screenshotId,
                            sessionId = it
                        )
                            .onSuccess {
                                offlineNetworkSessionRepository.updateIsUploadedToRemoteServer(
                                    sessionId,
                                    true
                                )
                                _isUploading.value = false
                            }
                            .onFailure { apiResponse ->
                                apiResponse.message?.let { it1 -> showToast(it1,ToastStyle.ERROR) }
                                _isUploading.value = false
                            }
                    }
                }


            }


        }
    }
}

class SessionViewModelFactory(
    private val application: Application,
    private val onlineRepository: OnlineRepository,
    private val offlineNetworkSessionRepository: OfflineNetworkSessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(
                application,
                onlineRepository,
                offlineNetworkSessionRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
