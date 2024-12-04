package com.example.captive_portal_analyzer_kotlin.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import com.example.captive_portal_analyzer_kotlin.repository.DataRepository
import com.example.captive_portal_analyzer_kotlin.my_screens.analysis.LandingScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.captive_portal_analyzer_kotlin.dataclasses.CaptivePortalReport
import com.example.captive_portal_analyzer_kotlin.my_screens.analysis.AnalysisScreen
import com.example.captive_portal_analyzer_kotlin.my_screens.analysis.ReportScreen
import com.google.gson.Gson

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Analysis : Screen("analysis")
    object Report : Screen("report")


}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AppNavGraph(navController: NavHostController) {
    val dataRepository = DataRepository()
    val actions = remember(navController) { NavigationActions(navController) }
    NavHost(navController = navController, startDestination = Screen.Landing.route) {
        composable(route = Screen.Landing.route) {
            LandingScreen(
                dataRepository,
                actions.navigateToAnalysisScreen,
                actions.navigateBack
            )
        }
        composable(route = Screen.Analysis.route) {
            AnalysisScreen(
                dataRepository = dataRepository,
                navigateToReport = { reportData ->
                    val jsonData = Gson().toJson(reportData)  // Serialize the object to JSON string
                    navController.navigate("${Screen.Report.route}?reportData=$jsonData")
                },
                navigateBack = actions.navigateBack
            )
        }
        composable(
            route = Screen.Report.route + "?reportData={reportData}",
            arguments = listOf(
                navArgument("reportData") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val reportDataJson = backStackEntry.arguments?.getString("reportData")
            val captivePortalReport = reportDataJson?.let {
                Gson().fromJson(it, CaptivePortalReport::class.java)
            }

            captivePortalReport?.let {
                ReportScreen(
                    captivePortalReport = it,
                    dataRepository = dataRepository,
                    navigateBack = actions.navigateBack
                )
            }
        }

    }
}

class NavigationActions(private val navController: NavHostController) {
    /*val navigateToSignUp: () -> Unit = {
        navController.navigate(Screen.SignUp.route) {
            popUpTo(Screen.SignUp.route) { inclusive = true }
        }
    }*/
    val navigateToAnalysisScreen: () -> Unit = {
        navController.navigate(Screen.Analysis.route)
    }

   /* val navigateToReportScreen: (CaptivePortalReport) -> Unit = { reportData ->
        val jsonData = Gson().toJson(reportData)
        navController.navigate("${Screen.Report.route}?reportData=$jsonData")
    }*/

    val navigateBack: () -> Unit = {
        navController.popBackStack()
    }

}