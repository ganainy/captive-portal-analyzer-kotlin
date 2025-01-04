package com.example.captive_portal_analyzer_kotlin.navigation

import NetworkSessionRepository
import androidx.annotation.StringRes
import com.example.captive_portal_analyzer_kotlin.screens.manual_connect.ManualConnectScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.example.captive_portal_analyzer_kotlin.screens.network_list.NetworkListScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.screens.about.AboutScreen
import com.example.captive_portal_analyzer_kotlin.screens.analysis.AnalysisScreen
import com.example.captive_portal_analyzer_kotlin.screens.session.SessionScreen
import com.example.captive_portal_analyzer_kotlin.SharedViewModel
import com.example.captive_portal_analyzer_kotlin.components.ActionAlertDialog
import com.example.captive_portal_analyzer_kotlin.components.AppToast
import com.example.captive_portal_analyzer_kotlin.components.DialogState
import com.example.captive_portal_analyzer_kotlin.screens.session_list.SessionListScreen
import com.example.captive_portal_analyzer_kotlin.screens.welcome.WelcomeScreen
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager

sealed class Screen(val route: String,@StringRes val titleStringResource: Int) {
    object Welcome : Screen("welcome", R.string.welcome_screen_title)
    object ManualConnect : Screen("manual_connect", R.string.manual_connect_screen_title)
    object NetworkList : Screen("network_list", R.string.network_list_screen_title)
    object Analysis : Screen("analysis", R.string.analysis_screen_title)
    object SessionList : Screen("session_list", R.string.session_list_screen_title)
    object Session : Screen("session", R.string.session_screen_title)
    object About : Screen("about", R.string.about_screen_title)
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    sessionManager: NetworkSessionManager,
    sharedViewModel: SharedViewModel,
    dialogState: DialogState,
    repository: NetworkSessionRepository,
) {
    val actions = remember(navController) { NavigationActions(navController) }

    val toastState by sharedViewModel.toastState.collectAsState()

    AppToast(
        toastState = toastState,
        onDismissRequest = { sharedViewModel.hideToast() }
    )

    ActionAlertDialog(
        dialogState = dialogState,
        onDismissRequest = { sharedViewModel.hideDialog() }
    )

    NavHost(navController = navController, startDestination = Screen.Welcome.route) {
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                navigateToNetworkList =actions.navigateToNetworkListScreen,

            )
        }
        composable(route = Screen.ManualConnect.route) {
            ManualConnectScreen(
                navigateToAnalysis =actions.navigateToAnalysisScreen,
                navigateToNetworkList =actions.navigateToNetworkListScreen,
                navigateToAbout = actions.navigateToAbout,
            )
        }
        composable(route = Screen.NetworkList.route) {
            NetworkListScreen(
                navigateToManualConnect = actions.navigateToManualConnectScreen,
                navigateToAnalysis = actions.navigateToAnalysisScreen,
                navigateToAbout = actions.navigateToAbout,
            )
        }
        composable(route = Screen.Analysis.route) {
            AnalysisScreen(
                repository = repository,
                navigateToSessionList = actions.navigateToSessionListScreen,
                navigateToManualConnect = actions.navigateToManualConnectScreen,
                sessionManager = sessionManager,
                sharedViewModel = sharedViewModel,
            )
        }
        composable(
            route = Screen.SessionList.route,
        ) {
                SessionListScreen(
                    repository = repository,
                    navigateToWelcome = actions.navigateToWelcomeScreen,
                    updateClickedSessionId = sharedViewModel::updateClickedSessionId,
                    navigateToSessionScreen = actions.navigateToSessionScreen
                )
        }
        composable(
            route = Screen.Session.route,
        ) {
            SessionScreen(
                repository = repository,
                sharedViewModel = sharedViewModel,
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
    val navigateToNetworkListScreen: () -> Unit = {
        navController.navigate(Screen.NetworkList.route)
    }
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


    val navigateBack: () -> Unit = {
        navController.popBackStack()
    }

}