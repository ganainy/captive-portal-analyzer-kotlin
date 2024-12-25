package com.example.captive_portal_analyzer_kotlin.utils

import android.content.Context
import android.util.Log
import com.fabricethilaw.sonarnet.ConnectivityCallback
import com.fabricethilaw.sonarnet.ConnectivityResult
import com.fabricethilaw.sonarnet.InternetStatus
import com.fabricethilaw.sonarnet.SonarNet

private fun getNetworkState(context: Context) {

    //get status first time on app open
    SonarNet.ping { result ->
        when(result) {
            InternetStatus.INTERNET -> {}
            InternetStatus.NO_INTERNET -> {}
            InternetStatus.CAPTIVE_PORTAL -> {}
        }
    }

    //get notified every time network changes
    val connectivityCallback = object : ConnectivityCallback {
        override fun onConnectionChanged(result: ConnectivityResult) {
            // Check the result, see the Using Results section
            when(result.internetStatus) {
                InternetStatus.INTERNET -> {
                    Log.d("LandingViewModel", "INTERNET")
                }
                InternetStatus.NO_INTERNET -> {
                    Log.d("LandingViewModel", "NO INTERNET")
                }
                InternetStatus.CAPTIVE_PORTAL -> {
                    Log.d("LandingViewModel", "CAPTIVE")
                }
            }
        }
    }
    SonarNet(context).registerConnectivityCallback(connectivityCallback)
}