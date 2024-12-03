package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.captive_portal_analyzer_kotlin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarWithMenu(openMenu: () -> Unit) {

    Column {
        // Toolbar
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.app_name)) },
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