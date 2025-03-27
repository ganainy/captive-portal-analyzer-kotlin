
// import android.content.Context // Context no longer needed directly for intent creation here
import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import com.example.captive_portal_analyzer_kotlin.PcapdroidConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Enum remains the same
enum class CaptureState {
    IDLE, STARTING, RUNNING, STOPPING, STOPPED, FILE_READY, ERROR
}

class CaptureViewModel : ViewModel() {

    // --- StateFlow ---
    private val _captureState = MutableStateFlow(CaptureState.IDLE)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready to capture to file.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Store the filename we asked PCAPdroid to use
    private val _targetPcapName = MutableStateFlow<String?>(null)
    val targetPcapName: StateFlow<String?> = _targetPcapName.asStateFlow()

    companion object {
        private const val TAG = "CaptureViewModel"
        // Define the filename we'll request
        private const val DEFAULT_PCAP_FILENAME = "captive_portal_analyzer.pcap"
    }

    // --- Intent Creation ---

    private fun createPcapdroidIntent(): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setClassName(PcapdroidConstants.PCAPDROID_PACKAGE, PcapdroidConstants.PCAPDROID_CAPTURE_CTRL_ACTIVITY)
        return intent
    }

    // No longer needs Context directly if package name isn't needed for broadcast receiver
    fun createStartIntent(): Intent {
        val filename = DEFAULT_PCAP_FILENAME // Or make dynamic if needed
        _targetPcapName.value = filename // Store the name we're using

        val intent = createPcapdroidIntent()
        intent.putExtra(PcapdroidConstants.EXTRA_ACTION, PcapdroidConstants.ACTION_START)
        // *** Use pcap_file mode ***
        intent.putExtra(PcapdroidConstants.EXTRA_PCAP_DUMP_MODE, "pcap_file")
        // *** Specify the filename within Download/PCAPdroid/ ***
        intent.putExtra("pcap_name", filename) // Use the 'pcap_name' extra
        // Filter for own app still useful
        // intent.putExtra(PcapdroidConstants.EXTRA_APP_FILTER, context.packageName) // Need context if filtering
        // dump_extensions might still be useful if PCAPdroid adds metadata even in file mode
        intent.putExtra(PcapdroidConstants.EXTRA_DUMP_EXTENSIONS, true)

        Log.d(TAG, "Creating start intent for pcap_file mode, filename: $filename")
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
        if (_captureState.value == CaptureState.IDLE || _captureState.value == CaptureState.STOPPED || _captureState.value == CaptureState.ERROR || _captureState.value == CaptureState.FILE_READY) {
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
            _statusMessage.value = "PCAPdroid capturing to file: ${_targetPcapName.value ?: "Unknown"}"
            // Ensure the filename is set if the start was successful
            if (_targetPcapName.value == null) { _targetPcapName.value = DEFAULT_PCAP_FILENAME } // Fallback
        } else {
            _captureState.value = CaptureState.ERROR
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
            _statusMessage.value = "Capture stopped. File '${targetFile ?: "Unknown"}' should be ready. $stats"
        } else {
            // Stop command failed, but maybe it was already stopped? Treat as stopped.
            _captureState.value = CaptureState.STOPPED // Or ERROR? Let's use STOPPED.
            _statusMessage.value = "Capture stop command sent (Result: ${result.resultCode}). File state unknown."
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
            val pcapVersion = data.getStringExtra(PcapdroidConstants.RESULT_EXTRA_VERSION_NAME) ?: "N/A"

            val currentState = _captureState.value
            val reportedState = if (isRunning) CaptureState.RUNNING else CaptureState.STOPPED

            _statusMessage.value = "Status: ${reportedState.name}. PCAPdroid v$pcapVersion. $stats"

            // Update state more carefully based on status report
            if (reportedState == CaptureState.RUNNING && (currentState == CaptureState.STOPPED || currentState == CaptureState.IDLE || currentState == CaptureState.FILE_READY || currentState == CaptureState.ERROR)) {
                _captureState.value = CaptureState.RUNNING // Correct state if it was wrong
                _statusMessage.value += " (State corrected to RUNNING)"
            } else if (reportedState == CaptureState.STOPPED && currentState == CaptureState.RUNNING) {
                _captureState.value = CaptureState.FILE_READY // Assume file is ready if status says stopped
                _statusMessage.value += " (State corrected to FILE_READY)"
            }
            // Don't overwrite STARTING or STOPPING

        } else {
            _statusMessage.value = "Failed to get status (Result: ${result.resultCode})."
            Log.e(TAG, "PCAPdroid get_status failed. Result code: ${result.resultCode}")
        }
    }

    fun handleActivityNotFound() {
        _captureState.value = CaptureState.ERROR
        _statusMessage.value = "Error: PCAPdroid app not found."
        Log.e(TAG, "PCAPdroid ActivityNotFoundException")
        _targetPcapName.value = null
    }

    fun handleCaptureStoppedExternally() {
        if (_captureState.value == CaptureState.RUNNING || _captureState.value == CaptureState.STARTING) {
            Log.i(TAG, "Capture stopped externally.")
            // Assume file is ready if stopped externally while running
            _captureState.value = CaptureState.FILE_READY
            _statusMessage.value = "Capture stopped externally. File '${_targetPcapName.value ?: "Unknown"}' may be ready."
        }
    }

    // Function called when UI indicates file processing is done or user navigated away
    fun fileProcessingDone() {
        if (_captureState.value == CaptureState.FILE_READY) {
            _captureState.value = CaptureState.STOPPED // Move from FILE_READY to general STOPPED
        }
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
}