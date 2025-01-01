package com.example.captive_portal_analyzer_kotlin.navigation

import com.example.captive_portal_analyzer_kotlin.screens.manual_connect.HomeScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.example.captive_portal_analyzer_kotlin.screens.landing.LandingScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.captive_portal_analyzer_kotlin.screens.about.AboutScreen
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisScreen
import com.example.captive_portal_analyzer_kotlin.screens.menu.MenuScreen
import com.example.captive_portal_analyzer_kotlin.screens.session.SessionScreen
import com.example.captive_portal_analyzer_kotlin.SharedViewModel
import com.example.captive_portal_analyzer_kotlin.components.ActionAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.AppToast
import com.example.captive_portal_analyzer_kotlin.components.DialogState
import com.example.captive_portal_analyzer_kotlin.firebase.OnlineRepository
import com.example.captive_portal_analyzer_kotlin.room.AppDatabase
import com.example.captive_portal_analyzer_kotlin.room.custom_webview_request.OfflineCustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.network_session.OfflineNetworkSessionRepository
import com.example.captive_portal_analyzer_kotlin.room.screenshots.OfflineScreenshotRepository
import com.example.captive_portal_analyzer_kotlin.room.webpage_content.OfflineWebpageContentRepository
import com.example.captive_portal_analyzer_kotlin.screens.session_list.SessionListScreen
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager

sealed class Screen(val route: String) {
    object Menu : Screen("menu")
    object ManualConnect : Screen("manual_connect")
    object Landing : Screen("landing")
    object Analysis : Screen("analysis")
    object Report : Screen("report")
    object Session : Screen("session")
    object About : Screen("about")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    sessionManager: NetworkSessionManager,
    sharedViewModel: SharedViewModel,
    dialogState: DialogState,
) {
    val actions = remember(navController) { NavigationActions(navController) }

     val offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository by lazy {
        OfflineCustomWebViewRequestsRepository(AppDatabase.getDatabase(navController.context).customWebViewRequestDao())
    }

    val offlineWebpageContentRepository: OfflineWebpageContentRepository by lazy {
        OfflineWebpageContentRepository(AppDatabase.getDatabase(navController.context).webpageContentDao())
    }

    val screenshotRepository: OfflineScreenshotRepository by lazy {
        OfflineScreenshotRepository(AppDatabase.getDatabase(navController.context).screenshotDao())
    }

    val offlineNetworkSessionRepository: OfflineNetworkSessionRepository by lazy {
        OfflineNetworkSessionRepository(AppDatabase.getDatabase(navController.context).networkSessionDao())
    }

    val onlineRepository: OnlineRepository by lazy {
        OnlineRepository()
    }

    val toastState by sharedViewModel.toastState.collectAsState()

    AppToast(
        toastState = toastState,
        onDismissRequest = { sharedViewModel.hideToast() }
    )

    ActionAlertDialog(
        dialogState = dialogState,
        onDismissRequest = { sharedViewModel.hideDialog() }
    )

    NavHost(navController = navController, startDestination = Screen.Menu.route) {
        composable(route = Screen.Menu.route) {
            MenuScreen(
                navigateToLanding =actions.navigateToLandingScreen,
                navigateToAbout = actions.navigateToAbout,
                navigateToReport = actions.navigateToReportScreen
            )
        }
        composable(route = Screen.ManualConnect.route) {
            HomeScreen(
                navigateToAnalysis =actions.navigateToAnalysisScreen,
                navigateToLanding =actions.navigateToLandingScreen,
                navigateToAbout = actions.navigateToAbout,
            )
        }
        composable(route = Screen.Landing.route) {
            LandingScreen(
                navigateToManualConnect = actions.navigateToManualConnectScreen,
                navigateToAnalysis = actions.navigateToAnalysisScreen,
                navigateToAbout = actions.navigateToAbout,
            )
        }
        composable(route = Screen.Analysis.route) {
            AnalysisScreen(
                offlineCustomWebViewRequestsRepository = offlineCustomWebViewRequestsRepository,
                offlineWebpageContentRepository = offlineWebpageContentRepository,
                screenshotRepository = screenshotRepository,
                navigateToReport = actions.navigateToReportScreen,
                navigateBack = actions.navigateBack,
                navigateToAbout = actions.navigateToAbout,
                sessionManager = sessionManager,
                sharedViewModel = sharedViewModel,
            )
        }
        composable(
            route = Screen.Report.route,
        ) {
                SessionListScreen(
                    navigateToMenu = actions.navigateToMenuScreen,
                    offlineCustomWebViewRequestsRepository = offlineCustomWebViewRequestsRepository,
                    offlineWebpageContentRepository = offlineWebpageContentRepository,
                    offlineScreenshotRepository = screenshotRepository,
                    navigateToAbout = actions.navigateToAbout,
                    offlineNetworkSessionRepository =offlineNetworkSessionRepository,
                    clickedSession = sharedViewModel::setClickedSession,
                    navigateToSessionScreen = actions.navigateToSessionScreen
                )
        }
        composable(
            route = Screen.Session.route,
        ) {
            SessionScreen(
                navigateBack = actions.navigateBack,
                navigateToAbout = actions.navigateToAbout,
                sharedViewModel = sharedViewModel,
                onlineRepository = onlineRepository,
                offlineNetworkSessionRepository = offlineNetworkSessionRepository,
            )
        }
        composable(
            route = Screen.About.route,
        ) {
            AboutScreen(
                navigateBack = actions.navigateBack,
            )
        }




    }
}

class NavigationActions(private val navController: NavHostController) {
    val navigateToAbout: () -> Unit = {
        navController.navigate(Screen.About.route)
    }
    /*val navigateToSignUp: () -> Unit = {
            navController.navigate(Screen.SignUp.route) {
                popUpTo(Screen.SignUp.route) { inclusive = true }
            }
        }*/
    val navigateToLandingScreen: () -> Unit = {
        navController.navigate(Screen.Landing.route)
    }
    val navigateToManualConnectScreen: () -> Unit = {
        navController.navigate(Screen.ManualConnect.route)
    }

    val navigateToAnalysisScreen: () -> Unit = {
        navController.navigate(Screen.Analysis.route)
    }

    val navigateToMenuScreen: () -> Unit = {
        navController.navigate(Screen.Menu.route)
    }

    val navigateToReportScreen: () -> Unit = {
        navController.navigate(Screen.Report.route)
    }

    val navigateToSessionScreen: () -> Unit = {
        navController.navigate(Screen.Session.route)
    }


    val navigateBack: () -> Unit = {
        navController.popBackStack()
    }

}