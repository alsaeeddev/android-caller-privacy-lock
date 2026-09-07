package com.alsaeeddev.presentation.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alsaeeddev.data.model.ProtectionMode
import com.alsaeeddev.presentation.components.PrivacyShieldHero
import com.alsaeeddev.telecom.RoleHelper
import com.alsaeeddev.ui.theme.AmberWarning
import com.alsaeeddev.ui.theme.CyberCyan
import com.alsaeeddev.ui.theme.DeepObsidian
import com.alsaeeddev.ui.theme.EmeraldProtect
import com.alsaeeddev.ui.theme.SlateCard
import com.alsaeeddev.ui.theme.SlateCardBorder
import com.alsaeeddev.ui.theme.SlateCardSelected
import com.alsaeeddev.ui.theme.TextSecondaryDark

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToSimulator: () -> Unit,
    onNavigateToIncomingCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()

    val dialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemStatus()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSystemStatus()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Private Caller ID",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Zero-Exposure Incoming Call Shield",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SlateCard)
                        .border(1.dp, SlateCardBorder, CircleShape)
                        .testTag("action_open_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Active Call Banner (if currently in call or test call)
        if (activeCall != null) {
            item {
                ActiveCallAlertCard(
                    onOpenCallScreen = onNavigateToIncomingCall
                )
            }
        }

        // Main Privacy Shield Hero
        item {
            PrivacyShieldHero(
                isProtectionEnabled = settings.isProtectionEnabled,
                isDefaultDialer = uiState.isDefaultDialer,
                protectionMode = settings.mode,
                onToggleProtection = { viewModel.toggleProtection(it) }
            )
        }

        // Default Phone App Warning (if not granted)
        if (!uiState.isDefaultDialer) {
            item {
                DefaultDialerRequirementCard(
                    onSetDefaultDialer = {
                        val roleHelper = RoleHelper(context)
                        val intent = roleHelper.createRequestDialerRoleIntent()
                        if (intent != null) {
                            try {
                                dialerLauncher.launch(intent)
                            } catch (e: Exception) {
                                try {
                                    context.startActivity(intent)
                                } catch (e2: Exception) {
                                    // ignore fallback failure
                                }
                            }
                        }
                    }
                )
            }
        }

        // Quick Protection Mode Selector
        item {
            Column {
                Text(
                    text = "PROTECTION MODE",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProtectionModePill(
                        title = "Maximum",
                        isSelected = settings.mode == ProtectionMode.MAX_PRIVACY,
                        onClick = { viewModel.setProtectionMode(ProtectionMode.MAX_PRIVACY) },
                        modifier = Modifier.weight(1f)
                    )
                    ProtectionModePill(
                        title = "Lock Screen",
                        isSelected = settings.mode == ProtectionMode.LOCK_SCREEN_PRIVACY,
                        onClick = { viewModel.setProtectionMode(ProtectionMode.LOCK_SCREEN_PRIVACY) },
                        modifier = Modifier.weight(1f)
                    )
                    ProtectionModePill(
                        title = "Always Private",
                        isSelected = settings.mode == ProtectionMode.ALWAYS_PRIVATE,
                        onClick = { viewModel.setProtectionMode(ProtectionMode.ALWAYS_PRIVATE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Security Layers Status Grid
        item {
            Column {
                Text(
                    text = "SECURITY LAYERS",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecurityLayerRow(
                        icon = Icons.Default.Security,
                        title = "Telecom InCall Stack",
                        subtitle = if (uiState.isDefaultDialer) "Bound as primary dialer" else "System dialer active",
                        isPassed = uiState.isDefaultDialer
                    )

                    SecurityLayerRow(
                        icon = Icons.Default.VpnKey,
                        title = "Hardware Keystore Encryption",
                        subtitle = "AES-256 GCM in Secure Element (TEE)",
                        isPassed = true
                    )

                    SecurityLayerRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Authentication",
                        subtitle = if (uiState.isDeviceSecure) "Strong biometrics + Device Lock" else "No device credential set",
                        isPassed = uiState.isDeviceSecure
                    )

                    SecurityLayerRow(
                        icon = Icons.Default.Notifications,
                        title = "Notification Privacy",
                        subtitle = "Masked payloads on lock-screen",
                        isPassed = true
                    )
                }
            }
        }

        // Action Buttons: Run Diagnostics & Simulate Call
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onNavigateToSimulator,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("action_open_simulator"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = DeepObsidian
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Simulate Incoming Call Test",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                        .clickable(onClick = onNavigateToDiagnostics)
                        .testTag("action_open_diagnostics"),
                    color = SlateCard
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "System Privacy Diagnostics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Inspect OEM compatibility & hardware stack",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondaryDark
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActiveCallAlertCard(
    onOpenCallScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, CyberCyan, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenCallScreen),
        color = SlateCardSelected
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyberCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = DeepObsidian,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Active Protected Call",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Text(
                        text = "Tap to open incoming call UI",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CyberCyan
            )
        }
    }
}

@Composable
private fun DefaultDialerRequirementCard(
    onSetDefaultDialer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, AmberWarning.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        color = SlateCard
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Default Phone App Required for Full Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarning
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Android requires Private Caller ID to become your default phone app to completely replace the system incoming-call UI.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSetDefaultDialer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("action_set_default_dialer"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberWarning,
                    contentColor = DeepObsidian
                )
            ) {
                Text(
                    text = "Set as Default Phone App",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProtectionModePill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (isSelected) CyberCyan else SlateCardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        color = if (isSelected) SlateCardSelected else SlateCard
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) CyberCyan else TextSecondaryDark
            )
        }
    }
}

@Composable
private fun SecurityLayerRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isPassed: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp)),
        color = SlateCard
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPassed) EmeraldProtect.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isPassed) EmeraldProtect else AmberWarning,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isPassed) EmeraldProtect else AmberWarning)
            )
        }
    }
}
