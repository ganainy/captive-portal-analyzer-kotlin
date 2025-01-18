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
/**
 * Represents the UI state of the Manual Connect screen.
 */
sealed class ManualConnectUiState {
    /**
     * Represents the loading state.
     */
    object Loading : ManualConnectUiState()

    /**
     * Represents the state where permissions are requested.
     */
    object AskPermissions : ManualConnectUiState()

    /**
     * Represents the state where permissions have been granted.
     */
    object PermissionsGranted : ManualConnectUiState()

    /**
     * Represents an error state with a message resource ID.
     *
     * @param messageStringResource The resource ID of the error message.
     */
    data class Error(val messageStringResource: Int) : ManualConnectUiState()
}

/**
 * Represents the state of permissions.
 */
sealed class PermissionState {
    /**
     * Represents the state where permissions are granted.
     */
    object Granted : PermissionState()

    /**
     * Represents the state where permissions are denied.
     */
    object Denied : PermissionState()

    /**
     * Represents the state where a rationale for permissions should be shown.
     */
    object ShouldShowRationale : PermissionState()

    /**
     * Represents the state where permissions need to be requested.
     */
    object NeedsRequest : PermissionState()
}
/**
 * ViewModel for the Manual Connect screen.
 *
 * This ViewModel handles the state of the Manual Connect screen. It checks if the user has granted
 * the necessary permissions and if Wi-Fi is enabled. It also checks if the user is connected to a
 * Wi-Fi network.
 *
 * The ViewModel uses a MutableStateFlow to manage the state of the screen. The state is exposed as a
 * StateFlow, which is a read-only flow that can be collected in a composable.
 *
 * The ViewModel also uses a MutableStateFlow to manage the state of the permissions. The state is
 * exposed as a StateFlow, which is a read-only flow that can be collected in a composable.
 */
class ManualConnectViewModel(private val application: Application) : AndroidViewModel(application) {

    // Data store for storing preferences
    private val dataStore = application.preferencesDataStore

    // Key for storing the welcome screen preference
    private val SHOW_WELCOME_SCREEN_KEY = booleanPreferencesKey("show_welcome_screen")

    /**
     * State of Wi-Fi being on/off.
     */
    private val _isWifiOn = MutableStateFlow<Boolean>(false)
    val isWifiOn: StateFlow<Boolean> = _isWifiOn.asStateFlow()

    /**
     * State of cellular being on/off.
     */
    private val _isCellularOn = MutableStateFlow<Boolean>(false)
    val isCellularOn: StateFlow<Boolean> = _isCellularOn.asStateFlow()

    /**
     * State of being connected to a Wi-Fi network or not.
     */
    private val _isConnectedToWifiNetwork = MutableStateFlow<Boolean>(false)
    val isConnectedToWifiNetwork: StateFlow<Boolean> = _isConnectedToWifiNetwork.asStateFlow()

    /**
     * State of all requirements (cellular off/wifi on/connected to wifi network) being fulfilled.
     */
    private val _areAllRequirementsFulfilled = MutableStateFlow<Boolean>(false)
    val areAllRequirementsFulfilled: StateFlow<Boolean> = _areAllRequirementsFulfilled.asStateFlow()

    /**
     * State of the UI.
     */
    private val _uiState = MutableStateFlow<ManualConnectUiState>(ManualConnectUiState.Loading)
    val uiState: StateFlow<ManualConnectUiState> = _uiState.asStateFlow()

    /**
     * State of the permissions.
     */
    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.NeedsRequest)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    /**
     * Continuously check if Wi-Fi is enabled on the device using a BroadcastReceiver.
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
    /**
     * Inits the ViewModel.
     *
     * Checks the initial state of the permissions on screen open and registers a BroadcastReceiver to listen for
     * Wi-Fi state changes. Also starts checking the network connection.
     */
    init {
        checkPermissions()
        // Register the BroadcastReceiver to listen for Wi-Fi state changes
        val intentFilter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        application.registerReceiver(wifiStateReceiver, intentFilter)
        startCheckingNetworkConnection()

        //if everything is ready move to analysis page
        viewModelScope.launch {
            // Combine the state of Wi-Fi being on, cellular being off, and connected to a Wi-Fi network
            // into one flow that is true if all of them are true
            combine(
                isWifiOn,
                isCellularOn,
                isConnectedToWifiNetwork
            ) { wifiOn, cellularOn, connectedToWifi ->
                wifiOn && !cellularOn && connectedToWifi
            }.collect { allTrue ->
                // Update the state if all requirements are fulfilled
                _areAllRequirementsFulfilled.value = allTrue
            }
        }
    }

    /**
     * Checks if the user has given all the required permissions.
     *
     * If all permissions are granted, updates the state to [PermissionState.Granted] and
     * [ManualConnectUiState.PermissionsGranted].
     *
     * If all permissions are not granted, updates the state to [PermissionState.NeedsRequest]
     * and [ManualConnectUiState.AskPermissions].
     */
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

    /**
     * Checks if the user has given all the required permissions.
     *
     * @return true if all permissions are granted, false otherwise
     */
    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                application,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    /**
     * If all permissions are granted, updates the state to [PermissionState.Granted] and
     * [ManualConnectUiState.PermissionsGranted].
     *
     * If all permissions are not granted, updates the state to [PermissionState.Denied]
     * and [ManualConnectUiState.Error] with a message indicating that the permissions were
     * denied.
     */
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
        /**
         * The permissions required by the Captive Portal Analyzer app.
         *
         * This is a list of all the permissions required by the Captive Portal Analyzer app.
         * * NEARBY_WIFI_DEVICES to detect nearby Wi-Fi devices (only on Android 12 and later)
         */
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

    /**
     * Unregisters the BroadcastReceiver when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        // Unregister the BroadcastReceiver when the ViewModel is cleared
        getApplication<Application>().unregisterReceiver(wifiStateReceiver)
    }


    /**
     * Checks if the device is connected to a Wi-Fi network.
     * Note: This does not check for internet connectivity.
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

    /**
     * Periodically checks the network connection status.
     * This function launches a coroutine that runs in the ViewModel's scope.
     * It checks the network status every 2 seconds.
     */
    private fun startCheckingNetworkConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                checkConnectionToNetwork()  // Check network status
                delay(2000)  // Delay for 2 seconds before checking again
            }
        }
    }

}