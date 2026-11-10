package com.shieldguard.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shieldguard.app.data.local.entity.BlockLogEntity
import com.shieldguard.app.data.local.entity.CommunicationChannel
import com.shieldguard.app.ui.components.GlassCard
import com.shieldguard.app.ui.components.GlassySwitch
import com.shieldguard.app.ui.components.StatusPulseIndicator
import com.shieldguard.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToRules: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val totalBlocked by viewModel.totalBlocked.collectAsState()
    val callBlocked by viewModel.callBlocked.collectAsState()
    val smsBlocked by viewModel.smsBlocked.collectAsState()
    val whatsappBlocked by viewModel.whatsappBlocked.collectAsState()
    val gmailBlocked by viewModel.gmailBlocked.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val isShieldActive by viewModel.isShieldActive.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundObsidian)
    ) {
        // Subtle ambient background gradient glow
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = (-80).dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isShieldActive) EmeraldGlow else Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = 150.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isShieldActive) CyberCyanGlow else Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // 1. Top Brand Header & Live State Pill
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SHIELDGUARD",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CyberCyan
                        )
                        Text(
                            text = "Unified Firewall",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Frosted status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassFillStandard)
                            .border(1.dp, GlassBorderSpecular, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPulseIndicator(isActive = isShieldActive, size = 8.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isShieldActive) "ACTIVE" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isShieldActive) NeonEmerald else TextTertiary
                            )
                        }
                    }
                }
            }

            // 2. Master Shield Hero Card with Glassy Switch
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = GlassFillElevated
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Glowing Frosted Shield Emblem
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                if (isShieldActive) Color(0x3310B981) else Color(0x1AFFFFFF),
                                                Color(0x0DFFFFFF)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                if (isShieldActive) Color(0x8010B981) else Color(0x30FFFFFF),
                                                Color(0x10FFFFFF)
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shield",
                                    tint = if (isShieldActive) NeonEmerald else TextTertiary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = if (isShieldActive) "Device Fully Guarded" else "Firewall Paused",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isShieldActive) "Filtering Calls, SMS & Alerts" else "Tap toggle to resume protection",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Custom Glassy UI Toggle
                        GlassySwitch(
                            checked = isShieldActive,
                            onCheckedChange = { viewModel.toggleShield(it) }
                        )
                    }
                }
            }

            // 3. Threat Defense Counter Hero
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = GlassFillStandard
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$totalBlocked",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "THREATS NEUTRALIZED",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x10FFFFFF))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zero Cloud Leaks • 100% Local On-Device",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 4. Protected Channels (2x2 Glass Tile Grid)
            item {
                Text(
                    text = "Channel Defense Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassChannelTile(
                        modifier = Modifier.weight(1f),
                        title = "Calls",
                        sub = "Robo & 140 series",
                        count = callBlocked,
                        icon = Icons.Default.Phone,
                        accentColor = CyberCyan
                    )
                    GlassChannelTile(
                        modifier = Modifier.weight(1f),
                        title = "SMS",
                        sub = "Scams & loan bots",
                        count = smsBlocked,
                        icon = Icons.Default.Message,
                        accentColor = NeonEmerald
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassChannelTile(
                        modifier = Modifier.weight(1f),
                        title = "WhatsApp",
                        sub = "Promos dismissed",
                        count = whatsappBlocked,
                        icon = Icons.Default.Chat,
                        accentColor = WhatsAppNeon
                    )
                    GlassChannelTile(
                        modifier = Modifier.weight(1f),
                        title = "Gmail",
                        sub = "Marketing muted",
                        count = gmailBlocked,
                        icon = Icons.Default.Mail,
                        accentColor = GmailCoral
                    )
                }
            }

            // 5. Recent Activity Feed Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Interceptions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassFillStandard)
                            .border(1.dp, GlassBorderHairline, RoundedCornerShape(12.dp))
                            .clickable { onNavigateToLogs() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Feed Items
            if (recentLogs.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All channels clean. No threats detected.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(recentLogs.take(5)) { log ->
                    GlassLogCard(log)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GlassChannelTile(
    modifier: Modifier = Modifier,
    title: String,
    sub: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color
) {
    GlassCard(
        modifier = modifier,
        backgroundColor = GlassFillSubtle,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun GlassLogCard(log: BlockLogEntity) {
    val dateFormat = remember { SimpleDateFormat("HH:mm • MMM dd", Locale.getDefault()) }
    val (icon, color) = when (log.channel) {
        CommunicationChannel.CALL -> Icons.Default.Phone to CyberCyan
        CommunicationChannel.SMS -> Icons.Default.Message to NeonEmerald
        CommunicationChannel.WHATSAPP -> Icons.Default.Chat to WhatsAppNeon
        CommunicationChannel.GMAIL -> Icons.Default.Mail to GmailCoral
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = GlassFillStandard,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(color.copy(alpha = 0.12f))
                    .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.senderOrCaller,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = dateFormat.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = log.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontWeight = FontWeight.Medium
                )

                if (log.contentSnippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = log.contentSnippet,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
