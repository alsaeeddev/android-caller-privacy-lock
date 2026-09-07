package com.alsaeeddev.presentation.call

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.alsaeeddev.data.model.ActiveCallSession
import com.alsaeeddev.data.model.CallState
import com.alsaeeddev.data.model.CallerRevealState
import com.alsaeeddev.presentation.components.CountdownTimerBar
import com.alsaeeddev.presentation.components.PrimaryCallActions
import com.alsaeeddev.presentation.components.RevealCallerButton
import com.alsaeeddev.presentation.components.SecondaryCallControls
import com.alsaeeddev.ui.theme.AmberWarning
import com.alsaeeddev.ui.theme.CyberCyan
import com.alsaeeddev.ui.theme.DeepObsidian
import com.alsaeeddev.ui.theme.EmeraldProtect
import com.alsaeeddev.ui.theme.MidnightDark
import com.alsaeeddev.ui.theme.SlateCard
import com.alsaeeddev.ui.theme.SlateCardBorder
import com.alsaeeddev.ui.theme.TextSecondaryDark

fun Context.findFragmentActivity(): FragmentActivity? {
    var curr: Context? = this
    while (curr is ContextWrapper) {
        if (curr is FragmentActivity) return curr
        curr = curr.baseContext
    }
    return null
}

@Composable
fun IncomingCallScreen(
    callSession: ActiveCallSession,
    viewModel: IncomingCallViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }

    val settings by viewModel.settings.collectAsState()
    val feedbackMessage by viewModel.authFeedbackMessage.collectAsState()
    val showNoSecurityDialog by viewModel.showNoSecurityDialog.collectAsState()

    // Activity launcher for Android standard Device PIN/Pattern/Password screen fallback
    val deviceCredentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onDeviceCredentialResult(result.resultCode)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    if (showNoSecurityDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoSecurityDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AmberWarning
                )
            },
            title = {
                Text(
                    text = "No Screen Lock Configured",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This device does not have a Biometric (Fingerprint/Face) or Device Screen Lock (PIN/Pattern/Password) set up. Please enable screen lock in Android Settings to protect caller privacy.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissNoSecurityDialog()
                        try {
                            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DeepObsidian)
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNoSecurityDialog() }) {
                    Text("Cancel")
                }
            },
            containerColor = SlateCard
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepObsidian,
                        MidnightDark,
                        DeepObsidian
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Call Status Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SlateCard)
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (!settings.isProtectionEnabled) {
                            Icons.Default.Warning
                        } else if (callSession.isSimulated) {
                            Icons.Default.VpnKey
                        } else {
                            Icons.Default.Shield
                        },
                        contentDescription = null,
                        tint = if (!settings.isProtectionEnabled) AmberWarning else CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (!settings.isProtectionEnabled) {
                            "PROTECTION OFF"
                        } else if (callSession.isSimulated) {
                            "SIMULATED CALL (PROTECTED)"
                        } else {
                            "TELECOM CALL (SHIELDED)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!settings.isProtectionEnabled) AmberWarning else CyberCyan,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (callSession.callState) {
                        CallState.INCOMING_RINGING -> if (settings.isProtectionEnabled) "Incoming Protected Call..." else "Incoming Call..."
                        CallState.CONNECTING -> "Connecting..."
                        CallState.ACTIVE -> "Active Call (${formatDuration(callSession.callDurationSeconds)})"
                        CallState.ON_HOLD -> "Call on Hold"
                        CallState.DISCONNECTED -> "Call Ended"
                        CallState.IDLE -> "Ready"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            // Middle: Avatar & Dynamic Mask / Decrypted Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val isRevealed = callSession.revealState is CallerRevealState.Revealed
                val shouldShowPhoto = !settings.isProtectionEnabled || isRevealed || !settings.hideContactPhoto

                // Avatar Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    // Pulsing Outer Ring during Ringing
                    if (callSession.callState == CallState.INCOMING_RINGING) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(ringScale)
                                .clip(CircleShape)
                                .background(
                                    if (isRevealed) {
                                        EmeraldProtect.copy(alpha = 0.15f)
                                    } else {
                                        CyberCyan.copy(alpha = 0.12f)
                                    }
                                )
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (isRevealed) {
                                        listOf(EmeraldProtect.copy(alpha = 0.25f), SlateCard)
                                    } else {
                                        listOf(CyberCyan.copy(alpha = 0.20f), SlateCard)
                                    }
                                )
                            )
                            .border(
                                width = 2.dp,
                                color = if (isRevealed) EmeraldProtect else CyberCyan,
                                shape = CircleShape
                            )
                    ) {
                        if (shouldShowPhoto) {
                            // Contact Initial Avatar
                            val rawName = callSession.rawCallerName.ifBlank { "Caller" }
                            val initials = getInitials(rawName)
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isRevealed) EmeraldProtect else CyberCyan
                            )
                        } else {
                            Icon(
                                imageVector = when (callSession.revealState) {
                                    is CallerRevealState.Revealed -> Icons.Default.VerifiedUser
                                    is CallerRevealState.Authenticating -> Icons.Default.Lock
                                    is CallerRevealState.Hidden -> Icons.Default.Shield
                                    is CallerRevealState.Expired -> Icons.Default.Shield
                                },
                                contentDescription = "Shield Avatar",
                                tint = if (isRevealed) EmeraldProtect else CyberCyan,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dynamic Info Section: Masked or Revealed
                AnimatedContent(
                    targetState = isRevealed,
                    transitionSpec = {
                        (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 })
                            .togetherWith(fadeOut(tween(200)))
                    },
                    label = "caller_info_anim"
                ) { revealed ->
                    if (revealed) {
                        val revealedState = callSession.revealState as? CallerRevealState.Revealed
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldProtect.copy(alpha = 0.15f))
                                    .border(1.dp, EmeraldProtect.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = EmeraldProtect,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "AUTHENTICATED & DECRYPTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldProtect,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = revealedState?.callerName ?: callSession.rawCallerName.ifBlank { "Private Contact" },
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = revealedState?.formattedNumber ?: callSession.rawCallerNumber.ifBlank { "Restricted" },
                                style = MaterialTheme.typography.titleLarge,
                                color = CyberCyan,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )

                            if (!callSession.rawLocation.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = revealedState?.locationOrCarrier ?: callSession.rawLocation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Auto-hide countdown bar
                            CountdownTimerBar(
                                remainingSeconds = revealedState?.remainingSeconds ?: 0,
                                totalSeconds = revealedState?.totalTimeoutSeconds ?: 0
                            )
                        }
                    } else {
                        // Unauthenticated / Masked State
                        val isNameHidden = settings.isProtectionEnabled && settings.hideCallerName
                        val isNumberHidden = settings.isProtectionEnabled && settings.hideCallerNumber

                        val displayName = if (isNameHidden) {
                            "🔒 PRIVATE CALL"
                        } else {
                            callSession.rawCallerName.ifBlank { "Incoming Caller" }
                        }

                        val displayNumber = if (isNumberHidden) {
                            "•••• •••• ••••"
                        } else {
                            callSession.rawCallerNumber.ifBlank { "Restricted" }
                        }

                        val badgeText = when {
                            !settings.isProtectionEnabled -> "PROTECTION DISABLED"
                            isNameHidden && isNumberHidden -> "PRIVACY SHIELD ACTIVE"
                            isNameHidden || isNumberHidden -> "PARTIAL MASKING ACTIVE"
                            else -> "MASKING OFF IN SETTINGS"
                        }

                        val badgeColor = when {
                            !settings.isProtectionEnabled -> AmberWarning
                            isNameHidden || isNumberHidden -> AmberWarning
                            else -> CyberCyan
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(badgeColor.copy(alpha = 0.12f))
                                    .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (!settings.isProtectionEnabled) Icons.Default.Warning else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = if (isNameHidden) 0.5.sp else 0.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = displayNumber,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isNumberHidden) TextSecondaryDark else CyberCyan,
                                textAlign = TextAlign.Center,
                                fontWeight = if (isNumberHidden) FontWeight.Normal else FontWeight.SemiBold,
                                letterSpacing = if (isNumberHidden) 2.sp else 1.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val subtitleText = when {
                                !settings.isProtectionEnabled -> "Caller protection is turned OFF in Settings"
                                isNameHidden && isNumberHidden -> "Caller identity masked for your privacy"
                                isNameHidden -> "Caller name is masked • Number is visible"
                                isNumberHidden -> "Caller name is visible • Number is masked"
                                else -> "All caller data visible • Masking toggles off"
                            }

                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center
                            )

                            if (settings.isProtectionEnabled && (isNameHidden || isNumberHidden || settings.hideContactPhoto)) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (settings.requireBiometrics) "1st: Biometrics • 2nd: Device PIN/Pattern" else "Unlock via Device Screen Lock (PIN/Pattern)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyberCyan,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (feedbackMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = feedbackMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                val hasHiddenElements = settings.isProtectionEnabled &&
                        (settings.hideCallerName || settings.hideCallerNumber || settings.hideContactPhoto)

                // Reveal / Hide Toggle Button
                RevealCallerButton(
                    revealState = callSession.revealState,
                    hasHiddenElements = hasHiddenElements,
                    requireBiometrics = settings.requireBiometrics,
                    onRevealClick = {
                        viewModel.clearFeedbackMessage()
                        viewModel.requestRevealAuthentication(
                            activity = activity,
                            onLaunchDeviceCredentialIntent = { intent ->
                                deviceCredentialLauncher.launch(intent)
                            }
                        )
                    },
                    onHideNowClick = {
                        viewModel.hideCallerNow()
                    }
                )
            }

            // Bottom Call Controls (Accept, Decline, Audio, Mute)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SecondaryCallControls(
                    isMuted = callSession.isMuted,
                    audioRoute = callSession.audioRoute,
                    isSilenced = callSession.isSilenced,
                    onToggleMute = { viewModel.toggleMute() },
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    onSilence = { viewModel.silenceCall() }
                )

                Spacer(modifier = Modifier.height(18.dp))

                PrimaryCallActions(
                    callState = callSession.callState,
                    onAccept = { viewModel.acceptCall() },
                    onDecline = { viewModel.declineCall() }
                )
            }
        }
    }
}

private fun getInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "?"
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
