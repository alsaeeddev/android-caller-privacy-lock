package com.alsaeeddev

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alsaeeddev.data.repository.SettingsRepository
import com.alsaeeddev.presentation.call.IncomingCallActivity
import com.alsaeeddev.presentation.dashboard.DashboardScreen
import com.alsaeeddev.presentation.dashboard.DashboardViewModel
import com.alsaeeddev.presentation.diagnostics.DiagnosticsScreen
import com.alsaeeddev.presentation.diagnostics.DiagnosticsViewModel
import com.alsaeeddev.presentation.navigation.Screen
import com.alsaeeddev.presentation.onboarding.OnboardingScreen
import com.alsaeeddev.presentation.onboarding.OnboardingViewModel
import com.alsaeeddev.presentation.settings.SettingsScreen
import com.alsaeeddev.presentation.settings.SettingsViewModel
import com.alsaeeddev.presentation.simulation.CallSimulatorScreen
import com.alsaeeddev.presentation.simulation.SimulationViewModel
import com.alsaeeddev.ui.theme.DeepObsidian
import com.alsaeeddev.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions granted callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install AndroidX SplashScreen for smooth backward-compatible launch on Android 7 to 17+
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Request required runtime permissions asynchronously without blocking the first frame render
        val permissionsToRequest = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())

        val initialSettings = settingsRepository.getCurrentSettings()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepObsidian
                ) {
                    val settings by settingsRepository.settingsFlow.collectAsState(initial = initialSettings)
                    val startDestination = if (settings.isOnboardingCompleted) {
                        Screen.Dashboard.route
                    } else {
                        Screen.Onboarding.route
                    }
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val launchIncomingCallActivity = {
        val intent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            val onboardingVm: OnboardingViewModel = viewModel()
            OnboardingScreen(
                viewModel = onboardingVm,
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onLaunchTestCall = launchIncomingCallActivity
            )
        }

        composable(Screen.Dashboard.route) {
            val dashboardVm: DashboardViewModel = viewModel()
            DashboardScreen(
                viewModel = dashboardVm,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                onNavigateToSimulator = { navController.navigate(Screen.Simulator.route) },
                onNavigateToIncomingCall = launchIncomingCallActivity
            )
        }

        composable(Screen.Settings.route) {
            val settingsVm: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = settingsVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Diagnostics.route) {
            val diagnosticsVm: DiagnosticsViewModel = viewModel()
            DiagnosticsScreen(
                viewModel = diagnosticsVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Simulator.route) {
            val simulationVm: SimulationViewModel = viewModel()
            CallSimulatorScreen(
                viewModel = simulationVm,
                onNavigateBack = { navController.popBackStack() },
                onLaunchCall = launchIncomingCallActivity
            )
        }

        composable(Screen.IncomingCall.route) {
            launchIncomingCallActivity()
        }
    }
}
