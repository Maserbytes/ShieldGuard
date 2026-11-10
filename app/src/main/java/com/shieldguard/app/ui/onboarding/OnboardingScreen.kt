package com.shieldguard.app.ui.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shieldguard.app.ui.components.GlassButton
import com.shieldguard.app.ui.components.GlassCard
import com.shieldguard.app.ui.theme.*

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkCurrentPermissions()
    }

    LaunchedEffect(Unit) {
        viewModel.checkCurrentPermissions()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundObsidian)
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-80).dp)
                .background(Brush.radialGradient(listOf(CyberCyanGlow, Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header & Glowing Progress Step Bars
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Shield Icon with Glass Glow
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0x3300E5FF), Color(0x1000E5FF)))
                        )
                        .border(1.dp, GlassBorderSpecular, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Shield",
                        tint = CyberCyan,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ShieldGuard Setup",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Step Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..5) {
                        val isPassed = i <= state.currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(if (isPassed) CyberCyan else Color(0x20FFFFFF))
                        )
                    }
                }
            }

            // Center Dynamic Step Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                when (state.currentStep) {
                    1 -> WelcomeGlassStep()
                    2 -> StepGlassCard(
                        icon = Icons.Default.Phone,
                        title = "Call Screening Role",
                        description = "Intercepts automated robocalls and commercial telemarketing lines silently before your phone rings.",
                        isGranted = state.isPhoneRoleGranted,
                        actionLabel = if (state.isPhoneRoleGranted) "Phone Role Active" else "Grant Phone Role",
                        accentColor = CyberCyan,
                        onAction = { requestPhoneRole(context, roleLauncher) }
                    )
                    3 -> StepGlassCard(
                        icon = Icons.Default.Message,
                        title = "Default SMS Filter Role",
                        description = "Quarantines phishing links and fake lottery SMS while guaranteeing bank OTPs arrive instantly.",
                        isGranted = state.isSmsRoleGranted,
                        actionLabel = if (state.isSmsRoleGranted) "SMS Filter Active" else "Set as Default SMS App",
                        accentColor = NeonEmerald,
                        onAction = { requestSmsRole(context, roleLauncher) }
                    )
                    4 -> StepGlassCard(
                        icon = Icons.Default.Notifications,
                        title = "Notification Interceptor",
                        description = "Permits ShieldGuard to catch and silently dismiss promotional push spam from WhatsApp and Gmail.",
                        isGranted = state.isNotificationAccessGranted,
                        actionLabel = if (state.isNotificationAccessGranted) "Notification Access Granted" else "Open Notification Settings",
                        accentColor = WhatsAppNeon,
                        onAction = { openNotificationSettings(context) }
                    )
                    5 -> StepGlassCard(
                        icon = Icons.Default.BatteryChargingFull,
                        title = "Background Protection",
                        description = "Prevents aggressive Android memory cleaners from terminating your shield while your phone is locked.",
                        isGranted = state.isBatteryOptimizedExempted,
                        actionLabel = if (state.isBatteryOptimizedExempted) "Exemption Granted" else "Disable Optimization",
                        accentColor = ElectricIndigo,
                        onAction = { requestBatteryExemption(context) }
                    )
                }
            }

            // Bottom Flow Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.currentStep > 1) {
                    TextButton(
                        onClick = { viewModel.previousStep() },
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Back", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                GlassButton(
                    onClick = {
                        if (state.currentStep == 5) {
                            onComplete()
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    modifier = Modifier.width(160.dp)
                ) {
                    Text(
                        text = if (state.currentStep == 5) "Finish & Protect" else "Continue",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeGlassStep() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = GlassFillElevated
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = "Pure Privacy. Zero Cloud Leaks.",
                style = MaterialTheme.typography.headlineMedium,
                color = NeonEmerald,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ShieldGuard acts as an impenetrable local firewall for your Android device:\n\n" +
                        "• Drops robocalls & 140 telemarketers silently\n" +
                        "• Quarantines scam SMS & phishing URLs\n" +
                        "• Silences marketing alerts from WhatsApp & Gmail\n" +
                        "• 100% On-Device: Your contacts never touch a remote server.",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun StepGlassCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    accentColor: Color,
    onAction: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = GlassFillElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isGranted) NeonEmerald.copy(alpha = 0.15f) else accentColor.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (isGranted) NeonEmerald.copy(alpha = 0.5f) else accentColor.copy(alpha = 0.5f),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else icon,
                    contentDescription = title,
                    tint = if (isGranted) NeonEmerald else accentColor,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            GlassButton(
                onClick = onAction,
                containerColor = if (isGranted) NeonEmerald else accentColor,
                contentColor = BackgroundObsidian,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun requestPhoneRole(context: Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            launcher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        } else if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            launcher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        }
    }
}

private fun requestSmsRole(context: Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
            launcher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
        }
    } else {
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        context.startActivity(intent)
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    } else {
        Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
    }
    context.startActivity(intent)
}

private fun requestBatteryExemption(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(fallback)
        }
    }
}
