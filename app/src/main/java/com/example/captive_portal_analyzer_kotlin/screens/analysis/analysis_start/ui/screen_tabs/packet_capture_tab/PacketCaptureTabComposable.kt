
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.PcapDroidPacketCaptureStatus
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.FileOpener
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.WebViewActions
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.screen_tabs.packet_capture_tab.packet_capture_states.PacketCaptureDisabledContent
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.screen_tabs.packet_capture_tab.packet_capture_states.PacketCaptureEnabledContent
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.screen_tabs.packet_capture_tab.packet_capture_states.PacketCaptureInitialContent


@Composable
internal fun PacketCaptureTabComposable(
    captureState: MainViewModel.PcapDroidCaptureState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onStatusCheck: () -> Unit,
    statusMessage: String,
    onOpenFile: FileOpener,
    webViewActions: WebViewActions,
    copiedPcapFileUri: Uri?,
    modifier: Modifier = Modifier,
    targetPcapName: String?,
    pcapDroidPacketCaptureStatus: PcapDroidPacketCaptureStatus,
    updateSelectedTabIndex: (Int) -> Unit,
    storePcapFileToSession: () -> Unit,
    markAnalysisAsComplete: () -> Unit
) {

    when (pcapDroidPacketCaptureStatus) {
        PcapDroidPacketCaptureStatus.INITIAL -> PacketCaptureInitialContent()

        PcapDroidPacketCaptureStatus.PCAPDROID_CAPTURE_DISABLED -> PacketCaptureDisabledContent(
            webViewActions = webViewActions,
            updateSelectedTabIndex = updateSelectedTabIndex,
            markAnalysisAsComplete = markAnalysisAsComplete,
        )

        PcapDroidPacketCaptureStatus.PCAPDROID_CAPTURE_ENABLED -> PacketCaptureEnabledContent(
            modifier = modifier,
            pcapdroidCaptureState = captureState,
            onStartCapture = onStartCapture,
            onStopCapture = onStopCapture,
            onStatusCheck = onStatusCheck,
            statusMessage = statusMessage,
            copiedPcapFileUri = copiedPcapFileUri,
            onOpenFile = onOpenFile,
            targetPcapName = targetPcapName,
            webViewActions = webViewActions,
            storePcapFileToSession = storePcapFileToSession,
            markAnalysisAsComplete = markAnalysisAsComplete,
        )
    }
}


