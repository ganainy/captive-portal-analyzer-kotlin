package com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.preview_related

import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.AutomaticAnalysisUiState
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.IAutomaticAnalysisViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreviewAutomaticAnalysisViewModel(
    initialState: AutomaticAnalysisUiState
) : IAutomaticAnalysisViewModel {

    // Use MutableStateFlow for the preview state
    private val _mockUiState = MutableStateFlow(initialState)
    override val automaticAnalysisUiState: StateFlow<AutomaticAnalysisUiState> = _mockUiState.asStateFlow()

    // Implement functions as No-Ops (they don't do anything in static previews)
    override fun analyzeWithAi() { /* No-op */ }
    override fun updatePromptEditText(text: String) { /* No-op */ }
    override fun loadSessionData() { /* No-op */ }
    override fun toggleRequestSelection(requestId: Int) { /* No-op */ }
    override fun toggleScreenshotSelection(screenshotId: Int) { /* No-op */ }
    override fun toggleWebpageContentSelection(contentId: Int) { /* No-op */ }
    override fun setAllRequestsSelected(select: Boolean) { /* No-op */ }
    override fun setAllScreenshotsSelected(select: Boolean) { /* No-op */ }
    override fun setAllWebpageContentSelected(select: Boolean) { /* No-op */ }
    override fun setClickedSessionId(clickedSessionId: String?) {
        /* No-op */
    }
}
