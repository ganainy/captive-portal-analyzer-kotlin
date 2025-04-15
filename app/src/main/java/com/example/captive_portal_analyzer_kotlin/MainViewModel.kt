package com.example.captive_portal_analyzer_kotlin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.components.DialogState
import com.example.captive_portal_analyzer_kotlin.components.ToastState
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import com.example.captive_portal_analyzer_kotlin.utils.NetworkConnectivityObserver
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale


enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class PcapDroidPacketCaptureStatus {
    INITIAL,   // user still didnt choose to go with packet capture enabled or not
    ENABLED,
    DISABLED
}

interface IMainViewModel {
    val clickedSessionId: StateFlow<String?>
    val isConnected: StateFlow<Boolean>

    fun updateClickedSessionId(clickedSessionId: String?)
    fun updateLocale(locale: Locale)
    fun updateThemeMode(mode: ThemeMode)
    fun showDialog(
        title: String,
        message: String,
        confirmText: String = "OK",
        dismissText: String = "Cancel",
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    )

    fun hideDialog()
    fun showToast(message: String, style: ToastStyle)
    fun hideToast()
    fun updateClickedContent(webpageContentEntity: WebpageContentEntity)
    fun updateClickedRequest(webViewRequestEntity: CustomWebViewRequestEntity)
}


class MainViewModel(
    application: Application,
    private val sessionManager: NetworkSessionManager,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val dataStore: DataStore<Preferences>,
) : AndroidViewModel(application), IMainViewModel {

    /*show action alert dialogs from anywhere in the app*/
    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState = _dialogState.asStateFlow()

    private val _clickedSessionId = MutableStateFlow<String?>(null) // stores the id of the session

    // clicked in the sessionList screen to be passed to Session screen
    override val clickedSessionId: StateFlow<String?> = _clickedSessionId

    private val _clickedWebViewRequestEntity = MutableStateFlow<CustomWebViewRequestEntity?>(null)
    val clickedWebViewRequestEntity: StateFlow<CustomWebViewRequestEntity?> =
        _clickedWebViewRequestEntity

    private val _toastState = MutableStateFlow<ToastState>(ToastState.Hidden)
    val toastState = _toastState.asStateFlow()

    // Skip Setup Screen preference
    private val SKIP_SETUP_KEY = booleanPreferencesKey("skip_setup")
    private val _skipSetup = MutableStateFlow(false)
    val skipSetup: StateFlow<Boolean> get() = _skipSetup

    override fun updateClickedSessionId(clickedSessionId: String?) {
        _clickedSessionId.value = clickedSessionId
    }

    //is device connected to the internet
    private val _isConnected = MutableStateFlow(true)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    //settings related to app language

    private val languageKey = stringPreferencesKey("app_language") // Keys for DataStore
    private val _currentLocale = MutableStateFlow<Locale>(Locale("en"))
    val currentLocale: StateFlow<Locale> get() = _currentLocale


    //settings related to dark/light mode

    private val themeModeKey = stringPreferencesKey("theme_mode")  // Keys for DataStore
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    //the clicked webpage content to show in the WebpageContent screen
    private val _clickedWebpageContent = MutableStateFlow<WebpageContentEntity?>(null)
    val clickedWebpageContent: StateFlow<WebpageContentEntity?> =
        _clickedWebpageContent.asStateFlow()


    companion object {
        private const val TAG = "MainViewModel"

        // This file name will be used by PCAPDroid to use for the .pcap file
        private const val DEFAULT_PCAP_FILENAME = "captive_portal_analyzer.pcap"
    }

    init {
        getLocalePreference()
        getThemePreference()
        getSkipSetupScreenPreference()
        viewModelScope.launch {
            connectivityObserver.observe().collect { isConnected ->
                _isConnected.value = isConnected
            }
        }
    }


    private fun getLocalePreference() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.data
                    .map { preferences ->
                        Locale.forLanguageTag(
                            preferences[languageKey] ?: Locale.getDefault().language
                        )
                    }
                    .catch { e ->
                        Log.e("SharedViewModel", "Error reading locale preference", e)
                        emit(Locale.getDefault())
                    }
                    .firstOrNull()?.let {
                        _currentLocale.value = it
                    }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Unexpected error in getLocalePreference", e)
            }
        }
    }


    override fun updateLocale(locale: Locale) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[languageKey] = locale.toLanguageTag()
            }
            _currentLocale.value = locale
        }
    }


    private fun getThemePreference() {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { preferences ->
                    ThemeMode.valueOf(preferences[themeModeKey] ?: ThemeMode.SYSTEM.name)
                }
                .catch { e ->
                    Log.e("SharedViewModel", "Error reading theme preference", e)
                    emit(ThemeMode.SYSTEM)
                }
                .firstOrNull()?.let {
                    _themeMode.value = it
                }
        }
    }

    override fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[themeModeKey] = mode.name
            }
            _themeMode.value = mode
        }
    }


    override fun showDialog(
        title: String,
        message: String,
        confirmText: String,
        dismissText: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        _dialogState.value = DialogState.Shown(
            title = title,
            message = message,
            confirmText = confirmText,
            dismissText = dismissText,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }

    override fun hideDialog() {
        _dialogState.value = DialogState.Hidden
    }


    override fun showToast(
        message: String,
        style: ToastStyle,
    ) {
        _toastState.value = ToastState.Shown("", message, style, 2000L)
    }


    override fun hideToast() {
        _toastState.value = ToastState.Hidden
    }


    /**
     * Updates the clicked content to show in the WebpageContent screen.
     * @param webpageContentEntity the webpage content entity to be updated.
     */
    override fun updateClickedContent(webpageContentEntity: WebpageContentEntity) {
        _clickedWebpageContent.value = webpageContentEntity
    }

    override fun updateClickedRequest(webViewRequestEntity: CustomWebViewRequestEntity) {
        _clickedWebViewRequestEntity.value = webViewRequestEntity
    }


    /**<<<<<<<<<<<<<<<   Capture packets related functions   >>>>>>>>>>>>>>**/

    enum class CaptureState {
        IDLE, STARTING, RUNNING, STOPPING, STOPPED, FILE_READY,
        WRONG_FILE_PICKED, ERROR // user picked file other than the captive_portal_analyzer.pcap
    }


    // --- StateFlow ---
    private val _captureState = MutableStateFlow(CaptureState.IDLE)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private val defaultStatusMessage = "Ready to capture to file."
    private val _statusMessage = MutableStateFlow(defaultStatusMessage)
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Store the filename we asked PCAPdroid to use
    private val _targetPcapName = MutableStateFlow<String?>(null)
    val targetPcapName: StateFlow<String?> = _targetPcapName.asStateFlow()

    // The URI of the copied PCAP file (due to Android storage restrictions we can't access the original directly so we do a workaround and make a copy with help of SAF)
    private val _copiedPcapFileUri = MutableStateFlow<Uri?>(null)
    val copiedPcapFileUri: StateFlow<Uri?> = _copiedPcapFileUri.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow<Int>(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private val _pcapDroidPacketCaptureStatus =
        MutableStateFlow<PcapDroidPacketCaptureStatus>(PcapDroidPacketCaptureStatus.INITIAL)
    val pcapDroidPacketCaptureStatus: StateFlow<PcapDroidPacketCaptureStatus> =
        _pcapDroidPacketCaptureStatus.asStateFlow()

    // --- Intent Creation ---

    private fun createPcapdroidIntent(): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setClassName(
            PcapdroidConstants.PCAPDROID_PACKAGE,
            PcapdroidConstants.PCAPDROID_CAPTURE_CTRL_ACTIVITY
        )
        return intent
    }

    // No longer needs Context directly if package name isn't needed for broadcast receiver
    fun createStartIntent(): Intent {

        // Get application context from AndroidViewModel
        val context = getApplication<Application>().applicationContext
        val ownPackageName = context.packageName

        val filename = DEFAULT_PCAP_FILENAME // Or make dynamic if needed
        _targetPcapName.value = filename // Store the name we're using

        // *** Define packages to filter ***
        val filterPackages = listOf(
            ownPackageName,                   // this app
            "com.android.captiveportallogin", // Standard Android captive portal handler
        ).joinToString(",")


        val intent = createPcapdroidIntent()
        intent.putExtra(PcapdroidConstants.EXTRA_ACTION, PcapdroidConstants.ACTION_START)
        // *** Use pcap_file mode ***
        intent.putExtra(PcapdroidConstants.EXTRA_PCAP_DUMP_MODE, "pcap_file")
        // *** Specify the filename within Download/PCAPdroid/ ***
        intent.putExtra("pcap_name", filename) // Use the 'pcap_name' extra
        // *** ADD THE APP FILTER ***
        intent.putExtra(PcapdroidConstants.EXTRA_APP_FILTER, filterPackages)
        // dump_extensions might still be useful if PCAPdroid adds metadata even in file mode
        intent.putExtra(PcapdroidConstants.EXTRA_DUMP_EXTENSIONS, true)

        Log.d(
            TAG,
            "Creating start intent for pcap_file mode, filename: $filename, filter: $filterPackages"
        )
        return intent
    }

    fun createStopIntent(): Intent {
        val intent = createPcapdroidIntent()
        intent.putExtra(PcapdroidConstants.EXTRA_ACTION, PcapdroidConstants.ACTION_STOP)
        return intent
    }

    fun createGetStatusIntent(): Intent {
        val intent = createPcapdroidIntent()
        intent.putExtra(PcapdroidConstants.EXTRA_ACTION, PcapdroidConstants.ACTION_GET_STATUS)
        return intent
    }

    // --- Action Initiation ---

    fun requestStartCapture() {
        if (_captureState.value == CaptureState.IDLE || _captureState.value == CaptureState.STOPPED || _captureState.value == CaptureState.WRONG_FILE_PICKED || _captureState.value == CaptureState.FILE_READY) {
            _captureState.value = CaptureState.STARTING
            _statusMessage.value = "Requesting file capture start..."
            // Reset filename? Or keep the last one? Let's keep it until next successful start.
            // _targetPcapName.value = null // Reset if needed
        } else {
            Log.w(TAG, "Start request ignored, state is ${_captureState.value}")
        }
    }

    fun requestStopCapture() {
        if (_captureState.value == CaptureState.RUNNING || _captureState.value == CaptureState.STARTING) {
            _captureState.value = CaptureState.STOPPING
            _statusMessage.value = "Requesting capture stop..."
        } else {
            Log.w(TAG, "Stop request ignored, state is ${_captureState.value}")
        }
    }

    fun requestGetStatus() {
        _statusMessage.value = "Requesting capture status..."
    }

    // --- Result Handling ---

    fun handleStartResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            _captureState.value = CaptureState.RUNNING
            _statusMessage.value =
                "PCAPdroid capturing to file: ${_targetPcapName.value ?: "Unknown"}"
            // Ensure the filename is set if the start was successful
            if (_targetPcapName.value == null) {
                _targetPcapName.value = DEFAULT_PCAP_FILENAME
            } // Fallback
        } else {
            _captureState.value = CaptureState.WRONG_FILE_PICKED
            _statusMessage.value = "Failed to start file capture (Result: ${result.resultCode})."
            _targetPcapName.value = null // Reset filename on failure
            Log.e(TAG, "PCAPdroid start failed. Result code: ${result.resultCode}")
        }
    }

    fun handleStopResult(result: ActivityResult) {
        val targetFile = _targetPcapName.value
        if (result.resultCode == Activity.RESULT_OK) {
            // Capture stopped successfully, file *should* be ready.
            _captureState.value = CaptureState.FILE_READY
            val stats = extractStatsFromResult(result.data)
            _statusMessage.value =
                "Capture stopped. File '${targetFile ?: "Unknown"}' should be ready. $stats"
        } else {
            // Stop command failed, but maybe it was already stopped? Treat as stopped.
            _captureState.value = CaptureState.STOPPED // Or ERROR? Let's use STOPPED.
            _statusMessage.value =
                "Capture stop command sent (Result: ${result.resultCode}). File state unknown."
            Log.w(TAG, "PCAPdroid stop potentially failed. Result code: ${result.resultCode}")
            // Keep _targetPcapName, the file *might* exist.
        }
    }

    fun handleGetStatusResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val isRunning = data.getBooleanExtra(PcapdroidConstants.RESULT_EXTRA_RUNNING, false)
            // ... (version logging etc. same as before)
            val stats = extractStatsFromResult(data)
            val pcapVersion =
                data.getStringExtra(PcapdroidConstants.RESULT_EXTRA_VERSION_NAME) ?: "N/A"

            val currentState = _captureState.value
            val reportedState = if (isRunning) CaptureState.RUNNING else CaptureState.STOPPED

            _statusMessage.value = "Status: ${reportedState.name}. PCAPdroid v$pcapVersion. $stats"

            // Update state more carefully based on status report
            if (reportedState == CaptureState.RUNNING && (currentState == CaptureState.STOPPED || currentState == CaptureState.IDLE || currentState == CaptureState.FILE_READY || currentState == CaptureState.WRONG_FILE_PICKED)) {
                _captureState.value = CaptureState.RUNNING // Correct state if it was wrong
                _statusMessage.value += " (State corrected to RUNNING)"
            } else if (reportedState == CaptureState.STOPPED && currentState == CaptureState.RUNNING) {
                _captureState.value =
                    CaptureState.FILE_READY // Assume file is ready if status says stopped
                _statusMessage.value += " (State corrected to FILE_READY)"
            }
            // Don't overwrite STARTING or STOPPING

        } else {
            _statusMessage.value = "Failed to get status (Result: ${result.resultCode})."
            Log.e(TAG, "PCAPdroid get_status failed. Result code: ${result.resultCode}")
        }
    }

    fun handleActivityNotFound() {
        _captureState.value = CaptureState.WRONG_FILE_PICKED
        _statusMessage.value = "Error: PCAPdroid app not found."
        Log.e(TAG, "PCAPdroid ActivityNotFoundException")
        _targetPcapName.value = null
    }

    fun handleCaptureStoppedExternally() {
        if (_captureState.value == CaptureState.RUNNING || _captureState.value == CaptureState.STARTING) {
            Log.i(TAG, "Capture stopped externally.")
            // Assume file is ready if stopped externally while running
            _captureState.value = CaptureState.FILE_READY
            _statusMessage.value =
                "Capture stopped externally. File '${_targetPcapName.value ?: "Unknown"}' may be ready."
        }
    }

    // Function called when UI indicates file processing is done or user navigated away
    fun fileProcessingDone() {
        if (_captureState.value == CaptureState.FILE_READY) {
            _captureState.value = CaptureState.STOPPED // Move from FILE_READY to general STOPPED
        }
    }

    fun updateCopiedPcapFileUri(uri: Uri?) {
        _copiedPcapFileUri.value = uri
    }

    fun setSelectedTabIndex(index: Int) {
        _selectedTabIndex.value = index
    }

    fun updatePcapDroidPacketCaptureStatus(pcapDroidPacketCaptureStatus: PcapDroidPacketCaptureStatus) {
        _pcapDroidPacketCaptureStatus.value = pcapDroidPacketCaptureStatus
    }

    /**
     * Resets the state variables related to PCAPdroid packet capture
     * back to their initial default values.
     */
    fun resetPacketCaptureState() {
        _captureState.value = CaptureState.IDLE
        _statusMessage.value = defaultStatusMessage
        _targetPcapName.value = null
        _copiedPcapFileUri.value = null
        _pcapDroidPacketCaptureStatus.value = PcapDroidPacketCaptureStatus.INITIAL
        _selectedTabIndex.value = 0 // Reset if applicable
        Log.d(TAG, "Packet capture related states reset to default.")
    }

    private fun extractStatsFromResult(data: Intent?): String {
        // If capturing to file, bytes_dumped might be relevant
        if (data == null) return "No stats data."
        val bytesDumped = data.getLongExtra("bytes_dumped", -1)
        // Add others if needed (pkts_sent etc)
        return "Stats: [Dumped: $bytesDumped B]"
    }


    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared.")
    }

    private fun getSkipSetupScreenPreference() {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { preferences ->
                    preferences[SKIP_SETUP_KEY] ?: false
                }
                .catch { e ->
                    Log.e(TAG, "Error reading skip setup preference", e)
                    emit(false)
                }
                .collect { value ->
                    _skipSetup.value = value
                }
        }
    }

    /**
     *  Handles the result from the file picker (SAF).
     *  Copies the selected file to internal storage with a unique name based on session ID.
     *  Updates the ViewModel state (_copiedPcapFileUri, _captureState).
     *
     *  @param sourceUri The Uri returned by the file picker. Null if cancelled.
     *  @param context The application context.
     */
    fun handleSelectedPcapFile(sourceUri: Uri?, context: Context) {
        if (sourceUri == null) {
            Log.d(TAG, "File selection cancelled by user.")
            // Optionally update status or show toast
            return
        }

        // Perform copy on background thread
        viewModelScope.launch(Dispatchers.IO) {
            // *** Get current session ID using NetworkSessionManager ***
            val currentSessionId =
                sessionManager.getCurrentSessionId() // <-- Get current active session ID

            if (currentSessionId == null) {
                Log.e(TAG, "Cannot copy PCAP, session ID is null when handling selection.")
                // Use showToast defined in ViewModel
                withContext(Dispatchers.Main) {
                    showToast("Error: Cannot save PCAP, session not active.", ToastStyle.ERROR)
                }
                // Update state on Main thread
                withContext(Dispatchers.Main) {
                    _captureState.value = CaptureState.ERROR
                }
                return@launch // Exit the coroutine launch block
            }

            // Verify if the selected filename matches the target filename
            val targetName = targetPcapName.value
            // Get filename from Uri (can be complex, this is a basic attempt)
            val selectedFilename =
                sourceUri.lastPathSegment?.substringAfterLast('/') ?: sourceUri.toString()

            if (targetName != null && !selectedFilename.endsWith(targetName)) {
                Log.w(TAG, "Wrong file selected. Expected: $targetName, Got: $selectedFilename")
                // Update state on Main thread
                withContext(Dispatchers.Main) {
                    _captureState.value = CaptureState.WRONG_FILE_PICKED
                    _statusMessage.value = "Wrong file selected. Please select '$targetName'."
                    showToast("Wrong file selected. Please select '$targetName'.", ToastStyle.ERROR)
                    _copiedPcapFileUri.value = null // Clear any previous selection
                }
                return@launch // Exit the coroutine launch block
            } else {
                Log.d(TAG, "Correct file selected: $sourceUri (Filename: $selectedFilename)")
                // Proceed with copying
            }

            // **Generate unique destination filename**
            val destinationFilename =
                "session_${currentSessionId}_${System.currentTimeMillis()}.pcap"
            val destinationFile =
                File(context.filesDir, destinationFilename) // Use app's internal filesDir

            try {
                // Use ContentResolver for SAF Uri
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw IOException("Failed to open input stream for $sourceUri")

                Log.i(TAG, "Successfully copied PCAP to: ${destinationFile.absolutePath}")

                // **Update MainViewModel with the URI/Path of the NEW file**
                val copiedFileUri = Uri.fromFile(destinationFile)
                withContext(Dispatchers.Main) {
                    updateCopiedPcapFileUri(copiedFileUri) // Update the state flow
                    _captureState.value = CaptureState.FILE_READY // Set state to ready
                    _statusMessage.value =
                        "File '$targetName' copied successfully." // Update status
                }

            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy PCAP file: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Error copying PCAP file: ${e.message}", ToastStyle.ERROR)
                    _captureState.value = CaptureState.ERROR // Indicate error
                    updateCopiedPcapFileUri(null) // Clear any previous URI on error
                    _statusMessage.value = "Error copying PCAP file."
                }
            }
        }
    }

    fun updateSkipSetupPreference(shouldSkipSetupScreen: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[SKIP_SETUP_KEY] = shouldSkipSetupScreen
            }
            _skipSetup.value = shouldSkipSetupScreen
        }
    }

    fun updateCaptureState(captureState: CaptureState) {
        _captureState.value = captureState
    }

}

class MainViewModelFactory(
    private val application: Application,
    private val sessionManager: NetworkSessionManager,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val dataStore: DataStore<Preferences>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                application,
                sessionManager,
                connectivityObserver,
                dataStore,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


