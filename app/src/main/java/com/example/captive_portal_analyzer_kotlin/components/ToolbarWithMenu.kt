package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarWithMenu(openMenu: () -> Unit,title:String?) {

    Column {
        // Toolbar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    /*Icon(
                        painter = painterResource(id = R.drawable.wifi_icon), // Add a custom icon
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp) // Space between icon and text
                    )*/
                    Text(
                        text = title ?: stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, // Make the title bold
                            //color = MaterialTheme.colorScheme.primary // Use theme's primary color
                        )
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = openMenu) {
                    Icon(
                        painter = painterResource(id = R.drawable.hamburger),
                        contentDescription = "Menu"
                    )
                }
            },
            actions = {
                // Add more actions here if needed
            }
        )


    }
}