package com.example.captive_portal_analyzer_kotlin

import SessionData
import androidx.lifecycle.ViewModel
import com.example.captive_portal_analyzer_kotlin.components.DialogState
import com.example.captive_portal_analyzer_kotlin.components.ToastState
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedViewModel : ViewModel() {


    /*show action alert dialogs from anywhere in the app*/
    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState = _dialogState.asStateFlow()


    private val _clickedSessionId = MutableStateFlow<String?>(null)
    val clickedSessionId: StateFlow<String?> = _clickedSessionId


    private val _toastState = MutableStateFlow<ToastState>(ToastState.Hidden)
    val toastState = _toastState.asStateFlow()


    fun updateClickedSessionId(clickedSessionId: String?)  {
        _clickedSessionId.value = clickedSessionId
    }


    fun showDialog(
        title: String,
        message: String,
        confirmText: String = "OK",
        dismissText: String = "Cancel",
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        _dialogState.value = DialogState.Shown(
            title = title,
            message = message,
            confirmText = confirmText,
            dismissText = dismissText,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    fun hideDialog() {
        _dialogState.value = DialogState.Hidden
    }


     fun showToast(title: String? = null, message: String, style: ToastStyle, duration: Long? = 2000L) {
        _toastState.value = ToastState.Shown(title, message, style, duration)
    }



    fun hideToast() {
        _toastState.value = ToastState.Hidden
    }

}
