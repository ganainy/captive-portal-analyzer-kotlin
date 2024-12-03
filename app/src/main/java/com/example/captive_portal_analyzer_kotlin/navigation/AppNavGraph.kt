package com.example.captive_portal_analyzer_kotlin.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import com.example.captive_portal_analyzer_kotlin.repository.DataRepository
import com.example.captive_portal_analyzer_kotlin.screens.LandingScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Analysis : Screen("analysis")


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

    val navigateBack: () -> Unit = {
        navController.popBackStack()
    }

}