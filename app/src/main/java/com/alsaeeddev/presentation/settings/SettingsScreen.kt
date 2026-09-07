package com.alsaeeddev.presentation.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alsaeeddev.data.model.ProtectionMode
import com.alsaeeddev.data.model.RevealTimeoutOption
import com.alsaeeddev.ui.theme.AmberWarning
import com.alsaeeddev.ui.theme.CyberCyan
import com.alsaeeddev.ui.theme.DeepObsidian
import com.alsaeeddev.ui.theme.EmeraldProtect
import com.alsaeeddev.ui.theme.SlateCard
import com.alsaeeddev.ui.theme.SlateCardBorder
import com.alsaeeddev.ui.theme.SlateCardSelected
import com.alsaeeddev.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val dialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshRoles()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshRoles()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Protection Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyberCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepObsidian,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = DeepObsidian,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Master Protection Toggle Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            1.dp,
                            if (settings.isProtectionEnabled) CyberCyan.copy(alpha = 0.5f) else AmberWarning.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        ),
                    color = SlateCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (settings.isProtectionEnabled) Icons.Default.Security else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (settings.isProtectionEnabled) CyberCyan else AmberWarning,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Private Caller Shield",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (settings.isProtectionEnabled) CyberCyan else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (settings.isProtectionEnabled) {
                                    "Protection is ACTIVE. Incoming calls will be shielded according to your masking preferences."
                                } else {
                                    "Protection is DISABLED. Caller name, number, and avatar will be displayed normally."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                        }

                        Switch(
                            checked = settings.isProtectionEnabled,
                            onCheckedChange = { viewModel.setProtectionEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepObsidian,
                                checkedTrackColor = CyberCyan,
                                uncheckedThumbColor = TextSecondaryDark,
                                uncheckedTrackColor = DeepObsidian
                            )
                        )
                    }
                }
            }

            // System Role Integration Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            1.dp,
                            if (uiState.isDefaultDialer) EmeraldProtect.copy(alpha = 0.5f) else AmberWarning.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        ),
                    color = SlateCard
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = if (uiState.isDefaultDialer) EmeraldProtect else AmberWarning,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Default Phone App Role",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (uiState.isDefaultDialer) EmeraldProtect.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (uiState.isDefaultDialer) "ACTIVE" else "NOT SET",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isDefaultDialer) EmeraldProtect else AmberWarning
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (uiState.isDefaultDialer) {
                                "Private Caller ID is your default phone app. Telecom routes full incoming call UI through the privacy engine."
                            } else {
                                "Set as default phone app to replace OEM dialers with the encrypted privacy incoming-call screen."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )

                        if (!uiState.isDefaultDialer) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val intent = viewModel.getRequestDialerIntent()
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
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("action_grant_dialer_settings"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberWarning,
                                    contentColor = DeepObsidian
                                )
                            ) {
                                Text(
                                    text = "Grant Default Dialer Role",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Protection Modes Section
            item {
                Column {
                    SectionHeader(
                        title = "PROTECTION MODES",
                        icon = Icons.Default.Security
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProtectionMode.entries.forEach { mode ->
                            ProtectionModeCard(
                                mode = mode,
                                isSelected = settings.mode == mode,
                                onSelect = { viewModel.setProtectionMode(mode) }
                            )
                        }
                    }
                }
            }

            // Reveal Auto-Hide Timeout Section
            item {
                Column {
                    SectionHeader(
                        title = "REVEAL AUTO-HIDE TIMEOUT",
                        icon = Icons.Default.Timer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Duration decrypted caller identity remains visible before automatically re-masking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RevealTimeoutOption.entries.forEach { option ->
                            TimeoutOptionRow(
                                option = option,
                                isSelected = settings.revealTimeoutSeconds == option.seconds,
                                onSelect = { viewModel.setRevealTimeout(option.seconds) }
                            )
                        }
                    }
                }
            }

            // Caller Identity Elements to Hide
            item {
                Column {
                    SectionHeader(
                        title = "CALLER DATA MASKING",
                        icon = Icons.Default.Lock
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                            .background(SlateCard)
                    ) {
                        SettingToggleRow(
                            title = "Hide Caller Name",
                            subtitle = "Replace contact name with 'Private Call'",
                            isChecked = settings.hideCallerName,
                            onCheckedChange = { viewModel.togglePrivacy(hideName = it) }
                        )

                        DividerLine()

                        SettingToggleRow(
                            title = "Hide Phone Number",
                            subtitle = "Zero digits exposed on incoming screen",
                            isChecked = settings.hideCallerNumber,
                            onCheckedChange = { viewModel.togglePrivacy(hideNumber = it) }
                        )

                        DividerLine()

                        SettingToggleRow(
                            title = "Hide Contact Photo",
                            subtitle = "Suppress avatar / profile photos",
                            isChecked = settings.hideContactPhoto,
                            onCheckedChange = { viewModel.togglePrivacy(hidePhoto = it) }
                        )
                    }
                }
            }

            // Lock Screen & Notifications Privacy
            item {
                Column {
                    SectionHeader(
                        title = "LOCK SCREEN & NOTIFICATIONS",
                        icon = Icons.Default.Info
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                            .background(SlateCard)
                    ) {
                        SettingToggleRow(
                            title = "Lock Screen Privacy",
                            subtitle = "Enforce strict masking when device is locked",
                            isChecked = settings.hideOnLockScreen,
                            onCheckedChange = { viewModel.togglePrivacy(hideOnLockScreen = it) }
                        )

                        DividerLine()

                        SettingToggleRow(
                            title = "Mask Notification Content",
                            subtitle = "Heads-up banners show generic private call alert",
                            isChecked = settings.hideNotificationContent,
                            onCheckedChange = { viewModel.togglePrivacy(hideNotificationContent = it) }
                        )
                    }
                }
            }

            // Auto-Hide Triggers & Authentication
            item {
                Column {
                    SectionHeader(
                        title = "SECURITY TRIGGERS",
                        icon = Icons.Default.Security
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                            .background(SlateCard)
                    ) {
                        SettingToggleRow(
                            title = "Biometric Authentication",
                            subtitle = "Require Fingerprint/Face before revealing caller identity",
                            isChecked = settings.requireBiometrics,
                            onCheckedChange = { viewModel.toggleSecurityBehavior(requireBiometrics = it) }
                        )

                        DividerLine()

                        SettingToggleRow(
                            title = "Device PIN/Passcode Fallback",
                            subtitle = "Allow device credential if biometric is unavailable",
                            isChecked = settings.allowDeviceCredentialFallback,
                            onCheckedChange = { viewModel.toggleSecurityBehavior(allowDeviceCredential = it) }
                        )

                        DividerLine()

                        SettingToggleRow(
                            title = "Auto-Hide on Screen Off",
                            subtitle = "Instantly re-locks identity if power button is pressed",
                            isChecked = settings.autoHideOnScreenOff,
                            onCheckedChange = { viewModel.toggleSecurityBehavior(autoHideScreenOff = it) }
                        )

                        DividerLine()

                        SettingToggleRow(
                            title = "Auto-Hide on App Background",
                            subtitle = "Clears decrypted RAM buffers when app loses focus",
                            isChecked = settings.autoHideOnBackground,
                            onCheckedChange = { viewModel.toggleSecurityBehavior(autoHideBackground = it) }
                        )

                        DividerLine()

                        SettingToggleRow(
                            title = "Auto-Hide on Call Disconnect",
                            subtitle = "Instantly clear session data when call ends",
                            isChecked = settings.autoHideAfterCallEnds,
                            onCheckedChange = { viewModel.toggleSecurityBehavior(autoHideCallEnds = it) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = CyberCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ProtectionModeCard(
    mode: ProtectionMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isSelected) CyberCyan else SlateCardBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .testTag("mode_${mode.name}"),
        color = if (isSelected) SlateCardSelected else SlateCard
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else DeepObsidian)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = mode.securityLevel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) CyberCyan else TextSecondaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) CyberCyan else DeepObsidian)
                    .border(1.dp, if (isSelected) CyberCyan else SlateCardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = DeepObsidian,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeoutOptionRow(
    option: RevealTimeoutOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isSelected) CyberCyan else SlateCardBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .testTag("timeout_${option.seconds}"),
        color = if (isSelected) SlateCardSelected else SlateCard
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) CyberCyan else DeepObsidian)
                    .border(1.dp, if (isSelected) CyberCyan else SlateCardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = DeepObsidian,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DeepObsidian,
                checkedTrackColor = CyberCyan,
                uncheckedThumbColor = TextSecondaryDark,
                uncheckedTrackColor = DeepObsidian
            )
        )
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SlateCardBorder)
    )
}
