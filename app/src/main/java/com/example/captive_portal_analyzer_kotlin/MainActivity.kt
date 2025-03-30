package com.example.captive_portal_analyzer_kotlin

import NetworkSessionRepository
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.captive_portal_analyzer_kotlin.datastore.settingsDataStore
import com.example.captive_portal_analyzer_kotlin.navigation.AppNavGraph
import com.example.captive_portal_analyzer_kotlin.navigation.AppScaffold
import com.example.captive_portal_analyzer_kotlin.room.AppDatabase
import com.example.captive_portal_analyzer_kotlin.theme.AppTheme
import com.example.captive_portal_analyzer_kotlin.utils.NetworkConnectivityObserver
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: NetworkSessionManager
    private lateinit var mainViewModel: MainViewModel




    // Launchers for PCAPdroid control
    private lateinit var startCaptureLauncher: ActivityResultLauncher<Intent>
    private lateinit var stopCaptureLauncher: ActivityResultLauncher<Intent>
    private lateinit var getStatusLauncher: ActivityResultLauncher<Intent>

    // *** Launcher for SAF File Picker ***
    private lateinit var openPcapFileLauncher: ActivityResultLauncher<Array<String>>


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
        setupSafLauncher() // Setup the SAF launcher
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


        // Initialize Firebase
        FirebaseApp.initializeApp(this)


        setContent {

            //connectivity observer
            val connectivityObserver = NetworkConnectivityObserver(this)

            // Main ViewModel for communicating between composable screens and the main activity if needed
         mainViewModel = MainViewModel(connectivityObserver, settingsDataStore)

            // Update app locale whenever language changes
            val currentLocale by mainViewModel.currentLocale.collectAsState()
            LaunchedEffect(currentLocale) {
                updateLocale(currentLocale)
            }

            val navController = rememberNavController()
            val dialogState by mainViewModel.dialogState.collectAsState()
            val themeMode by mainViewModel.themeMode.collectAsState()
            val skipSetup by mainViewModel.skipSetup.collectAsState()


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
                        content = {
                            AppNavGraph(
                                navController = navController,
                                sessionManager = sessionManager,
                                repository = repository,
                                mainViewModel = mainViewModel,
                                dialogState = dialogState,
                                themeMode = themeMode,
                                currentLanguage = currentLocale.toLanguageTag(),
                                onThemeChanged = mainViewModel::updateThemeMode,
                                onLocalChanged = mainViewModel::updateLocale,
                                onStartIntentLaunchRequested = onStartIntentLaunchRequested,
                                onStopIntentLaunchRequested = onStopIntentLaunchRequested,
                                onStatusIntentLaunchRequested = onStatusIntentLaunchRequested,
                                onOpenFileRequested = ::handleOpenFileRequest,
                                skipSetup = skipSetup, //if PCAPDroid setup screen should be skipped
                            )
                        }
                    )
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
            mainViewModel.handleStartResult(result)
        }
        stopCaptureLauncher = registerForActivityResult(contract) { result ->
            mainViewModel.handleStopResult(result)
        }
        getStatusLauncher = registerForActivityResult(contract) { result ->
            mainViewModel.handleGetStatusResult(result)
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
            mainViewModel.handleActivityNotFound() // Notify ViewModel
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
                        mainViewModel.handleCaptureStoppedExternally() // Notify VM
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

    // --- SAF Setup and Handling ---
    private fun setupSafLauncher() {
        openPcapFileLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                if (uri != null) {
                    Log.i(TAG, "User selected file URI: $uri")
                    // Launch a coroutine to handle the suspend function
                    lifecycleScope.launch {
                        val copiedUri = processSelectedPcapFile(uri)
                        if (copiedUri != null) {
                            // the .pcap file from PCAPdroid is copied to our app storage
                            mainViewModel.setCopiedPcapFileUri(copiedUri) // Treat as done with file ready state
                            Log.i(TAG, "File successfully copied to: $copiedUri")
                            // Handle the copied URI as needed
                        } else {
                            Log.w(TAG, "File copy operation failed")
                        }
                    }
                } else {
                    Log.w(TAG, "User cancelled file selection.")
                    showToast("File selection cancelled.")
                    mainViewModel.fileProcessingDone() // Treat cancellation as done with file ready state
                }
            }
    }

    // Called from AnalysisScreen via the callback
    private fun handleOpenFileRequest(expectedFileName: String) {
        Log.d(TAG, "Request to open file: $expectedFileName")
        // Use SAF to let the user pick the file.
        // We suggest the MIME type for PCAP files.
        // ACTION_OPEN_DOCUMENT lets the user pick, grants persistent read access.
        try {
            // You could try suggesting a starting directory, but it's complex and
            // often doesn't work reliably across Android versions/OEMs for Downloads.
            // Let the user navigate.
            // val downloadsDirUri = DocumentsContract.buildDocumentUri(
            //    "com.android.providers.downloads.documents", "downloads")
            // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, downloadsDirUri) // Often ignored

            openPcapFileLauncher.launch(
                arrayOf(
                    "application/vnd.tcpdump.pcap",
                    "*/*"
                )
            ) // PCAP MIME type + fallback

        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No file picker found.", e)
            showToast("Cannot open file picker. No suitable application found.")
            mainViewModel.fileProcessingDone() // Cannot proceed
        } catch (e: Exception) {
            Log.e(TAG, "Error launching file picker.", e)
            showToast("Error opening file picker.")
            mainViewModel.fileProcessingDone() // Cannot proceed
        }
    }

    // --- Process the URI obtained from SAF ---
    private suspend fun processSelectedPcapFile(sourceFileUri: Uri): Uri? {
        // Get source filename for display/logging
        val sourceFileName =
            DocumentFile.fromSingleUri(this, sourceFileUri)?.name ?: "unknown_capture.pcap"
        //showToast("Copying '$sourceFileName' to app storage...")

        return withContext(Dispatchers.IO) {
            var destinationFile: File? = null
            var bytesCopied: Long = 0
            try {
                // 1. Define Destination in App's External Storage
                val appStorageDir =
                    getExternalFilesDir(null) // -> Android/data/your.package.name/files/
                if (appStorageDir == null) {
                    throw IOException("Cannot access app external storage directory.")
                }
                // Ensure the directory exists (usually does, but good practice)
                if (!appStorageDir.exists()) {
                    appStorageDir.mkdirs()
                }
                destinationFile = File(
                    appStorageDir,
                    "copied_${sourceFileName}"
                ) // Add prefix or use original name

                Log.d(TAG, "Source URI: $sourceFileUri")
                Log.d(TAG, "Destination File: ${destinationFile.absolutePath}")

                // 2. Open Streams using 'use' for automatic closing
                contentResolver.openInputStream(sourceFileUri)?.use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        // 3. Copy Data
                        bytesCopied =
                            inputStream.copyTo(outputStream) // Efficient Kotlin copy function
                    }
                } ?: run {
                    // Handle case where InputStream couldn't be opened
                    throw IOException("Could not open InputStream for source URI.")
                }

                // 4. Success: Report and maybe process the copied file
                Log.i(
                    TAG,
                    "Successfully copied $bytesCopied bytes to ${destinationFile.absolutePath}"
                )
                withContext(Dispatchers.Main) {
                    //showToast("Copied '$sourceFileName' (${bytesCopied / 1024} KB) to app storage.")
                    // Now you can work with 'destinationFile' within your app's storage
                    // Example: processCopiedFileInternally(destinationFile)
                }

                // Return the URI of the copied file
                Uri.fromFile(destinationFile)

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error copying file URI: $sourceFileUri to ${destinationFile?.absolutePath}",
                    e
                )
                withContext(Dispatchers.Main) { showToast("Error copying file: ${e.message}") }
                // Clean up partially copied file if it exists on error
                destinationFile?.delete()
                null // Return null on failure
            } finally {
                // Optionally update ViewModel state if needed after copy attempt
                // viewModel.fileProcessingDone() // Or similar signal
            }
        }
    }

    //todo maybe find alibrary to show the parsed .pcap file in app like in the pcapdroid app
    // Optional: function to process the file now that it's in app storage
    private fun processCopiedFileInternally(localFile: File) {
        Log.d(TAG, "Processing the copied file: ${localFile.absolutePath}")
        // Add your PCAP parsing logic here, reading directly from 'localFile'
        // This can now use standard File APIs
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

    companion object {
        private const val TAG = "MainActivity" // Consistent tag
    }

}


@Preview(showBackground = true)
@Composable
fun MainPreview() {
    AppTheme {

    }
}