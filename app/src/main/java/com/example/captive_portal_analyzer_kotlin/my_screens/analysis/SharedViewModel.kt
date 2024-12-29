package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import androidx.lifecycle.ViewModel
import com.example.captive_portal_analyzer_kotlin.components.ActionAlertDialogData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedViewModel  : ViewModel(){

    /*show action alert dialogs from anywhere in the app*/
    private val _actionAlertDialogData= MutableStateFlow<ActionAlertDialogData?>(null)
    val actionAlertDialogData: StateFlow<ActionAlertDialogData?> = _actionAlertDialogData

    fun showDialog(actionAlertDialogData: ActionAlertDialogData) {
        _actionAlertDialogData.value = actionAlertDialogData
    }

    fun hideDialog() {
        _actionAlertDialogData.value = _actionAlertDialogData.value?.copy(showDialog = false)
    }
}
