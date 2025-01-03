package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Data class for drawer menu items
data class DrawerMenuItem(
    val titleStringResource: Int,
    val route: String,
    val icon: ImageVector? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavHostController,
    scope: CoroutineScope,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    var showMenu by remember { mutableStateOf(false) }

    val drawerMenuItems = remember(currentRoute) {
        listOf(
            DrawerMenuItem(titleStringResource = Screen.Welcome.titleStringResource, route = Screen.Welcome.route),
            DrawerMenuItem(titleStringResource = Screen.SessionList.titleStringResource, route = Screen.SessionList.route),
            DrawerMenuItem(titleStringResource = Screen.ManualConnect.titleStringResource, route = Screen.ManualConnect.route),
            DrawerMenuItem(titleStringResource = Screen.About.titleStringResource, route = Screen.About.route)
        ).filterNot { it.route == currentRoute }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
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
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
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
                            text = stringResource(R.string.app_name),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
//                        if (currentRoute == Screen.Analysis.route) {
//                        DropDownMenu(showMenu)
//                    }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                content()
            }
        }
    }
}

@Composable
private fun DropDownMenu(showMenu: Boolean) {
    var showMenu1 = showMenu
    Box {
        IconButton(onClick = { showMenu1 = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More options"
            )
        }
        DropdownMenu(
            expanded = showMenu1,
            onDismissRequest = { showMenu1 = false }
        ) {
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                    showMenu1 = false
                    // Add navigation or action here
                }
            )
            DropdownMenuItem(
                text = { Text("Help") },
                onClick = {
                    showMenu1 = false
                    // Add navigation or action here
                }
            )
            // Add more menu items as needed
        }
    }
}
