package com.example.captive_portal_analyzer_kotlin.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.captive_portal_analyzer_kotlin.R


data class MenuItem(
    val iconPath: Int,           // Resource ID for the icon
    val itemName: String,        // Name of the menu item
    val onClick: () -> Unit       // Action to perform when the item is clicked
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarWithMenu(
    title: String?,
    menuItems: List<MenuItem> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            navigationIcon = {
                if (menuItems.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            menuItems.forEach { menuItem ->
                                DropdownMenuItem(
                                    text = { Text(menuItem.itemName) },
                                    onClick = {
                                        expanded = false
                                        menuItem.onClick()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = menuItem.iconPath),
                                            contentDescription = menuItem.itemName,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title ?: stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )
    }
}
