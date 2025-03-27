package com.example.captive_portal_analyzer_kotlin.screens.pcap_setup


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.captive_portal_analyzer_kotlin.PcapdroidConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.BindException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

// Enum to represent capture state (remains the same)
enum class CaptureState {
    IDLE, STARTING, RUNNING, STOPPING, STOPPED, ERROR
}

class CaptureViewModel : ViewModel() {

    // --- StateFlow for UI State ---
    private val _captureState = MutableStateFlow(CaptureState.IDLE)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Initializing...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _receivedPacketCount = MutableStateFlow(0L)
    val receivedPacketCount: StateFlow<Long> = _receivedPacketCount.asStateFlow()

    // --- Internal State ---
    private var udpListenerJob: Job? = null
    private var udpSocket: DatagramSocket? = null

    companion object {
        private const val TAG = "CaptureViewModel"
        private const val UDP_BUFFER_SIZE = 65535
    }

    // --- Intent Creation (Requires Context for package name) ---

    private fun createPcapdroidIntent(): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setClassName(PcapdroidConstants.PCAPDROID_PACKAGE, PcapdroidConstants.PCAPDROID_CAPTURE_CTRL_ACTIVITY)
        return intent
    }

    // Now requires Context
    fun createStartIntent(context: Context): Intent {
        val intent = createPcapdroidIntent()
        intent.putExtra(PcapdroidConstants.EXTRA_ACTION, PcapdroidConstants.ACTION_START)
        intent.putExtra(PcapdroidConstants.EXTRA_PCAP_DUMP_MODE, PcapdroidConstants.DUMP_MODE_UDP_EXPORTER)
        intent.putExtra(PcapdroidConstants.EXTRA_COLLECTOR_IP, PcapdroidConstants.LOCALHOST_IP)
        intent.putExtra(PcapdroidConstants.EXTRA_COLLECTOR_PORT, PcapdroidConstants.UDP_LISTENER_PORT)
        intent.putExtra(PcapdroidConstants.EXTRA_APP_FILTER, context.packageName) // Use context here
        intent.putExtra(PcapdroidConstants.EXTRA_DUMP_EXTENSIONS, true)
        // Optional: BroadcastReceiver - requires fully qualified name if used
        // intent.putExtra(PcapdroidConstants.EXTRA_BROADCAST_RECEIVER, "${context.packageName}.capture.CaptureStatusReceiver")
        return intent
    }

    // These don't need context
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

    // --- Action Initiation (Called by UI/Composable) ---

    fun requestStartCapture() {
        if (_captureState.value == CaptureState.IDLE || _captureState.value == CaptureState.STOPPED || _captureState.value == CaptureState.ERROR) {
            _captureState.value = CaptureState.STARTING
            _statusMessage.value = "Requesting start capture..."
            _receivedPacketCount.value = 0L // Reset count on new start request
            // The actual intent launch must happen in the Activity via callback
        } else {
            Log.w(TAG, "Start request ignored, state is ${_captureState.value}")
        }
    }

    fun requestStopCapture() {
        if (_captureState.value == CaptureState.RUNNING || _captureState.value == CaptureState.STARTING) {
            _captureState.value = CaptureState.STOPPING
            _statusMessage.value = "Requesting stop capture..."
            // The actual intent launch must happen in the Activity via callback
        } else {
            Log.w(TAG, "Stop request ignored, state is ${_captureState.value}")
        }
    }

    fun requestGetStatus() {
        _statusMessage.value = "Requesting capture status..."
        // The actual intent launch must happen in the Activity via callback
    }


    // --- Result Handling (Called from Activity's ActivityResultLauncher) ---

    fun handleStartResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            _captureState.value = CaptureState.RUNNING
            _statusMessage.value = "Capture started by PCAPdroid. Listening..."
            startUdpListener()
        } else {
            _captureState.value = CaptureState.ERROR
            _statusMessage.value = "Failed to start capture (Result: ${result.resultCode}). User denied? PCAPdroid issue?"
            Log.e(TAG, "PCAPdroid start failed. Result code: ${result.resultCode}")
            stopUdpListener()
        }
    }

    fun handleStopResult(result: ActivityResult) {
        stopUdpListener() // Stop listener regardless
        if (result.resultCode == Activity.RESULT_OK) {
            _captureState.value = CaptureState.STOPPED
            val stats = extractStatsFromResult(result.data)
            _statusMessage.value = "Capture stopped by PCAPdroid. $stats"
        } else {
            _captureState.value = CaptureState.STOPPED // Assume stopped even on API error
            _statusMessage.value = "Capture stop command sent (Result: ${result.resultCode}). Assuming stopped."
            Log.w(TAG, "PCAPdroid stop potentially failed. Result code: ${result.resultCode}")
        }
    }

    fun handleGetStatusResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val isRunning = data.getBooleanExtra(PcapdroidConstants.RESULT_EXTRA_RUNNING, false)
            val pcapVersion = data.getStringExtra(PcapdroidConstants.RESULT_EXTRA_VERSION_NAME) ?: "N/A"
            val pcapVersionCode = data.getIntExtra(PcapdroidConstants.RESULT_EXTRA_VERSION_CODE, 0)
            val stats = extractStatsFromResult(data)

            val currentState = if (isRunning) CaptureState.RUNNING else CaptureState.STOPPED
            // Only update state if it has actually changed based on the status check
            // This prevents overwriting STARTING/STOPPING states unnecessarily
            if (_captureState.value != currentState &&
                _captureState.value != CaptureState.STARTING &&
                _captureState.value != CaptureState.STOPPING) {
                _captureState.value = currentState
            }
            _statusMessage.value = "Status: ${currentState.name}. PCAPdroid v$pcapVersion ($pcapVersionCode). $stats"

            // Correct listener state based on reported status
            if (isRunning && !isUdpListenerRunning()) {
                Log.w(TAG, "Status is RUNNING, but listener wasn't. Starting listener.")
                startUdpListener()
            } else if (!isRunning && isUdpListenerRunning()) {
                Log.w(TAG, "Status is STOPPED, but listener was running. Stopping listener.")
                stopUdpListener()
            }

        } else {
            // Avoid changing core state on status check failure, just report message
            _statusMessage.value = "Failed to get status (Result: ${result.resultCode})."
            Log.e(TAG, "PCAPdroid get_status failed. Result code: ${result.resultCode}")
        }
    }

    fun handleActivityNotFound() {
        _captureState.value = CaptureState.ERROR
        _statusMessage.value = "Error: PCAPdroid app not found. Please install it."
        Log.e(TAG, "PCAPdroid ActivityNotFoundException")
        stopUdpListener()
    }

    // Called if BroadcastReceiver notifies of a stop event
    fun handleCaptureStoppedExternally() {
        if (_captureState.value == CaptureState.RUNNING || _captureState.value == CaptureState.STARTING) {
            Log.i(TAG, "Capture stopped externally.")
            _captureState.value = CaptureState.STOPPED
            _statusMessage.value = "Capture stopped externally."
            stopUdpListener()
        }
    }

    // --- UDP Listener (Identical logic, uses viewModelScope) ---

    private fun isUdpListenerRunning(): Boolean {
        return udpListenerJob?.isActive == true && udpSocket?.isBound == true && udpSocket?.isClosed == false
    }

    private fun startUdpListener() {
        if (isUdpListenerRunning()) return
        stopUdpListener()

        udpListenerJob = viewModelScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Attempting to start UDP Listener on port ${PcapdroidConstants.UDP_LISTENER_PORT}")
            try {
                udpSocket = DatagramSocket(PcapdroidConstants.UDP_LISTENER_PORT, InetAddress.getByName("0.0.0.0"))
                Log.i(TAG, "UDP Listener started successfully.")
                // Update status message on the main thread
                withContext(Dispatchers.Main) {
                    if (_captureState.value == CaptureState.RUNNING) { // Only if we are supposed to be running
                        _statusMessage.value = "Capture running. Listening for packets..."
                    }
                }

                val buffer = ByteArray(UDP_BUFFER_SIZE)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    try {
                        udpSocket?.receive(packet) // Blocking
                        val receivedSize = packet.length
                        // Process packet on IO thread if complex, update StateFlow on Main
                        _receivedPacketCount.update { it + 1 } // Thread-safe update
                        Log.v(TAG, "Received UDP packet: size=$receivedSize bytes, Total=${_receivedPacketCount.value}")
                        packet.length = buffer.size // Reset for next receive
                    } catch (e: SocketException) {
                        if (isActive) Log.e(TAG, "SocketException in UDP loop: ${e.message}")
                        break // Exit loop
                    } catch (e: Exception) {
                        if (isActive) Log.e(TAG, "Error receiving UDP packet: ${e.message}", e)
                        // Consider adding a small delay here if errors are persistent but recoverable
                    }
                }
            } catch (e: BindException) {
                Log.e(TAG, "UDP Listener BindException on port ${PcapdroidConstants.UDP_LISTENER_PORT}", e)
                withContext(Dispatchers.Main) {
                    _captureState.value = CaptureState.ERROR
                    _statusMessage.value = "Error: Port ${PcapdroidConstants.UDP_LISTENER_PORT} already in use."
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP Listener failed to start or crashed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    // Only set error if we were supposed to be running
                    if (_captureState.value == CaptureState.RUNNING || _captureState.value == CaptureState.STARTING) {
                        _captureState.value = CaptureState.ERROR
                        _statusMessage.value = "Error in UDP listener."
                    }
                }
            } finally {
                Log.i(TAG, "UDP Listener loop finished.")
                udpSocket?.close()
                udpSocket = null
                // No need to update state here usually, should be handled by stop request or error handling
            }
        }
    }

    fun stopUdpListener() {
        if (!isUdpListenerRunning() && udpListenerJob == null) return // Already stopped
        Log.i(TAG, "Stopping UDP Listener...")
        udpListenerJob?.cancel()
        udpSocket?.close() // Interrupts receive()
        udpListenerJob = null
        udpSocket = null
        Log.i(TAG, "UDP Listener stopped.")
        // Don't reset packet count here
    }

    private fun extractStatsFromResult(data: Intent?): String {
        // Same as before
        if (data == null) return "No stats data."
        val bytesSent = data.getLongExtra("bytes_sent", -1)
        val bytesRcvd = data.getLongExtra("bytes_rcvd", -1)
        return "Stats: [Sent: $bytesSent B, Rcvd: $bytesRcvd B]"
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared. Stopping listener.")
        stopUdpListener()
    }
}