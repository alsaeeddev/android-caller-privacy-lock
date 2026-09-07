package com.alsaeeddev.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alsaeeddev.data.model.CallAudioRoute
import com.alsaeeddev.data.model.CallState
import com.alsaeeddev.data.model.CallerRevealState
import com.alsaeeddev.ui.theme.CyberCyan
import com.alsaeeddev.ui.theme.DeepObsidian
import com.alsaeeddev.ui.theme.EmeraldProtect
import com.alsaeeddev.ui.theme.RoseDecline
import com.alsaeeddev.ui.theme.SlateCard
import com.alsaeeddev.ui.theme.SlateCardBorder

@Composable
fun RevealCallerButton(
    revealState: CallerRevealState,
    onRevealClick: () -> Unit,
    onHideNowClick: () -> Unit,
    hasHiddenElements: Boolean = true,
    requireBiometrics: Boolean = true,
    modifier: Modifier = Modifier
) {
    when (revealState) {
        is CallerRevealState.Revealed -> {
            OutlinedButton(
                onClick = onHideNowClick,
                modifier = modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("action_hide_now"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Hide Caller Identity Now",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        else -> {
            if (!hasHiddenElements) {
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = SlateCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Masking toggles are OFF in Settings",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Button(
                    onClick = onRevealClick,
                    modifier = modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("action_reveal_caller"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = DeepObsidian
                    )
                ) {
                    Icon(
                        imageVector = if (requireBiometrics) Icons.Default.Fingerprint else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = if (requireBiometrics) "Tap to Reveal Caller" else "Unlock with Device PIN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PrimaryCallActions(
    callState: CallState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (callState == CallState.INCOMING_RINGING) {
            // Decline Button
            CallCircleButton(
                icon = Icons.Default.CallEnd,
                label = "Decline",
                backgroundColor = RoseDecline,
                contentColor = Color.White,
                onClick = onDecline,
                testTag = "action_decline_call"
            )

            // Accept Button
            CallCircleButton(
                icon = Icons.Default.Call,
                label = "Accept",
                backgroundColor = EmeraldProtect,
                contentColor = Color.White,
                onClick = onAccept,
                testTag = "action_accept_call"
            )
        } else {
            // Active Call - End Call Button
            CallCircleButton(
                icon = Icons.Default.CallEnd,
                label = "End Call",
                backgroundColor = RoseDecline,
                contentColor = Color.White,
                onClick = onDecline,
                testTag = "action_end_call"
            )
        }
    }
}

@Composable
fun SecondaryCallControls(
    isMuted: Boolean,
    audioRoute: CallAudioRoute,
    isSilenced: Boolean = false,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSilence: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecondaryActionButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = "Mute",
            isActive = isMuted,
            onClick = onToggleMute,
            modifier = Modifier.weight(1f),
            testTag = "action_mute_toggle"
        )

        SecondaryActionButton(
            icon = Icons.Default.VolumeUp,
            label = "Speaker",
            isActive = audioRoute == CallAudioRoute.SPEAKER,
            onClick = onToggleSpeaker,
            modifier = Modifier.weight(1f),
            testTag = "action_speaker_toggle"
        )

        SecondaryActionButton(
            icon = Icons.Default.NotificationsOff,
            label = "Silence",
            isActive = isSilenced,
            onClick = onSilence,
            modifier = Modifier.weight(1f),
            testTag = "action_silence_call"
        )
    }
}

@Composable
private fun CallCircleButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SecondaryActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (isActive) CyberCyan else SlateCard)
                .border(
                    width = 1.dp,
                    color = if (isActive) CyberCyan else SlateCardBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) DeepObsidian else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
