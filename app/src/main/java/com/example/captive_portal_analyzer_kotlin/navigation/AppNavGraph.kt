package com.example.captive_portal_analyzer_kotlin.navigation


import NetworkSessionRepository
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.captive_portal_analyzer_kotlin.MainViewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.ThemeMode
import com.example.captive_portal_analyzer_kotlin.components.ActionAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.AppToast
import com.example.captive_portal_analyzer_kotlin.components.DialogState
import com.example.captive_portal_analyzer_kotlin.screens.about.AboutScreen
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisScreen
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisScreenConfig
import com.example.captive_portal_analyzer_kotlin.screens.analysis.IntentLaunchConfig
import com.example.captive_portal_analyzer_kotlin.screens.analysis.NavigationConfig
import com.example.captive_portal_analyzer_kotlin.screens.automatic_analysis.AutomaticAnalysisScreen
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

/**
 * A sealed class representing the possible screens in the app. This class is used as a navigation
 * graph, and the [route] property is used as the route for the NavHost.
 *
 * @property route The route for the NavHost.
 * @property titleStringResource The string resource id for the title of the screen.
 */
sealed class Screen(val route: String, @StringRes val titleStringResource: Int) {

    object Welcome : Screen("welcome", R.string.welcome_screen_title)

    object ManualConnect : Screen("manual_connect", R.string.manual_connect_screen_title)

    object Analysis : Screen("analysis", R.string.analysis_screen_title)

    object SessionList : Screen("session_list", R.string.session_list_screen_title)

    object Session : Screen("session", R.string.session_screen_title)

    object About : Screen("about", R.string.about_screen_title)

    object AutomaticAnalysis : Screen("automatic_analysis", R.string.automatic_analysis)

    object Settings : Screen("settings", R.string.settings)

    object WebPageContent : Screen("webpage_content", R.string.webpage_content)

    object RequestDetails : Screen("request_details", R.string.request_details)

    object PCAPDroidSetup : Screen("pcapdroid_setup", R.string.pcap_setup_screen_title)
}

/**
 * A composable function that sets up the navigation graph for the app. This function is responsible for
 * setting up the navigation graph, and for handling the navigation between the different screens in
 * the app.
 *
 * @param navController The navController to use for navigating between screens.
 * @param sessionManager The sessionManager to use for managing the network sessions.
 * @param mainViewModel The sharedViewModel to use for shared state between screens.
 * @param dialogState The dialogState to use for showing and hiding dialogs.
 * @param repository The repository to use for storing and retrieving data both locally and remotely.
 * @param themeMode The theme mode to use for the app.
 * @param currentLanguage The current language to use for the app.
 * @param onThemeChanged A function to call when the theme is changed.
 * @param onLocalChanged A function to call when the locale is changed.
 */
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
    // Remember navigation actions for the navController
    val actions = remember(navController) { NavigationActions(navController) }

    // Collect the current toast state from the shared ViewModel
    val toastState by mainViewModel.toastState.collectAsState()

    // Passed to screens to give the ability to display a toast notification
    AppToast(
        toastState = toastState,
        onDismissRequest = { mainViewModel.hideToast() }
    )

    // Passed to screens to give the ability to display an alert dialog
    ActionAlertDialog(
        dialogState = dialogState,
        onDismissRequest = { mainViewModel.hideDialog() }
    )

    //Setup the navigation graph,parameters needed for each screen and which screen to use when the app starts
    NavHost(navController = navController, startDestination = Screen.Welcome.route) {
        composable(route = Screen.Welcome.route) {
            // No need to pass showTopBar, as the default is true and we want it here
            WelcomeScreen(
                navigateToSetupPCAPDroidScreen = actions.navigateToSetupPCAPDroidScreen,
                navigateToManualConnectScreen = actions.navigateToManualConnectScreen,
                skipSetup = skipSetup,
            )
        }
        composable(route = Screen.ManualConnect.route) {
            // No need to pass showTopBar, as the default is true and we want it here
            ManualConnectScreen(
                navigateToAnalysis = actions.navigateToAnalysisScreen,
            )
        }

        composable(route = Screen.Analysis.route) {
            //Pass showTopBar = false to hide the top bar on this screen
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
        composable(
            route = Screen.SessionList.route,
        ) {
            // No need to pass showTopBar, as the default is true and we want it here
            SessionListScreen(
                repository = repository,
                navigateToWelcome = actions.navigateToWelcomeScreen,
                updateClickedSessionId = mainViewModel::updateClickedSessionId,
                navigateToSessionScreen = actions.navigateToSessionScreen
            )
        }
        composable(
            route = Screen.Session.route,
        ) {
            // No need to pass showTopBar, as the default is true and we want it here
            SessionScreen(
                repository = repository,
                mainViewModel = mainViewModel,
                navigateToAutomaticAnalysis = actions.navigateToAutomaticAnalysisScreen,
                navigateToWebpageContentScreen = actions.navigateToWebpageContentScreen,
                navigateToRequestDetailsScreen = actions.navigateToRequestDetailsScreen
            )
        }
        composable(
            route = Screen.About.route,
        ) {
            // No need to pass showTopBar, as the default is true and we want it here
            AboutScreen()
        }

        composable(
            route = Screen.AutomaticAnalysis.route,
        ) {
            // No need to pass showTopBar, as the default is true and we want it here
            AutomaticAnalysisScreen(
                mainViewModel = mainViewModel,
                repository = repository,
            )
        }

        composable(
            route = Screen.Settings.route,
        ) {
            // No need to pass showTopBar, as the default is true and we want it here
            SettingsScreen(
                themeMode = themeMode,
                currentLanguage = currentLanguage,
                onThemeChange = onThemeChanged,
                onLocalChanged = onLocalChanged,
            )
        }
        composable(
            route = Screen.WebPageContent.route,
        ) {
            // No need to pass showTopBar, as the default is true and we want it here
            WebpageContentScreen(
                mainViewModel = mainViewModel,
            )
        }
        composable(
            route = Screen.RequestDetails.route,
        ) {
            // No need to pass showTopBar, as the default is true and we want it here
            RequestDetailsScreen(
                mainViewModel = mainViewModel,
            )
        }

        composable(
            route = Screen.PCAPDroidSetup.route,
        ) {
            SetupPCAPDroidScreen(
                navigateToManualConnectScreen = actions.navigateToManualConnectScreen,
                updateSkipSetup = mainViewModel::updateSkipSetupPreference,
            )
        }


    }
}

/**
 * Class to hold navigation actions for the app. This class is used to create actions which can be used to
 * navigate between different screens in the app.
 *
 * @param navController The navController to use for navigating between screens.
 */
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

    val navigateToAutomaticAnalysisScreen: () -> Unit = {
        navController.navigate(Screen.AutomaticAnalysis.route)
    }

    val navigateToWebpageContentScreen: () -> Unit = {
        navController.navigate(Screen.WebPageContent.route)
    }

    val navigateToRequestDetailsScreen: () -> Unit =
        { navController.navigate(Screen.RequestDetails.route) }

    val navigateToSetupPCAPDroidScreen: () -> Unit = {
        navController.navigate(Screen.PCAPDroidSetup.route)
    }

    val navigateBack: () -> Unit = {
        navController.popBackStack()
    }


}