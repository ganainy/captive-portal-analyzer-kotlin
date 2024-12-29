package com.example.captive_portal_analyzer_kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.captive_portal_analyzer_kotlin.navigation.AppNavGraph
import com.example.captive_portal_analyzer_kotlin.theme.CaptivePortalAnalyzerComposeTheme
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.util.Date

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: NetworkSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager = NetworkSessionManager(this)

        // Start monitoring network changes
        sessionManager.startMonitoring()

        //for Debugging : Observe session changes
        lifecycleScope.launch {
            sessionManager.getSessionInfo().collect { sessionInfo ->
                sessionInfo?.let {
                    println("Current Session ID: ${it.sessionId}")
                    println("Network SSID: ${it.ssid}")
                    println("Connected at: ${Date(it.timestamp)}")
                }
            }
        }

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        setContent {
            CaptivePortalAnalyzerComposeTheme {
                Surface() {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController,sessionManager = sessionManager,
                        showToast = { isSuccess: Boolean, message: String? ->
                            showToast(isSuccess, message)
                        },)
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

    val title = if (isSuccess) "Success" else "Error"
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