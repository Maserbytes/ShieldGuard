package com.shieldguard.app.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.shieldguard.app.ui.home.HomeScreen
import com.shieldguard.app.ui.home.HomeViewModel
import com.shieldguard.app.ui.logs.LogsScreen
import com.shieldguard.app.ui.logs.LogsViewModel
import com.shieldguard.app.ui.navigation.Screen
import com.shieldguard.app.ui.onboarding.OnboardingScreen
import com.shieldguard.app.ui.onboarding.OnboardingViewModel
import com.shieldguard.app.ui.rules.RulesScreen
import com.shieldguard.app.ui.rules.RulesViewModel
import com.shieldguard.app.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("shield_guard_prefs", Context.MODE_PRIVATE)
        val isOnboarded = prefs.getBoolean("is_onboarded", false)

        setContent {
            ShieldGuardTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val startDestination = if (isOnboarded) Screen.Home.route else Screen.Onboarding.route

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundObsidian)
                ) {
                    // Main Content NavHost
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Screen.Onboarding.route) {
                            val onboardingViewModel: OnboardingViewModel = viewModel()
                            OnboardingScreen(
                                viewModel = onboardingViewModel,
                                onComplete = {
                                    prefs.edit().putBoolean("is_onboarded", true).apply()
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Home.route) {
                            val homeViewModel: HomeViewModel = viewModel()
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToRules = { navController.navigate(Screen.Rules.route) },
                                onNavigateToLogs = { navController.navigate(Screen.Logs.route) }
                            )
                        }

                        composable(Screen.Rules.route) {
                            val rulesViewModel: RulesViewModel = viewModel()
                            RulesScreen(viewModel = rulesViewModel)
                        }

                        composable(Screen.Logs.route) {
                            val logsViewModel: LogsViewModel = viewModel()
                            LogsScreen(viewModel = logsViewModel)
                        }
                    }

                    // Floating Glass Dock (Bottom Navigation)
                    if (currentRoute != Screen.Onboarding.route) {
                        FloatingGlassDock(
                            currentRoute = currentRoute,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Apple-style floating translucent glass dock with specular reflection.
 */
@Composable
private fun FloatingGlassDock(
    currentRoute: String?,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = Color(0x60000000)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD9161E2E), // 85% opacity dark slate
                        Color(0xE60A0E17)  // 90% opacity deep obsidian
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassBorderSpecular, // Specular light catching top rim
                        GlassBorderHairline,
                        Color(0x05FFFFFF)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(
                icon = Icons.Default.Shield,
                label = "Shield",
                isSelected = currentRoute == Screen.Home.route,
                onClick = { onNavigate(Screen.Home.route) }
            )
            DockItem(
                icon = Icons.Default.Rule,
                label = "Rules",
                isSelected = currentRoute == Screen.Rules.route,
                onClick = { onNavigate(Screen.Rules.route) }
            )
            DockItem(
                icon = Icons.Default.History,
                label = "Logs",
                isSelected = currentRoute == Screen.Logs.route,
                onClick = { onNavigate(Screen.Logs.route) }
            )
        }
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) CyberCyan else TextSecondary,
        label = "iconColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "dockScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(CyberCyan.copy(alpha = 0.12f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
