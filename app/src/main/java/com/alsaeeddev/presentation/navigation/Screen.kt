package com.alsaeeddev.presentation.navigation

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object Settings : Screen("settings", "Settings")
    data object Diagnostics : Screen("diagnostics", "Diagnostics")
    data object Simulator : Screen("simulator", "Call Simulator")
    data object Onboarding : Screen("onboarding", "Setup Protection")
    data object IncomingCall : Screen("incoming_call", "Incoming Call")
}
