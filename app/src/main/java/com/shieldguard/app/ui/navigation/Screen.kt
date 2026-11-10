package com.shieldguard.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Onboarding : Screen("onboarding", "Setup Wizard")
    object Home : Screen("home", "Shield Status")
    object Rules : Screen("rules", "Filter Rules")
    object Logs : Screen("logs", "Blocked History")
}
