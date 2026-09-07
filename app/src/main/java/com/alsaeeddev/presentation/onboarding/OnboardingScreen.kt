package com.alsaeeddev.presentation.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alsaeeddev.ui.theme.AmberWarning
import com.alsaeeddev.ui.theme.CyberCyan
import com.alsaeeddev.ui.theme.DeepObsidian
import com.alsaeeddev.ui.theme.EmeraldProtect
import com.alsaeeddev.ui.theme.MidnightDark
import com.alsaeeddev.ui.theme.SlateCard
import com.alsaeeddev.ui.theme.SlateCardBorder
import com.alsaeeddev.ui.theme.TextSecondaryDark

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
    onLaunchTestCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val dialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkStatus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Step Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..4).forEach { stepIndex ->
                    val isCurrent = uiState.currentStep == stepIndex
                    val isPast = uiState.currentStep > stepIndex

                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isCurrent) 32.dp else 16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when {
                                    isCurrent -> CyberCyan
                                    isPast -> EmeraldProtect
                                    else -> SlateCardBorder
                                }
                            )
                    )
                    if (stepIndex < 4) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }

            // Step Content
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step_content"
            ) { step ->
                when (step) {
                    0 -> OnboardingStepCard(
                        icon = Icons.Default.Shield,
                        title = "Welcome to Private Caller ID",
                        subtitle = "Protect your privacy from shoulder surfers and bystanders. Incoming call identities remain strictly encrypted and masked until you choose to authenticate.",
                        badgeText = "STEP 1 OF 5"
                    )
                    1 -> OnboardingStepCard(
                        icon = Icons.Default.Phone,
                        title = "Enable Telecom Protection",
                        subtitle = "To provide complete caller masking on incoming calls, Android requires Private Caller ID to become your default phone handler.",
                        badgeText = "STEP 2 OF 5",
                        actionButton = {
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
                                    .height(48.dp)
                                    .testTag("action_onboarding_set_dialer"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isDefaultDialer) EmeraldProtect else AmberWarning,
                                    contentColor = DeepObsidian
                                )
                            ) {
                                Text(
                                    text = if (uiState.isDefaultDialer) "✓ Default Dialer Granted" else "Set as Default Phone App",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                    2 -> OnboardingStepCard(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Authentication",
                        subtitle = "Strong biometric security (Fingerprint / Face ID) or device lock ensures that only you can decrypt and view sensitive caller information.",
                        badgeText = "STEP 3 OF 5"
                    )
                    3 -> OnboardingStepCard(
                        icon = Icons.Default.Lock,
                        title = "Lock Screen Privacy",
                        subtitle = "When your device is locked, notifications and the full-screen call UI show only 'Private Incoming Call'. Auto-hide timers automatically re-mask caller data.",
                        badgeText = "STEP 4 OF 5"
                    )
                    4 -> OnboardingStepCard(
                        icon = Icons.Default.CheckCircle,
                        title = "Protection is Ready",
                        subtitle = "Your device privacy architecture is active. You can run a live simulated call test right now to experience the secure incoming call screen.",
                        badgeText = "STEP 5 OF 5",
                        actionButton = {
                            Button(
                                onClick = {
                                    viewModel.launchTestCall()
                                    onLaunchTestCall()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("action_onboarding_test_call"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = DeepObsidian
                                )
                            ) {
                                Text(
                                    text = "Launch Test Incoming Call",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                    else -> Unit
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.currentStep > 0) {
                    OutlinedButton(
                        onClick = { viewModel.prevStep() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("action_onboarding_back")
                    ) {
                        Text("Back", color = TextSecondaryDark)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (uiState.currentStep >= 4) {
                            viewModel.completeOnboarding()
                            onComplete()
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = DeepObsidian
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("action_onboarding_next")
                ) {
                    Text(
                        text = if (uiState.currentStep >= 4) "Finish & Enter Dashboard" else "Next Step",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    actionButton: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, SlateCardBorder, RoundedCornerShape(24.dp)),
        color = SlateCard
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MidnightDark)
                    .border(2.dp, CyberCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            if (actionButton != null) {
                Spacer(modifier = Modifier.height(20.dp))
                actionButton()
            }
        }
    }
}
