package com.example.captive_portal_analyzer_kotlin.navigation

// Import NEW Screens and ViewModel Factory
import AutomaticAnalysisInputScreen
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
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.AnalysisScreen
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.AnalysisScreenConfig
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.IntentLaunchConfig
import com.example.captive_portal_analyzer_kotlin.screens.analysis.ui.NavigationConfig
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
    object Analysis : Screen("analysis", R.string.analysis_screen_title)
    object SessionList : Screen("session_list", R.string.session_list_screen_title)
    object Session : Screen("session", R.string.session_screen_title)
    object About : Screen("about", R.string.about_screen_title)
    object Settings : Screen("settings", R.string.settings)
    object WebPageContent : Screen("webpage_content", R.string.webpage_content)
    object RequestDetails : Screen("request_details", R.string.request_details)
    object PCAPDroidSetup : Screen("pcapdroid_setup", R.string.pcap_setup_screen_title)
}

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
        composable(route = Screen.Analysis.route) {
            AnalysisScreen(
                screenConfig = AnalysisScreenConfig(
                    repository = repository,
                    sessionManager = sessionManager,
                    mainViewModel = mainViewModel,
                ),
                navigationConfig = NavigationConfig(
                    onNavigateToSessionList = actions.navigateToSessionListScreen,
                    onNavigateToManualConnect = actions.navigateToManualConnectScreen,
                    onNavigateToSetupPCAPDroidScreen = actions.navigateToSetupPCAPDroidScreen,
                ),
                intentLaunchConfig = IntentLaunchConfig(
                    onStartIntent = onStartIntentLaunchRequested,
                    onStopIntent = onStopIntentLaunchRequested,
                    onStatusIntent = onStatusIntentLaunchRequested,
                    onOpenFile = onOpenFileRequested
                )
            )
        }
        composable(route = Screen.SessionList.route) {
            SessionListScreen(
                repository = repository,
                navigateToWelcome = actions.navigateToWelcomeScreen,
                updateClickedSessionId = mainViewModel::updateClickedSessionId,
                navigateToSessionScreen = actions.navigateToSessionScreen
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

    // Updated action: Navigate to the START of the nested graph
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