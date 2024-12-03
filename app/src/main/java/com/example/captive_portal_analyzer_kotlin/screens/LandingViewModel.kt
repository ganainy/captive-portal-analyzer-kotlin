package com.example.captive_portal_analyzer_kotlin.screens

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.repository.IDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


sealed class LandingUiState {
    object Loading : LandingUiState()
    object Initial : LandingUiState()
    data class Error(val messageStringResource: Int) : LandingUiState()
}


class LandingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<LandingUiState>(LandingUiState.Initial)
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    private val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val _wifiNetworks = MutableStateFlow<List<ScanResult>>(emptyList())
    val wifiNetworks: StateFlow<List<ScanResult>> = _wifiNetworks

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @SuppressLint("MissingPermission")
    fun scanWifiNetworks() {
        viewModelScope.launch(Dispatchers.IO) {
            val results = wifiManager.scanResults
            val openNetworks = results.filter { it.capabilities.contains("[ESS]") } // Open networks
            _wifiNetworks.value = openNetworks
        }
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
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    // Handle connection failure
                }
            })
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
