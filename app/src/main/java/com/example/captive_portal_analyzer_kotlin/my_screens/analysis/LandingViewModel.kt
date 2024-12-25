package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.dataclasses.NetworkItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


sealed class LandingUiState {
    object Loading : LandingUiState()
    object LoadNetworkSuccess : LandingUiState()
    object NoOpenNetworks : LandingUiState()
    object ConnectionSuccess : LandingUiState()
    data class Error(val messageStringResource: Int) : LandingUiState()
}


class LandingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<LandingUiState>(LandingUiState.Loading)
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    private val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _openWifiNetworks = MutableStateFlow<List<NetworkItem>>(emptyList())
    val openWifiNetworks: StateFlow<List<NetworkItem>> = _openWifiNetworks.asStateFlow()

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val context: Context get() = getApplication<Application>().applicationContext

    init {

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToNetwork(ssid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val networkSpecifier = android.net.wifi.WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .build()

            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(networkSpecifier)
                .build()

            connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network) // Bind to the Wi-Fi network

                    // Notify UI of successful connection
                    viewModelScope.launch(Dispatchers.Main) {
                        _uiState.value = LandingUiState.ConnectionSuccess
                    }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    _uiState.value = LandingUiState.Error(R.string.error_connecting_to_wifi)
                    // Handle connection failure
                }
            })
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
            val  uniqueOpenNetworks = uniqueNetworks.filter { !isNetworkSecured(it) }

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
                _uiState.value = LandingUiState.LoadNetworkSuccess
            }else{
                _uiState.value = LandingUiState.NoOpenNetworks
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
        if (modelClass.isAssignableFrom(LandingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LandingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}*/
