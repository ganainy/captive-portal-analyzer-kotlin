package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
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



class ManualConnectViewModel(application: Application) : AndroidViewModel(application) {

    val context=application.applicationContext

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
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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


/*    fun checkCaptivePortal() {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://clients3.google.com/generate_204")
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                if (connection.responseCode != 204) {
                    _uiState.value = ManualConnectUiState.CaptivePortalConnectionSuccess
                } else {
                    _uiState.value =
                        ManualConnectUiState.Error(R.string.error_connecting_to_captive_portal)
                }
            } catch (e: Exception) {
                _uiState.value =
                    ManualConnectUiState.Error(R.string.error_connecting_to_captive_portal)
            } finally {
            }
        }
    }*/
}