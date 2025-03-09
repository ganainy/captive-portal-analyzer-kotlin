package com.example.captive_portal_analyzer_kotlin

import NetworkSessionRepository
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.captive_portal_analyzer_kotlin.datastore.settingsDataStore
import com.example.captive_portal_analyzer_kotlin.navigation.AppNavGraph
import com.example.captive_portal_analyzer_kotlin.navigation.AppScaffold
import com.example.captive_portal_analyzer_kotlin.room.AppDatabase
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.example.captive_portal_analyzer_kotlin.utils.NetworkConnectivityObserver
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import com.google.firebase.FirebaseApp
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: NetworkSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val database by lazy { AppDatabase.getDatabase(this) }

        val repository = NetworkSessionRepository(
            sessionDao = database.networkSessionDao(),
            requestDao = database.customWebViewRequestDao(),
            screenshotDao = database.screenshotDao(),
            webpageContentDao = database.webpageContentDao()
        )


        // Start monitoring network changes
        sessionManager = NetworkSessionManager(this, repository)
        sessionManager.startMonitoring()

        //connectivity observer
        val connectivityObserver = NetworkConnectivityObserver(this)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        setContent {
            val sharedViewModel: SharedViewModel =
                viewModel(factory = SharedViewModelFactory(connectivityObserver, settingsDataStore))
            // Update app locale whenever language changes
            val currentLocale by sharedViewModel.currentLocale.collectAsState()
            LaunchedEffect(currentLocale) {
                updateLocale(currentLocale)
            }

            val navController = rememberNavController()
            val dialogState by sharedViewModel.dialogState.collectAsState()
            val themeMode by sharedViewModel.themeMode.collectAsState()
            val scope = rememberCoroutineScope()



            // Collect theme preferences
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }


            AppTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppScaffold(
                        navController = navController,
                    ) {
                        AppNavGraph(
                            navController = navController,
                            sessionManager = sessionManager,
                            repository = repository,
                            sharedViewModel = sharedViewModel,
                            dialogState = dialogState,
                            themeMode = themeMode,
                            currentLanguage = currentLocale.toLanguageTag(),
                            onThemeChanged = sharedViewModel::updateThemeMode,
                            onLocalChanged = sharedViewModel::updateLocale,
                        )
                    }
                }
            }

        }
    }


    private fun updateLocale(locale: Locale) {
        if (locale != Locale.getDefault()) {
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            // Recreate the activity to apply changes immediately
            recreate()
        }
    }

}


@Preview(showBackground = true)
@Composable
fun MainPreview() {
    AppTheme {

    }
}