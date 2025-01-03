package com.example.captive_portal_analyzer_kotlin

import NetworkSessionRepository
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.captive_portal_analyzer_kotlin.components.AppScaffold
import com.example.captive_portal_analyzer_kotlin.navigation.AppNavGraph
import com.example.captive_portal_analyzer_kotlin.room.AppDatabase
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: NetworkSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val database by lazy { AppDatabase.getDatabase(this) }

        val repository = NetworkSessionRepository(
            sessionDao = database.networkSessionDao(),
            requestDao =database.customWebViewRequestDao(),
            screenshotDao = database.screenshotDao(),
            webpageContentDao =  database.webpageContentDao()
        )


        // Start monitoring network changes
        sessionManager = NetworkSessionManager(this, repository)
        sessionManager.startMonitoring()


        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        setContent {
            val navController = rememberNavController()
            val sharedViewModel: SharedViewModel = viewModel()
            val dialogState by sharedViewModel.dialogState.collectAsState()
            val scope = rememberCoroutineScope()

            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppScaffold(
                        navController = navController,
                        scope = scope
                    ) {
                    AppNavGraph(
                        navController = navController,
                        sessionManager = sessionManager,
                        repository = repository,
                        sharedViewModel = sharedViewModel,
                        dialogState = dialogState
                    )
                }
                }
            }
        }
    }


}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    AppTheme {

    }
}