package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.SecurityState
import com.example.ui.components.AtmosphericBackdrop
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLuxuryTokens
import com.example.ui.theme.StatusRose
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashOverlay(visible: Boolean) {
    val tokens = LocalLuxuryTokens.current
    val infinite = rememberInfiniteTransition(label = "splashPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(420))
    ) {
        AtmosphericBackdrop {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(164.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().scale(pulse)) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    tokens.accent,
                                    Color.Transparent,
                                    tokens.accent.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            ),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_art),
                        contentDescription = stringResource(R.string.brand_full_title),
                        modifier = Modifier
                            .size(116.dp)
                            .clip(RoundedCornerShape(30.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "HARDIK BUDS PRO",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = CircleShape,
                    color = tokens.accent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, tokens.accent.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "ULTRA EDITION",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.accent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Precision Bluetooth Acoustics & Companion Vault",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PinLockOverlay(
    securityState: SecurityState,
    feedbackMessage: String?,
    onVerifyPin: (String) -> Unit,
    onRequestBiometric: () -> Unit,
    onRecoverPin: (String, String) -> Boolean,
    onHaptic: (Boolean) -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    var enteredPin by remember { mutableStateOf("") }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var remainingLockoutSeconds by remember(securityState.lockoutUntilMillis) {
        mutableStateOf(
            ((securityState.lockoutUntilMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        )
    }

    LaunchedEffect(securityState.lockoutUntilMillis) {
        while (true) {
            val rem = ((securityState.lockoutUntilMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            remainingLockoutSeconds = rem
            if (rem <= 0L) break
            delay(1000L)
        }
    }

    LaunchedEffect(securityState.isBiometricEnabled, securityState.isBiometricHardwareAvailable) {
        if (securityState.isBiometricEnabled && securityState.isBiometricHardwareAvailable && remainingLockoutSeconds <= 0L) {
            onRequestBiometric()
        }
    }

    AtmosphericBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = tokens.accent.copy(alpha = 0.14f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "App locked",
                            tint = tokens.accent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "HARDIK BUDS PRO ULTRA",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (remainingLockoutSeconds > 0) {
                        "Rate-limit lockout active: ${remainingLockoutSeconds}s"
                    } else {
                        "Enter your ${securityState.pinLength}-digit companion PIN"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (remainingLockoutSeconds > 0) StatusRose else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(securityState.pinLength) { idx ->
                        val filled = idx < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) tokens.accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                )
                        )
                    }
                }
                if (!feedbackMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = feedbackMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusRose,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { digit ->
                            PinPadButton(
                                label = digit,
                                enabled = remainingLockoutSeconds <= 0L,
                                onClick = {
                                    onHaptic(false)
                                    if (enteredPin.length < securityState.pinLength) {
                                        enteredPin += digit
                                        if (enteredPin.length == securityState.pinLength) {
                                            onVerifyPin(enteredPin)
                                            enteredPin = ""
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (securityState.isBiometricEnabled && securityState.isBiometricHardwareAvailable) {
                        Surface(
                            onClick = {
                                onHaptic(false)
                                onRequestBiometric()
                            },
                            shape = CircleShape,
                            color = tokens.glassSurface,
                            border = BorderStroke(1.dp, tokens.accent.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("lock_biometric_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = "Unlock with Biometrics",
                                    tint = tokens.accent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.size(68.dp))
                    }

                    PinPadButton(
                        label = "0",
                        enabled = remainingLockoutSeconds <= 0L,
                        onClick = {
                            onHaptic(false)
                            if (enteredPin.length < securityState.pinLength) {
                                enteredPin += "0"
                                if (enteredPin.length == securityState.pinLength) {
                                    onVerifyPin(enteredPin)
                                    enteredPin = ""
                                }
                            }
                        }
                    )

                    Surface(
                        onClick = {
                            onHaptic(false)
                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                        },
                        shape = CircleShape,
                        color = tokens.glassSurface,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (securityState.isRecoveryConfigured) {
                    TextButton(onClick = { showRecoveryDialog = true }) {
                        Text("Forgot PIN? Recover via Phrase")
                    }
                }
            }
        }
    }

    if (showRecoveryDialog) {
        var phrase by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text("Recover Companion PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = phrase,
                        onValueChange = { phrase = it },
                        label = { Text("Recovery Phrase") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                        label = { Text("New 4 or 6-Digit PIN") },
                        singleLine = true
                    )
                    error?.let { Text(it, color = StatusRose, style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (onRecoverPin(phrase, newPin)) {
                        showRecoveryDialog = false
                    } else {
                        error = "Recovery phrase incorrect or PIN must be 4 or 6 digits."
                    }
                }) { Text("Reset PIN") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRecoveryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PinPadButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = tokens.glassSurface,
        border = BorderStroke(1.dp, tokens.glassBorderTop.copy(alpha = 0.5f)),
        modifier = Modifier
            .size(68.dp)
            .testTag("pin_key_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
    }
}
