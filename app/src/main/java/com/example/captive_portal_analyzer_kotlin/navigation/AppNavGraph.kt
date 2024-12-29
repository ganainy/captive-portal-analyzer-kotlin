package com.example.captive_portal_analyzer_kotlin.navigation

import HomeScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.example.captive_portal_analyzer_kotlin.my_screens.analysis.LandingScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.captive_portal_analyzer_kotlin.my_screens.analysis.AboutScreen
import com.example.captive_portal_analyzer_kotlin.my_screens.analysis.AnalysisScreen
import com.example.captive_portal_analyzer_kotlin.my_screens.analysis.ReportScreen
import com.example.captive_portal_analyzer_kotlin.room.AppDatabase
import com.example.captive_portal_analyzer_kotlin.room.OfflineCustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.OfflineWebpageContentRepository
import com.example.captive_portal_analyzer_kotlin.utils.NetworkSessionManager

sealed class Screen(val route: String) {
    object ManualConnect : Screen("manual_connect")
    object Landing : Screen("landing")
    object Analysis : Screen("analysis")
    object Report : Screen("report")
    object About : Screen("about")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    sessionManager: NetworkSessionManager,
    showToast: (Boolean, String?) -> Unit
) {
    val actions = remember(navController) { NavigationActions(navController) }

     val offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository by lazy {
        OfflineCustomWebViewRequestsRepository(AppDatabase.getDatabase(navController.context).customWebViewRequestDao())
    }

    val offlineWebpageContentRepository: OfflineWebpageContentRepository by lazy {
        OfflineWebpageContentRepository(AppDatabase.getDatabase(navController.context).webpageContentDao())
    }


    NavHost(navController = navController, startDestination = Screen.Landing.route) {
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
                navigateToReport = actions.navigateToReportScreen,
                navigateBack = actions.navigateBack,
                navigateToAbout = actions.navigateToAbout,
                sessionManager = sessionManager,
                showToast = showToast,
            )
        }
        composable(
            route = Screen.Report.route,
        ) {
                ReportScreen(
                    navigateBack = actions.navigateBack,
                    offlineCustomWebViewRequestsRepository = offlineCustomWebViewRequestsRepository,
                    offlineWebpageContentRepository = offlineWebpageContentRepository,
                    navigateToAbout = actions.navigateToAbout,
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

    val navigateToReportScreen: () -> Unit = {
        navController.navigate(Screen.Report.route)
    }

    val navigateBack: () -> Unit = {
        navController.popBackStack()
    }

}