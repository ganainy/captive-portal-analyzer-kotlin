package com.example.captive_portal_analyzer_kotlin.screens.network_list

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


sealed class NetworkListUiState {
    object Loading : NetworkListUiState()
    object AskPermissions : NetworkListUiState()
    object LoadNetworkSuccess : NetworkListUiState()
    object NoOpenNetworks : NetworkListUiState()
    object ConnectionSuccess : NetworkListUiState()
    data class Error(val messageStringResource: Int) : NetworkListUiState()
}

// Extension for DataStore
val Context.dataStore by preferencesDataStore(name = "app_preferences")


class NetworkListViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<NetworkListUiState>(NetworkListUiState.Loading)
    val uiState: StateFlow<NetworkListUiState> = _uiState.asStateFlow()

    private val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _openWifiNetworks = MutableStateFlow<List<NetworkItem>>(emptyList())
    val openWifiNetworks: StateFlow<List<NetworkItem>> = _openWifiNetworks.asStateFlow()

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val context: Context get() = getApplication<Application>().applicationContext

    private val dataStore = context.dataStore

    // Key for storing the welcome screen preference
    private val SHOW_WELCOME_SCREEN_KEY = booleanPreferencesKey("show_welcome_screen")


    init {
        loadShowWelcomeScreen()
    }

    private fun loadShowWelcomeScreen() {
        viewModelScope.launch {
            val showWelcomeScreen = dataStore.data.first()[SHOW_WELCOME_SCREEN_KEY] ?: true
            if (!showWelcomeScreen) {
                scanOpenWifiNetworks()
            } else {
                _uiState.value = NetworkListUiState.AskPermissions
            }
        }
    }

    fun updateShowWelcomeScreen(showWelcomeScreen: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SHOW_WELCOME_SCREEN_KEY] = showWelcomeScreen
            }
            if (showWelcomeScreen){
                _uiState.value = NetworkListUiState.AskPermissions
            }else{
                scanOpenWifiNetworks()
            }
        }
    }

    fun connectToNetwork(ssid: String, password: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For API 29 and above
            viewModelScope.launch(Dispatchers.IO) {
                val networkSpecifier = android.net.wifi.WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .apply {
                        password?.let {
                            setWpa2Passphrase(it)
                        }
                    }
                    .build()

                val networkRequest = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(networkSpecifier)
                    .build()

                connectivityManager.requestNetwork(
                    networkRequest,
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            super.onAvailable(network)
                            connectivityManager.bindProcessToNetwork(network)

                            // Notify UI of successful connection
                            viewModelScope.launch(Dispatchers.Main) {
                                _uiState.value = NetworkListUiState.ConnectionSuccess
                            }
                        }

                        override fun onUnavailable() {
                            super.onUnavailable()
                            // Notify UI of failure
                            viewModelScope.launch(Dispatchers.Main) {
                                _uiState.value =
                                    NetworkListUiState.Error(R.string.error_connecting_to_wifi)
                            }
                        }
                    }
                )
            }
        } else {
            // For API levels below 29
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$ssid\"" // Quote the SSID
                password?.let {
                    preSharedKey = "\"$it\"" // Quote the password
                }
            }

            val networkId = wifiManager.addNetwork(wifiConfig)
            if (networkId != -1) {
                wifiManager.disconnect()
                wifiManager.enableNetwork(networkId, true)
                wifiManager.reconnect()

                // Notify UI of successful connection
                _uiState.value = NetworkListUiState.ConnectionSuccess
            } else {
                // Notify UI of failure
                _uiState.value = NetworkListUiState.Error(R.string.error_connecting_to_wifi)
            }
        }
    }



    @SuppressLint("MissingPermission")
    fun scanOpenWifiNetworks() {
        viewModelScope.launch(Dispatchers.IO) {
            val results = wifiManager.scanResults

            // Remove duplicates by SSID and select the strongest signal
            val uniqueNetworks = results
                .filter { it.SSID.isNotEmpty() }
                .groupBy { it.SSID }
                .map { (_, networkGroup) ->
                    networkGroup.maxByOrNull { it.level }
                }
                .filterNotNull()

            //extract unique open networks with no password since its common for captive portal not
            //to have a password
            val uniqueOpenNetworks = uniqueNetworks.filter { !isNetworkSecured(it) }

            // Map to NetworkInfo with security status
            val openNetworkInfoList = uniqueOpenNetworks.map { scanResult ->
                NetworkItem(
                    scanResult = scanResult,
                    isSecured = isNetworkSecured(scanResult),
                    securityIcon = getSecurityIcon(scanResult)
                )
            }

            if (openNetworkInfoList.isNotEmpty()) {
                _openWifiNetworks.value = openNetworkInfoList
                _uiState.value = NetworkListUiState.LoadNetworkSuccess
            } else {
                _uiState.value = NetworkListUiState.NoOpenNetworks
            }
        }
    }

    private fun isNetworkSecured(scanResult: ScanResult): Boolean {
        return scanResult.capabilities.let { capabilities ->
            capabilities.contains("WPA") ||
                    capabilities.contains("WPA2") ||
                    capabilities.contains("WPA3") ||
                    capabilities.contains("RSN") ||
                    capabilities.contains("WEP") ||
                    capabilities.contains("PSK")
        }
    }

    private fun getSecurityIcon(scanResult: ScanResult): Int {
        return if (isNetworkSecured(scanResult)) {
            R.drawable.lock // Your locked network icon
        } else {
            R.drawable.unlock // Your open network icon
        }

    }


}


/*
class FeedViewModelFactory(private val repository: IDataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NetworkListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NetworkListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}*/
