package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related

import com.example.captive_portal_analyzer_kotlin.IMainViewModel
import com.example.captive_portal_analyzer_kotlin.ThemeMode
import com.example.captive_portal_analyzer_kotlin.components.ToastStyle
import com.example.captive_portal_analyzer_kotlin.dataclasses.CustomWebViewRequestEntity
import com.example.captive_portal_analyzer_kotlin.dataclasses.WebpageContentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

class PreviewMainViewModel(
) : IMainViewModel {

    // Implement required properties with simple StateFlows for previews
    override val isConnected = MutableStateFlow(true)
    override val clickedSessionId = MutableStateFlow<String?>("preview")

    // Implement required functions (mostly no-ops for static previews)
    override fun hideToast() {}
    override fun updateClickedContent(webpageContentEntity: WebpageContentEntity) {
    }

    override fun updateClickedRequest(webViewRequestEntity: CustomWebViewRequestEntity) {
    }

    override fun hideDialog() {}
    override fun showToast(
        message: String,
        style: ToastStyle
    ) {
    }

    override fun updateClickedSessionId(sessionId: String?) {}
    override fun updateLocale(locale: Locale) {
    }

    override fun updateThemeMode(mode: ThemeMode) {
    }

    override fun showDialog(
        title: String,
        message: String,
        confirmText: String,
        dismissText: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
    }

}