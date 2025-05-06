package com.example.captive_portal_analyzer_kotlin.navigation

import NetworkSessionRepository
import android.app.Application
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.ThemeMode
import com.example.captive_portal_analyzer_kotlin.components.ActionAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.AppToast
import com.example.captive_portal_analyzer_kotlin.components.DialogState
import com.example.captive_portal_analyzer_kotlin.screens.about.AboutScreen
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.AnalysisScreen
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.AnalysisScreenConfig
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.AnalysisViewModel
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.AnalysisViewModelFactory
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.IntentLaunchConfig
import com.example.captive_portal_analyzer_kotlin.screens.analysis.analysis_start.ui.NavigationConfig
import com.example.captive_portal_analyzer_kotlin.screens.analysis.screenshots_flagging.ScreenshotFlaggingScreen
import com.example.captive_portal_analyzer_kotlin.screens.analysis.screenshots_flagging.ScreenshotFlaggingViewModel
import com.example.captive_portal_analyzer_kotlin.screens.analysis.screenshots_flagging.ScreenshotFlaggingViewModelFactory
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.AutomaticAnalysisInputScreen
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.AutomaticAnalysisOutputScreen
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.AutomaticAnalysisViewModel
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.AutomaticAnalysisViewModelFactory
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.PcapInclusionScreen
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.PromptPreviewScreen
import com.example.captive_portal_analyzer_kotlin.screens.manual_connect.ManualConnectScreen
import com.example.captive_portal_analyzer_kotlin.screens.request_details_screen.RequestDetailsScreen
import com.example.captive_portal_analyzer_kotlin.screens.session.SessionScreen
import com.example.captive_portal_analyzer_kotlin.screens.session_list.SessionListScreen
import com.example.captive_portal_analyzer_kotlin.screens.settings.SettingsScreen
import com.example.captive_portal_analyzer_kotlin.screens.setup_pcapdroid.SetupPCAPDroidScreen
import com.example.captive_portal_analyzer_kotlin.screens.webpage_content.WebpageContentScreen
import com.example.captive_portal_analyzer_kotlin.screens.welcome.WelcomeScreen
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager
import java.util.Locale

sealed class Screen(val route: String, @StringRes val titleStringResource: Int) {
    object Welcome : Screen("welcome", R.string.welcome_screen_title)
    object ManualConnect : Screen("manual_connect", R.string.manual_connect_screen_title)
    object Analysis : Screen("analysis_start", R.string.analysis_screen_title)
    object ScreenshotFlagging : Screen("screenshot_flagging", R.string.screenshot_flagging_screen_title)
    object SessionList : Screen("session_list", R.string.session_list_screen_title)
    object Session : Screen("session", R.string.session_screen_title)
    object About : Screen("about", R.string.about_screen_title)
    object Settings : Screen("settings", R.string.settings)
    object WebPageContent : Screen("webpage_content", R.string.webpage_content)
    object RequestDetails : Screen("request_details", R.string.request_details)
    object PCAPDroidSetup : Screen("pcapdroid_setup", R.string.pcap_setup_screen_title)
}

//  a new route for the primary analysis flow graph
val PrimaryAnalysisGraphRoute = "primary_analysis_graph"

// Routes related to Automatic Analysis
val AutomaticAnalysisInputRoute = "automatic_analysis_input"
val AutomaticAnalysisGraphRoute = "automatic_analysis_graph"
val AutomaticAnalysisOutputRoute = "automatic_analysis_output"
val PcapInclusionRoute = "pcap_inclusion"
val AutomaticAnalysisPromptPreviewRoute = "automatic_analysis_prompt_preview"

@Composable
fun AppNavGraph(
    navController: NavHostController,
    sessionManager: NetworkSessionManager,
    mainViewModel: MainViewModel,
    dialogState: DialogState,
    repository: NetworkSessionRepository,
    themeMode: ThemeMode,
    currentLanguage: String,
    onThemeChanged: (mode: ThemeMode) -> Unit,
    onLocalChanged: (locale: Locale) -> Unit,
    onStartIntentLaunchRequested: (Intent) -> Unit,
    onStopIntentLaunchRequested: (Intent) -> Unit,
    onStatusIntentLaunchRequested: (Intent) -> Unit,
    onOpenFileRequested: (String) -> Unit,
    skipSetup: Boolean,
) {
    val actions = remember(navController) { NavigationActions(navController) }
    val toastState by mainViewModel.toastState.collectAsState()

    AppToast(
        toastState = toastState,
        onDismissRequest = { mainViewModel.hideToast() }
    )

    ActionAlertDialog(
        dialogState = dialogState,
        onDismissRequest = { mainViewModel.hideDialog() }
    )

    NavHost(navController = navController, startDestination = Screen.Welcome.route) {
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                navigateToSetupPCAPDroidScreen = actions.navigateToSetupPCAPDroidScreen,
                navigateToManualConnectScreen = actions.navigateToManualConnectScreen,
                skipSetup = skipSetup,
                repository = repository
            )
        }
        composable(route = Screen.ManualConnect.route) {
            ManualConnectScreen(
                navigateToAnalysis = actions.navigateToAnalysisScreen,
            )
        }


        // --- Nested Navigation Graph for Primary Analysis Flow (Analysis -> Flagging) ---
        navigation(
            startDestination = Screen.Analysis.route, // Start with the Analysis screen
            route = PrimaryAnalysisGraphRoute // Define the route for this graph
        ) {
            // Analysis Screen Composable (inside the primary analysis graph)
            composable(route = Screen.Analysis.route) { backStackEntry ->
                // Get ViewModel scoped to the primary analysis graph
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(PrimaryAnalysisGraphRoute)
                }
                val application = LocalContext.current.applicationContext as Application

                val analysisViewModel: AnalysisViewModel = viewModel(
                    viewModelStoreOwner = graphEntry, // Scope ViewModel to the graph
                    factory = AnalysisViewModelFactory(
                        application = application,
                        repository = repository,
                        sessionManager = sessionManager,
                        showToast = mainViewModel::showToast,
                        mainViewModel = mainViewModel
                    )
                )

                AnalysisScreen(
                    screenConfig = AnalysisScreenConfig(
                        repository = repository,
                        sessionManager = sessionManager,
                        mainViewModel = mainViewModel,
                    ),
                    navigationConfig = NavigationConfig(
                        // navigateToSessionList is NOT called from here anymore
                        onNavigateToSessionList = {}, // Dummy lambda or remove if not needed by AnalysisScreen internal logic
                        onNavigateToManualConnect = actions.navigateToManualConnectScreen, // Still navigates outside graph if needed
                        onNavigateToSetupPCAPDroid = actions.navigateToSetupPCAPDroidScreen, // Still navigates outside graph
                        // Pass action to navigate to the next screen *within* this graph
                        onNavigateToScreenshotFlagging = actions.navigateToScreenshotFlaggingScreen
                    ),
                    intentLaunchConfig = IntentLaunchConfig(
                        onStartIntent = onStartIntentLaunchRequested,
                        onStopIntent = onStopIntentLaunchRequested,
                        onStatusIntent = onStatusIntentLaunchRequested,
                        onOpenFile = onOpenFileRequested
                    )
                    // Pass the ViewModel explicitly if needed by nested composables
                    // analysisViewModel = analysisViewModel // Pass ViewModel down
                )
            }

            // Screenshot Flagging Screen Composable (inside the primary analysis graph)
            composable(route = Screen.ScreenshotFlagging.route) { backStackEntry ->
                // Get the SAME ViewModel instance scoped to the graph
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(PrimaryAnalysisGraphRoute)
                }

                // *** Create and get the ScreenshotFlaggingViewModel scoped to the graph ***
                val application = LocalContext.current.applicationContext as Application
                val screenshotFlaggingViewModel: ScreenshotFlaggingViewModel = viewModel(
                    viewModelStoreOwner = graphEntry, // Scope to the *graph* NavBackStackEntry
                    factory = ScreenshotFlaggingViewModelFactory( // Use the correct factory
                        application = application,
                        repository = repository, // Pass dependencies the factory needs
                        sessionManager = sessionManager,
                        showToast = mainViewModel::showToast // Pass dependency
                    )
                )

                ScreenshotFlaggingScreen(
                    viewModel = screenshotFlaggingViewModel, // Pass the shared ViewModel
                    // Pass action to navigate *outside* the graph to the Session List
                    onFinishFlagging = actions.navigateToSessionListScreen,
                    // Optionally pass a back navigation action if needed
                    // onNavigateBack = actions.popBackStack
                )
            }
        }
        // --- END Primary Analysis Graph ---

        composable(route = Screen.SessionList.route) {
            SessionListScreen(
                repository = repository,
                navigateToWelcome = actions.navigateToWelcomeScreen,
                updateClickedSessionId = mainViewModel::updateClickedSessionId,
                navigateToSessionScreen = actions.navigateToSessionScreen,
                mainViewModel = mainViewModel
            )
        }
        composable(route = Screen.Session.route) {
            SessionScreen(
                repository = repository,
                mainViewModel = mainViewModel,
                navigateToAutomaticAnalysis = actions.navigateToAutomaticAnalysisGraph, // Navigate to the GRAPH
                navigateToWebpageContentScreen = actions.navigateToWebpageContentScreen,
                navigateToRequestDetailsScreen = actions.navigateToRequestDetailsScreen
            )
        }
        composable(route = Screen.About.route) {
            AboutScreen()
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                themeMode = themeMode,
                currentLanguage = currentLanguage,
                onThemeChange = onThemeChanged,
                onLocalChanged = onLocalChanged,
            )
        }
        composable(route = Screen.WebPageContent.route) {
            WebpageContentScreen(
                mainViewModel = mainViewModel,
            )
        }
        composable(route = Screen.RequestDetails.route) {
            RequestDetailsScreen(
                mainViewModel = mainViewModel,
            )
        }
        composable(route = Screen.PCAPDroidSetup.route) {
            SetupPCAPDroidScreen(
                navigateToManualConnectScreen = actions.navigateToManualConnectScreen,
                updateSkipSetup = mainViewModel::updateSkipSetupPreference,
            )
        }

        // --- Nested Navigation Graph for Automatic Analysis ---
        navigation(
            startDestination = AutomaticAnalysisInputRoute,
            route = AutomaticAnalysisGraphRoute
        ) {
            // Input Screen Composable
            composable(route = AutomaticAnalysisInputRoute) { backStackEntry ->
                // ---> Get the graph's NavBackStackEntry <---
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AutomaticAnalysisGraphRoute)
                }
                val clickedSessionId by mainViewModel.clickedSessionId.collectAsState()
                val application = LocalContext.current.applicationContext as Application

                // ---> Create/Get ViewModel scoped to the graph <---
                val automaticAnalysisViewModel: AutomaticAnalysisViewModel = viewModel(
                    viewModelStoreOwner = graphEntry,
                    factory = AutomaticAnalysisViewModelFactory(
                        application = application,
                        repository = repository,
                        clickedSessionId = clickedSessionId
                    )
                )

                AutomaticAnalysisInputScreen(
                    navController = navController,
                    viewModel = automaticAnalysisViewModel,
                    mainViewModel = mainViewModel
                )
            }

            // --- PCAP Inclusion Screen Composable ---
            composable(route = PcapInclusionRoute) { backStackEntry ->
                // ---> Get the graph's NavBackStackEntry <---
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AutomaticAnalysisGraphRoute)
                }

                // ---> Get the SAME ViewModel instance scoped to the graph <---
                val automaticAnalysisViewModel: AutomaticAnalysisViewModel = viewModel(
                    viewModelStoreOwner = graphEntry
                )

                PcapInclusionScreen(
                    navController = navController,
                    viewModel = automaticAnalysisViewModel,
                    mainViewModel = mainViewModel
                )
            }
            // --- END PCAP Inclusion Screen Composable ---

            // ---  Prompt Preview Screen Composable ---
            composable(route = AutomaticAnalysisPromptPreviewRoute) { backStackEntry ->
                // ---> Get the graph's NavBackStackEntry <---
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AutomaticAnalysisGraphRoute)
                }

                // ---> Get the SAME ViewModel instance scoped to the graph <---
                val automaticAnalysisViewModel: AutomaticAnalysisViewModel = viewModel(
                    viewModelStoreOwner = graphEntry
                )

                PromptPreviewScreen(
                    navController = navController,
                    viewModel = automaticAnalysisViewModel,
                    mainViewModel = mainViewModel
                )
            }
            // --- END Prompt Preview Screen Composable ---

            // Output Screen Composable
            composable(route = AutomaticAnalysisOutputRoute) { backStackEntry ->
                // ---> Get the graph's NavBackStackEntry <---
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AutomaticAnalysisGraphRoute)
                }

                // ---> Get the SAME ViewModel instance scoped to the graph <---
                val automaticAnalysisViewModel: AutomaticAnalysisViewModel = viewModel(
                    viewModelStoreOwner = graphEntry
                )

                AutomaticAnalysisOutputScreen(
                    navController = navController,
                    viewModel = automaticAnalysisViewModel,
                    mainViewModel = mainViewModel
                )
            }
        }

    }
}

class NavigationActions(private val navController: NavHostController) {

    val navigateToManualConnectScreen: () -> Unit = {
        navController.navigate(Screen.ManualConnect.route)
    }
    val navigateToAnalysisScreen: () -> Unit = {
        navController.navigate(Screen.Analysis.route)
    }
    val navigateToWelcomeScreen: () -> Unit = {
        navController.navigate(Screen.Welcome.route)
    }
    val navigateToSessionListScreen: () -> Unit = {
        navController.navigate(Screen.SessionList.route)
    }
    val navigateToSessionScreen: () -> Unit = {
        navController.navigate(Screen.Session.route)
    }

    //  action to navigate to the start of the Primary Analysis Graph
    val navigateToPrimaryAnalysisGraph: () -> Unit = {
        navController.navigate(PrimaryAnalysisGraphRoute) {
            // Clear backstack up to Welcome to simplify flow after connecting
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
        }
    }

    // Action to navigate *within* the Primary Analysis Graph to the Screenshot Flagging screen
    val navigateToScreenshotFlaggingScreen: () -> Unit = {
        navController.navigate(Screen.ScreenshotFlagging.route) {
            // Optional: Clear Analysis screen from backstack once navigating to Flagging
            popUpTo(Screen.Analysis.route) { inclusive = true }
        }
    }

    //  action: Navigate to the START of the nested graph
    val navigateToAutomaticAnalysisGraph: () -> Unit = {
        navController.navigate(AutomaticAnalysisGraphRoute)
    }

    val navigateToWebpageContentScreen: () -> Unit = {
        navController.navigate(Screen.WebPageContent.route)
    }
    val navigateToRequestDetailsScreen: () -> Unit = {
        navController.navigate(Screen.RequestDetails.route)
    }
    val navigateToSetupPCAPDroidScreen: () -> Unit = {
        navController.navigate(Screen.PCAPDroidSetup.route)
    }

}