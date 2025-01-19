package com.example.captive_portal_analyzer_kotlin.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.captive_portal_analyzer_kotlin.R
import kotlinx.coroutines.launch

// Data class for drawer menu items
data class DrawerMenuItem(
    val titleStringResource: Int,
    val route: String,
    val icon: ImageVector? = null
)

/**
 * Composable function for the main application scaffold with a navigation drawer.
 * This scaffold controls the navigation between screens using the NavHostController.
 *
 * @param navController The NavHostController used for navigation.
 * @param content The content to be displayed within the scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: ""

    // Handle back press when drawer is open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    val drawerMenuItems = remember(currentRoute) {
        listOf(
            DrawerMenuItem(titleStringResource = Screen.Welcome.titleStringResource, route = Screen.Welcome.route),
            DrawerMenuItem(titleStringResource = Screen.SessionList.titleStringResource, route = Screen.SessionList.route),
            DrawerMenuItem(titleStringResource = Screen.ManualConnect.titleStringResource, route = Screen.ManualConnect.route),
            DrawerMenuItem(titleStringResource = Screen.Settings.titleStringResource, route = Screen.Settings.route),
            DrawerMenuItem(titleStringResource = Screen.About.titleStringResource, route = Screen.About.route),
        ).filterNot { it.route == currentRoute }
    }

    val onNavigate = remember(navController) { { route: String ->
        scope.launch {
            drawerState.close()
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }}
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.menu),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Divider()
                drawerMenuItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(stringResource(item.titleStringResource)) },
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(
                                when (currentRoute) {
                                    Screen.Welcome.route -> Screen.Welcome.titleStringResource
                                    Screen.SessionList.route -> Screen.SessionList.titleStringResource
                                    Screen.ManualConnect.route -> Screen.ManualConnect.titleStringResource
                                    Screen.Settings.route -> Screen.Settings.titleStringResource
                                    Screen.About.route -> Screen.About.titleStringResource
                                    Screen.Session.route -> Screen.Session.titleStringResource
                                    Screen.WebPageContent.route -> Screen.WebPageContent.titleStringResource
                                    else -> R.string.app_name
                                }
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.menu)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                content()
            }
        }
    }
}
