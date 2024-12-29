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
import com.example.captive_portal_analyzer_kotlin.components.ActionAlertDialog
import com.example.captive_portal_analyzer_kotlin.my_screens.analysis.SharedViewModel
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

        sessionManager = NetworkSessionManager(this,offlineNetworkSessionRepository)

        // Start monitoring network changes
        sessionManager.startMonitoring()



        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        setContent {
            val sharedViewModel: SharedViewModel = viewModel()
            val actionAlertDialogData by sharedViewModel.actionAlertDialogData.collectAsState()
            CaptivePortalAnalyzerComposeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController,sessionManager = sessionManager,
                        showToast = { isSuccess: Boolean, message: String? ->
                            showToast(isSuccess, message)
                        },sharedViewModel = sharedViewModel)
                    actionAlertDialogData?.let { ActionAlertDialog(it) }
                }
            }
        }
    }

private fun showToast(
    isSuccess: Boolean,
    message: String? = null
) {
    val finalMessage = when {
        message != null -> message
        isSuccess -> this.getString(R.string.operation_successful)
        else -> this.getString(R.string.operation_failed)
    }

    val title = if (isSuccess) getString(R.string.success) else  getString(R.string.error)
    val toastStyle = if (isSuccess) MotionToastStyle.SUCCESS else MotionToastStyle.ERROR

    // Use MotionToast to display the toast
    MotionToast.createToast(
        this,
        title,
        finalMessage,
        toastStyle,
        MotionToast.GRAVITY_BOTTOM,
        MotionToast.LONG_DURATION,
        ResourcesCompat.getFont(this, R.font.mon_regular)
    )
}
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    CaptivePortalAnalyzerComposeTheme {

    }
}