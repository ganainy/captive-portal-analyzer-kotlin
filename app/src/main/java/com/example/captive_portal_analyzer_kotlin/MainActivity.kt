package com.example.captive_portal_analyzer_kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.captive_portal_analyzer_kotlin.navigation.AppNavGraph
import com.example.captive_portal_analyzer_kotlin.room.AppDatabase
import com.example.captive_portal_analyzer_kotlin.room.network_session.OfflineNetworkSessionRepository
import com.example.captive_portal_analyzer_kotlin.theme.CaptivePortalAnalyzerComposeTheme
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import com.google.firebase.FirebaseApp
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: NetworkSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val offlineNetworkSessionRepository: OfflineNetworkSessionRepository by lazy {
            OfflineNetworkSessionRepository(AppDatabase.getDatabase(this).networkSessionDao())
        }

        // Start monitoring network changes
        sessionManager = NetworkSessionManager(this, offlineNetworkSessionRepository)
        sessionManager.startMonitoring()


        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        setContent {
            val navController = rememberNavController()
            val sharedViewModel: SharedViewModel = viewModel()
            val dialogState by sharedViewModel.dialogState.collectAsState()
            CaptivePortalAnalyzerComposeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(
                        navController = navController, sessionManager = sessionManager,
                        sharedViewModel = sharedViewModel,
                        dialogState = dialogState
                    )
                }
            }
        }
    }


}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    CaptivePortalAnalyzerComposeTheme {

    }
}