package com.example.captive_portal_analyzer_kotlin.screens.analysis


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service.ConnectionDescriptor
import com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service.ConnectionListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    connections: List<ConnectionDescriptor> = emptyList(),
    olderConnectionsCount: Int = 0,
    showOlderConnectionsNotice: Boolean = false,
    showActiveFilter: Boolean = false,
    filterTags: List<String> = emptyList(),
    selectedFilterTag: String? = null,
    onFilterTagSelected: (String) -> Unit = {},
    onConnectionItemClick: (ConnectionDescriptor) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Older connections notice
            AnimatedVisibility(visible = showOlderConnectionsNotice && olderConnectionsCount > 0) {
                Text(
                    text = "$olderConnectionsCount older connections not shown",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    fontSize = 15.sp,
                    fontStyle = FontStyle.Italic
                )
            }

            // Filter chips
            AnimatedVisibility(visible = showActiveFilter) {
                FilterChipGroup(
                    filterTags = filterTags,
                    selectedFilterTag = selectedFilterTag,
                    onFilterTagSelected = onFilterTagSelected,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .wrapContentSize()
                )
            }

            // Either "No connections" message or the list
            if (connections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = stringResource(R.string.start_capture_first),
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = listState
                ) {
                    items(connections) { connection ->
                        ConnectionListItem(
                            connection = connection,
                            onClick = { onConnectionItemClick(connection) }
                        )
                    }
                }
            }
        }

        // Scroll down FAB
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = colorResource(id = R.color.colorAccent),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                contentDescription = "Scroll to bottom",
                tint = Color.Unspecified
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipGroup(
    filterTags: List<String>,
    selectedFilterTag: String?,
    onFilterTagSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
        ) {
            filterTags.forEach { tag ->
                FilterChip(
                    selected = tag == selectedFilterTag,
                    onClick = { onFilterTagSelected(tag) },
                    label = { Text(text = tag) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ConnectionsScreenPreview() {
    val mockConnections = listOf(
        createMockConnection(ConnectionDescriptor.Status.STATUS_ACTIVE),
        createMockConnection(ConnectionDescriptor.Status.STATUS_CLOSED)
    )

    val mockFilterTags = listOf("All", "TCP", "UDP", "HTTPS")

    ConnectionsScreen(
        connections = mockConnections,
        olderConnectionsCount = 5,
        showOlderConnectionsNotice = true,
        showActiveFilter = true,
        filterTags = mockFilterTags,
        selectedFilterTag = "All",
    )
}

@Preview(showBackground = true)
@Composable
fun ConnectionsScreenPreview_Empty() {
    val mockConnections = emptyList<ConnectionDescriptor>()

    val mockFilterTags = listOf("All", "TCP", "UDP", "HTTPS")

    ConnectionsScreen(
        connections = mockConnections,
        olderConnectionsCount = 0,
        showOlderConnectionsNotice = false,
        showActiveFilter = false,
        filterTags = mockFilterTags,
        selectedFilterTag = "All",
    )
}

// Reuse the existing mockConnection function or define it here if needed
private fun createMockConnection(status: ConnectionDescriptor.Status): ConnectionDescriptor {
    return ConnectionDescriptor(
        1,
        4,
        6,
        "",
        "192.168.1.1",
        "",
        0,
        443,
        0,
        10001,
        0,
        false,
        System.currentTimeMillis()
    ).apply {
        l7proto = "HTTPS"
        info = "example.com"
        sent_bytes = 1024
        rcvd_bytes = 2048
        is_blocked = status == ConnectionDescriptor.Status.STATUS_CLOSED
    }
    }