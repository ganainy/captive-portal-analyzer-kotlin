package com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service.ConnectionDescriptor.Status.STATUS_ACTIVE
import com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service.ConnectionDescriptor.Status.STATUS_CLOSED
import com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service.ConnectionDescriptor.Status.STATUS_INVALID
import com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service.ConnectionDescriptor.Status.STATUS_UNREACHABLE
import com.example.captive_portal_analyzer_kotlin.screens.pcap_setup.Utils

@Composable
fun ConnectionListItem(
    connection: ConnectionDescriptor,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val appsResolver = AppsResolver(context) //todo: move to caller
    val app: AppDescriptor? = appsResolver.getAppByUid(connection.uid, 0)
    val appName = app?.getName() ?: connection.uid.toString()

    val isJsInjected =  (connection.js_injected_scripts != null && connection.js_injected_scripts.isNotEmpty())
    val isRedirected =connection.isPortMappingApplied && !connection.is_blocked
    val isBlocked = connection.is_blocked
    val lastSeen = Utils.formatEpochShort(context, connection.last_seen / 1000)


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon on the left
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.analyzer),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(5.dp))

        // Main content area
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            // App name and indicators row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Indicators (JavaScript, Redirected, Blacklisted, Blocked)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isJsInjected) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_javascript),
                                contentDescription = stringResource(id = R.string.injected),
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }

                        if (isRedirected) {
                            Icon(
                                painter = painterResource(id = R.drawable.reply),
                                contentDescription = stringResource(id = R.string.redirected),
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }

                        if (connection.isBlacklisted()) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_skull),
                                contentDescription = stringResource(id = R.string.malicious_connection_filter),
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }

                        if (isBlocked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_block),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Status indicator
                Text(
                    text = connection.getStatusLabel(context),
                    fontSize = 12.sp,
                    color = getStatusColor(connection.getStatus()),
                    textAlign = TextAlign.End
                )
            }

            // Second row with protocol and last seen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Decryption status icon
                    if (!connection.isCleartext()) {
                        Icon(
                            painter = painterResource(
                                id = if (connection.isDecrypted())
                                    R.drawable.ic_lock_open
                                else
                                    R.drawable.ic_lock
                            ),
                            contentDescription = null,
                            modifier = Modifier.height(16.dp),
                            tint = if (connection.isDecrypted())
                                Color(0xFF28BC36) // ok color
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Protocol and port
                    Text(
                        text = "${connection.l7proto}, ${connection.dst_port}",
                        fontSize = 13.sp
                    )
                }

                // Last seen time
                Text(
                    text = lastSeen,
                    fontSize = 12.sp
                )
            }

            // Third row with remote host and traffic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = connection.info.takeIf { !it.isNullOrEmpty() } ?: connection.dst_ip,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Traffic info
                Text(
                    text = "${connection.sent_bytes + connection.rcvd_bytes} bytes",
                    fontSize = 14.sp,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// Helper function to determine status color
@Composable
fun getStatusColor(status: ConnectionDescriptor.Status): Color {
    val context = LocalContext.current

    val colorResId = when {
        // Active connection
         status == STATUS_ACTIVE ->
            R.color.statusOpen

        // Closed/Reset connection
        status == STATUS_CLOSED  ->
            R.color.statusClosed

        // Warning states (error, socket error, unreachable)
        status == STATUS_INVALID || status == STATUS_UNREACHABLE ->
            R.color.warning
        // Other error states
        else -> R.color.statusError
    }

    return androidx.compose.ui.res.colorResource(id = colorResId)
}


@Preview(showBackground = true)
@Composable
fun ConnectionListItemPreviewActive() {
    MaterialTheme {
        val mockConnection = createMockConnection(ConnectionDescriptor.Status.STATUS_ACTIVE)
        ConnectionListItem(connection = mockConnection, onClick = {})
    }
}


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