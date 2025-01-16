package com.example.captive_portal_analyzer_kotlin.screens.manual_connect

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.datastore.preferencesDataStore

sealed class ManualConnectUiState {
    object Loading : ManualConnectUiState()
    object AskPermissions : ManualConnectUiState()
    object PermissionsGranted : ManualConnectUiState()
    data class Error(val messageStringResource: Int) : ManualConnectUiState()
}

sealed class PermissionState {
    object Granted : PermissionState()
    object Denied : PermissionState()
    object ShouldShowRationale : PermissionState()
    object NeedsRequest : PermissionState()
}

class ManualConnectViewModel(private val application: Application) : AndroidViewModel(application) {

    private val dataStore = application.preferencesDataStore

    // Key for storing the welcome screen preference
    private val SHOW_WELCOME_SCREEN_KEY = booleanPreferencesKey("show_welcome_screen")

    private val _isWifiOn = MutableStateFlow<Boolean>(false)
    val isWifiOn: StateFlow<Boolean> = _isWifiOn.asStateFlow()

    private val _isCellularOn = MutableStateFlow<Boolean>(false)
    val isCellularOn: StateFlow<Boolean> = _isCellularOn.asStateFlow()

    private val _isConnectedToWifiNetwork = MutableStateFlow<Boolean>(false)
    val isConnectedToWifiNetwork: StateFlow<Boolean> = _isConnectedToWifiNetwork.asStateFlow()

    private val _areAllRequirementsFulfilled = MutableStateFlow<Boolean>(false)
    val areAllRequirementsFulfilled: StateFlow<Boolean> = _areAllRequirementsFulfilled.asStateFlow()

    private val connectivityManager =
        application.getSystemService(ConnectivityManager::class.java)

    private val wifiManager =
        application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


    private val _uiState = MutableStateFlow<ManualConnectUiState>(ManualConnectUiState.Loading)
    val uiState: StateFlow<ManualConnectUiState> = _uiState.asStateFlow()

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.NeedsRequest)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    /**
     * Continuously check if Wi-Fi is enabled on the device.
     */
    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                val wifiState =
                    intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                    _isWifiOn.value = true
                } else {
                    _isWifiOn.value = false
                }
            }
        }
    }

    init {
        checkPermissions()
        // Register the BroadcastReceiver to listen for Wi-Fi state changes
        val intentFilter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        application.registerReceiver(wifiStateReceiver, intentFilter)
        startCheckingNetworkConnection()

        //if everything is ready move to analysis page
        viewModelScope.launch {
            combine(
                isWifiOn,
                isCellularOn,
                isConnectedToWifiNetwork
            ) { wifiOn, cellularOn, connectedToWifi ->
                wifiOn && !cellularOn && connectedToWifi
            }.collect { allTrue ->
                    _areAllRequirementsFulfilled.value = allTrue
            }
        }

    }

    fun checkPermissions() {
        val hasPermissions = hasRequiredPermissions()
        if (hasPermissions) {
            _permissionState.value = PermissionState.Granted
            _uiState.value = ManualConnectUiState.PermissionsGranted
        } else {
            if (_uiState.value is ManualConnectUiState.Error) {
                // Keep showing error state if we're already there
                _permissionState.value = PermissionState.Denied
            } else {
                _permissionState.value = PermissionState.NeedsRequest
                _uiState.value = ManualConnectUiState.AskPermissions
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                application,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isPermissionPermanentlyDenied(): Boolean {
        val preferences = application.getSharedPreferences("permissions", Context.MODE_PRIVATE)
        return REQUIRED_PERMISSIONS.any { permission ->
            !hasRequiredPermissions() &&
                    preferences.getBoolean("permission_requested_$permission", false)
        }
    }

    fun onPermissionResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            _permissionState.value = PermissionState.Granted
            _uiState.value = ManualConnectUiState.PermissionsGranted
        } else {
            _permissionState.value = PermissionState.Denied
            _uiState.value = ManualConnectUiState.Error(R.string.permissions_denied_message)
        }
    }

    companion object {
        val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister the BroadcastReceiver when the ViewModel is cleared
        getApplication<Application>().unregisterReceiver(wifiStateReceiver)
    }


    /**
     * Checks if the device is connected to a Wi-Fi network.
     * This does not check for internet connectivity.
     */
    private fun checkConnectionToNetwork() {
        val connectivityManager = application.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                _isCellularOn.value = true
                _isConnectedToWifiNetwork.value = false
            }
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                _isConnectedToWifiNetwork.value = true
                _isCellularOn.value = false
            }
            else -> {
                _isCellularOn.value = false
                _isConnectedToWifiNetwork.value = false
            }
        }
    }

    private fun startCheckingNetworkConnection() {
        // Launch a coroutine in the ViewModel's scope
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                checkConnectionToNetwork()  // Check network status
                delay(2000)  // Delay for 5 seconds before checking again
            }
        }
    }

    fun updateShowWelcomeScreen(showWelcomeScreen: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SHOW_WELCOME_SCREEN_KEY] = showWelcomeScreen
            }
            if (showWelcomeScreen){
                _uiState.value = ManualConnectUiState.AskPermissions
            }
        }
    }



}