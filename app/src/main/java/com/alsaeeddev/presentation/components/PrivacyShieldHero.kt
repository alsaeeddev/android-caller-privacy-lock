package com.alsaeeddev.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alsaeeddev.data.model.ProtectionMode
import com.alsaeeddev.ui.theme.AmberWarning
import com.alsaeeddev.ui.theme.CyberCyan
import com.alsaeeddev.ui.theme.DeepObsidian
import com.alsaeeddev.ui.theme.EmeraldProtect
import com.alsaeeddev.ui.theme.RoseDecline
import com.alsaeeddev.ui.theme.SlateCard
import com.alsaeeddev.ui.theme.SlateCardBorder
import com.alsaeeddev.ui.theme.TextSecondaryDark

@Composable
fun PrivacyShieldHero(
    isProtectionEnabled: Boolean,
    isDefaultDialer: Boolean,
    protectionMode: ProtectionMode,
    onToggleProtection: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_pulse"
    )

    val activeColor = when {
        !isProtectionEnabled -> RoseDecline
        !isDefaultDialer -> AmberWarning
        else -> EmeraldProtect
    }

    val statusText = when {
        !isProtectionEnabled -> "PROTECTION DISABLED"
        !isDefaultDialer -> "LIMITED PROTECTION"
        else -> "YOU ARE PROTECTED"
    }

    val statusSubtitle = when {
        !isProtectionEnabled -> "Incoming caller identities will not be masked"
        !isDefaultDialer -> "Lock screen & notification protected (Dialer role recommended)"
        else -> "All incoming caller IDs are encrypted & masked"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, SlateCardBorder, RoundedCornerShape(24.dp)),
        color = SlateCard,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Shield Indicator with subtle glowing ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .padding(8.dp)
            ) {
                // Pulse background ring
                if (isProtectionEnabled) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(activeColor.copy(alpha = 0.15f))
                    )
                }

                // Center shield circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    activeColor.copy(alpha = 0.3f),
                                    SlateCard
                                )
                            )
                        )
                        .border(1.5.dp, activeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!isProtectionEnabled) Icons.Default.Warning else Icons.Default.Shield,
                        contentDescription = "Protection Shield",
                        tint = activeColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(activeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = activeColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepObsidian.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Call Privacy Shield",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mode: ${protectionMode.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberCyan
                    )
                }

                Switch(
                    checked = isProtectionEnabled,
                    onCheckedChange = onToggleProtection,
                    modifier = Modifier.testTag("protection_toggle_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DeepObsidian,
                        checkedTrackColor = EmeraldProtect,
                        uncheckedThumbColor = TextSecondaryDark,
                        uncheckedTrackColor = SlateCard
                    )
                )
            }
        }
    }
}
