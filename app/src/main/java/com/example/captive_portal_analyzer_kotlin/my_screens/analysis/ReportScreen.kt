package com.example.captive_portal_analyzer_kotlin.my_screens.analysis

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.CustomProgressIndicator
import com.example.captive_portal_analyzer_kotlin.components.CustomSnackBar
import com.example.captive_portal_analyzer_kotlin.components.MenuItem
import com.example.captive_portal_analyzer_kotlin.components.ToolbarWithMenu
import com.example.captive_portal_analyzer_kotlin.room.OfflineCustomWebViewRequestsRepository
import com.example.captive_portal_analyzer_kotlin.room.OfflineWebpageContentRepository
import java.text.SimpleDateFormat

@Composable
fun ReportScreen(
    navigateBack: () -> Unit,
    offlineCustomWebViewRequestsRepository: OfflineCustomWebViewRequestsRepository,
    navigateToAbout: () -> Unit,
    offlineWebpageContentRepository: OfflineWebpageContentRepository
) {
    val viewModel: ReportViewModel = viewModel(
        factory = ReportViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            offlineCustomWebViewRequestsRepository = offlineCustomWebViewRequestsRepository,
            offlineWebpageContentRepository = offlineWebpageContentRepository
        )
    )
    val uiState by viewModel.uiState.collectAsState()


    Scaffold(
        topBar = {
                ToolbarWithMenu(
                    title = stringResource(id = R.string.analysis_screen_title),
                    menuItems = listOf(
                        MenuItem(
                            iconPath = R.drawable.about,
                            itemName = stringResource(id = R.string.about),
                            onClick = {
                                navigateToAbout()
                            }
                        ),

                        )
                )
        },
    ) { paddingValues ->

        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

        }
    }



    when (uiState) {

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

@Composable
fun FieldDisplay(label: String, value: String?) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(value ?: "No Data", style = MaterialTheme.typography.bodySmall)
    }
}


@Composable
fun KeyValueDisplay(key: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$key: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.3f) // Adjust weight to control alignment
        )
        Text(
            text = value ?: "N/A",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.7f)
        )
    }
}




@Composable
fun KeyValueDisplayWithTruncation(key: String, value: String?, maxLength: Int = 20) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$key: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.3f)
        )

        if (value != null && value.length > maxLength && !isExpanded) {
            Text(
                text = "${value.take(maxLength)}...",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.5f)
            )
            TextButton(
                onClick = { isExpanded = true },
                modifier = Modifier.weight(0.2f)
            ) {
                Text(
                    text = stringResource(R.string.see_more),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Text(
                text = value ?: "N/A",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.7f)
            )
        }
    }
}



