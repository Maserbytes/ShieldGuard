package com.shieldguard.app.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shieldguard.app.data.local.entity.CommunicationChannel
import com.shieldguard.app.ui.components.GlassButton
import com.shieldguard.app.ui.components.GlassCard
import com.shieldguard.app.ui.home.GlassLogCard
import com.shieldguard.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(viewModel: LogsViewModel) {
    val logs by viewModel.logs.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundObsidian)
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .background(Brush.radialGradient(listOf(CyberCyanGlow, Color.Transparent)))
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SECURITY AUDIT",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CyberCyan
                        )
                        Text(
                            text = "Blocked Threat Logs",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (logs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassFillStandard)
                                .border(1.dp, GlassBorderHairline, RoundedCornerShape(12.dp))
                                .clickable { showClearConfirmDialog = true }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Logs",
                                tint = GmailCoral,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                // Glassy Filter Pills Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassFilterPill(
                        title = "All Threats",
                        isSelected = selectedFilter == null,
                        onClick = { viewModel.setFilter(null) }
                    )
                    GlassFilterPill(
                        title = "Calls",
                        isSelected = selectedFilter == CommunicationChannel.CALL,
                        accentColor = CyberCyan,
                        onClick = { viewModel.setFilter(CommunicationChannel.CALL) }
                    )
                    GlassFilterPill(
                        title = "SMS",
                        isSelected = selectedFilter == CommunicationChannel.SMS,
                        accentColor = NeonEmerald,
                        onClick = { viewModel.setFilter(CommunicationChannel.SMS) }
                    )
                    GlassFilterPill(
                        title = "WhatsApp",
                        isSelected = selectedFilter == CommunicationChannel.WHATSAPP,
                        accentColor = WhatsAppNeon,
                        onClick = { viewModel.setFilter(CommunicationChannel.WHATSAPP) }
                    )
                    GlassFilterPill(
                        title = "Gmail",
                        isSelected = selectedFilter == CommunicationChannel.GMAIL,
                        accentColor = GmailCoral,
                        onClick = { viewModel.setFilter(CommunicationChannel.GMAIL) }
                    )
                    GlassFilterPill(
                        title = "Facebook",
                        isSelected = selectedFilter == CommunicationChannel.FACEBOOK,
                        accentColor = Color(0xFF1877F2),
                        onClick = { viewModel.setFilter(CommunicationChannel.FACEBOOK) }
                    )
                    GlassFilterPill(
                        title = "Browser",
                        isSelected = selectedFilter == CommunicationChannel.BROWSER,
                        accentColor = Color(0xFF38BDF8),
                        onClick = { viewModel.setFilter(CommunicationChannel.BROWSER) }
                    )
                    GlassFilterPill(
                        title = "App Store",
                        isSelected = selectedFilter == CommunicationChannel.APP_STORE,
                        accentColor = Color(0xFFF59E0B),
                        onClick = { viewModel.setFilter(CommunicationChannel.APP_STORE) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No threats recorded in this category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            GlassLogCard(log)
                        }
                    }
                }
            }

            if (showClearConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmDialog = false },
                    containerColor = BackgroundSurface,
                    title = {
                        Text("Clear All Audit Logs?", fontWeight = FontWeight.Bold, color = TextPrimary)
                    },
                    text = {
                        Text(
                            "This will permanently erase all local records of blocked calls, SMS messages, and notification alerts from the on-device database.",
                            color = TextSecondary
                        )
                    },
                    confirmButton = {
                        GlassButton(
                            onClick = {
                                viewModel.clearAllLogs()
                                showClearConfirmDialog = false
                            },
                            containerColor = GmailCoral,
                            contentColor = TextPrimary,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Permanently Clear", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirmDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GlassFilterPill(
    title: String,
    isSelected: Boolean,
    accentColor: Color = CyberCyan,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else GlassFillStandard)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(accentColor, accentColor.copy(alpha = 0.4f))
                    } else {
                        listOf(GlassBorderSpecular, GlassBorderHairline)
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
