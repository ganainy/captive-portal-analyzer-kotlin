package com.example.captive_portal_analyzer_kotlin.screens.setup_pcapdroid


import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class SetupPCAPDroidViewModel(
    private val application: Application
) : ViewModel() {




    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_PACKAGE_ADDED) {
                val packageName = intent.data?.schemeSpecificPart
                if (packageName == "com.emanuelef.remote_capture") {
                    checkPcapDroidInstalled()
                }
            }
        }
    }

    // SharedPreferences to store the skipSetup value
    private val _skipSetup = MutableStateFlow(false)
    val skipSetup: StateFlow<Boolean> get() = _skipSetup

    // current step of the pager
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> get() = _currentStep

    private val _isPcapDroidInstalled = MutableStateFlow(false)
    open val isPcapDroidInstalled: StateFlow<Boolean> get() = _isPcapDroidInstalled

    init {
        checkPcapDroidInstalled()
        // Register the receiver
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }
        application.registerReceiver(packageReceiver, filter)
    }

    // Function to save the skipSetup value to SharedPreferences
    fun setSkipSetup(value: Boolean) {
        _skipSetup.value = value
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister the receiver to avoid leaks
        application.unregisterReceiver(packageReceiver)
    }

    private fun checkPcapDroidInstalled() {
        viewModelScope.launch {
            val isInstalled = try {
                application.packageManager.getPackageInfo("com.emanuelef.remote_capture", 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
            _isPcapDroidInstalled.value = isInstalled
        }
}
    // Public method to refresh the status (e.g., after returning from Play Store)
    fun refreshPcapDroidStatus() {
        checkPcapDroidInstalled()
    }

    //  functions to control steps
    fun setStep(step: Int) {
        _currentStep.value = step.coerceIn(0, 5) // Ensure step stays within valid range (0-5)
    }

    fun nextStep() {
        _currentStep.value = (_currentStep.value + 1).coerceAtMost(5)
    }

    fun previousStep() {
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(0)
    }

}

class SetupPCAPDroidViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetupPCAPDroidViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetupPCAPDroidViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}