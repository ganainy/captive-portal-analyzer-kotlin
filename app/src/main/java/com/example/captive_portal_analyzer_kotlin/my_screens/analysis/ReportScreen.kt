package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.components.CustomProgressIndicator
import com.example.captive_portal_analyzer_kotlin.components.CustomSnackBar
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.dataclasses.CaptivePortalReport
import com.example.captive_portal_analyzer_kotlin.repository.IDataRepository
import kotlinx.coroutines.launch

@Composable
fun ReportScreen(
    dataRepository: IDataRepository,
    navigateBack: () -> Unit,
    captivePortalReport: CaptivePortalReport
) {
    val viewModel: ReportViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()


    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()


    Scaffold(
        topBar = {
            ToolbarWithMenu(
                openMenu = {
                    scope.launch {
                        drawerState.open() // Open the drawer when menu icon is clicked
                    }
                }
            )
        },
    ) { paddingValues ->

    Text("Report Screen")
    //Text(captivePortalReport.toString())
        Text("Report Screen")
    }



    when (uiState) {
        is ReportUiState.Initial -> {
            // Show initial state (empty form)
        }

        is ReportUiState.Loading -> {
            // Show loading indicator
            CustomProgressIndicator()
        }

        is ReportUiState.Error -> {
            // Show error message
            CustomSnackBar(message = stringResource((uiState as LandingUiState.Error).messageStringResource)) {
            }
        }


    }

    }




