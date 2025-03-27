package com.example.captive_portal_analyzer_kotlin

import NetworkSessionRepository
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.captive_portal_analyzer_kotlin.datastore.settingsDataStore
import com.example.captive_portal_analyzer_kotlin.navigation.AppNavGraph
import com.example.captive_portal_analyzer_kotlin.navigation.AppScaffold
import com.example.captive_portal_analyzer_kotlin.room.AppDatabase
import com.example.captive_portal_analyzer_kotlin.screens.pcap_setup.CaptureViewModel
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.example.captive_portal_analyzer_kotlin.utils.NetworkConnectivityObserver
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import com.google.firebase.FirebaseApp
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: NetworkSessionManager

    // ViewModel needed to pass results back
    private val captureViewModel: CaptureViewModel by viewModels()

    // ActivityResultLaunchers MUST live here
    private lateinit var startCaptureLauncher: ActivityResultLauncher<Intent>
    private lateinit var stopCaptureLauncher: ActivityResultLauncher<Intent>
    private lateinit var getStatusLauncher: ActivityResultLauncher<Intent>


    val onStartIntentLaunchRequested =
        { intent: Intent -> safeLaunch(startCaptureLauncher, intent) }
    val onStopIntentLaunchRequested = { intent: Intent -> safeLaunch(stopCaptureLauncher, intent) }
    val onStatusIntentLaunchRequested = { intent: Intent -> safeLaunch(getStatusLauncher, intent) }

    //  BroadcastReceiver to receive capture status updates
    private var captureStatusReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupResultLaunchers()
        setupBroadcastReceiver()

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
                            captureViewModel = captureViewModel,
                            onStartIntentLaunchRequested =onStartIntentLaunchRequested,
                            onStopIntentLaunchRequested =onStopIntentLaunchRequested,
                            onStatusIntentLaunchRequested =onStatusIntentLaunchRequested
                        )
                    }
                }
            }

        }
    }


    override fun onResume() {
        super.onResume()
        registerCaptureStatusReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterCaptureStatusReceiver()
    }

    private fun setupResultLaunchers() {
        val contract = ActivityResultContracts.StartActivityForResult()

        // Launchers now directly call the ViewModel's result handlers
        startCaptureLauncher = registerForActivityResult(contract) { result ->
            captureViewModel.handleStartResult(result)
        }
        stopCaptureLauncher = registerForActivityResult(contract) { result ->
            captureViewModel.handleStopResult(result)
        }
        getStatusLauncher = registerForActivityResult(contract) { result ->
            captureViewModel.handleGetStatusResult(result)
        }
    }

    // Helper function to launch intents safely
    private fun safeLaunch(launcher: ActivityResultLauncher<Intent>, intent: Intent) {
        try {
            Log.d(
                "MainActivity",
                "Launching intent: ${intent.action} with extras: ${intent.extras}"
            )
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("MainActivity", "PCAPdroid not found!", e)
            captureViewModel.handleActivityNotFound() // Notify ViewModel
            showToast("PCAPdroid app not found. Please install it.")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to launch intent: ${intent.action}", e)
            showToast("Error launching PCAPdroid action.")
            // Optionally, notify ViewModel of generic launch failure if needed
        }
    }

    // --- Optional: Broadcast Receiver Handling (Identical to previous examples) ---

    private fun setupBroadcastReceiver() {
        captureStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == PcapdroidConstants.BROADCAST_ACTION_STATUS) {
                    val isRunning =
                        intent.getBooleanExtra(PcapdroidConstants.RESULT_EXTRA_RUNNING, true)
                    Log.d("CaptureStatusReceiver", "Received broadcast: running=$isRunning")
                    if (!isRunning) {
                        captureViewModel.handleCaptureStoppedExternally() // Notify VM
                    }
                }
            }
        }
    }

    private fun registerCaptureStatusReceiver() {
        captureStatusReceiver?.let { receiver ->
            val intentFilter = IntentFilter(PcapdroidConstants.BROADCAST_ACTION_STATUS)
            try {
                ContextCompat.registerReceiver(
                    this,
                    receiver,
                    intentFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                Log.d("MainActivity", "CaptureStatusReceiver registered")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error registering receiver", e)
            }
        }
    }

    private fun unregisterCaptureStatusReceiver() {
        captureStatusReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
                Log.d("MainActivity", "CaptureStatusReceiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w("MainActivity", "Receiver not registered or already unregistered.")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error unregistering receiver", e)
            }
        }
    }

    // --- Utility ---
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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