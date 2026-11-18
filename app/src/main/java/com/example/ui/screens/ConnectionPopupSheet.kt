package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AnimatedBatteryRing
import com.example.ui.components.LuxuryGlassCard
import com.example.ui.theme.LocalLuxuryTokens
import com.example.ui.theme.StatusAmber
import com.example.ui.viewmodel.ConnectionPopupState

@Composable
fun ConnectionPopupModal(
    state: ConnectionPopupState,
    onDismiss: () -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    val floatTransition = rememberInfiniteTransition(label = "earbudFloat")
    val floatOffset by floatTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    AnimatedVisibility(
        visible = state.visible,
        enter = fadeIn() + slideInVertically(animationSpec = spring(dampingRatio = 0.78f)) { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            LuxuryGlassCard(
                cornerRadius = 32.dp,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (state.isUiPreviewOnly) {
                            Surface(
                                shape = CircleShape,
                                color = StatusAmber.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "UI ANIMATION PREVIEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusAmber,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            text = state.deviceName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.brandName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.accent
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close connection popup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_earbud_left),
                        contentDescription = "Left Earbud Graphic",
                        modifier = Modifier
                            .size(84.dp)
                            .offset(y = floatOffset.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Image(
                        painter = painterResource(id = R.drawable.img_charging_case),
                        contentDescription = "Charging Case Graphic",
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(26.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Image(
                        painter = painterResource(id = R.drawable.img_earbud_right),
                        contentDescription = "Right Earbud Graphic",
                        modifier = Modifier
                            .size(84.dp)
                            .offset(y = (-floatOffset).dp)
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AnimatedBatteryRing(
                        label = "Left",
                        percent = state.leftBatteryPercent,
                        size = 76.dp
                    )
                    AnimatedBatteryRing(
                        label = "Combined",
                        percent = state.combinedBatteryPercent,
                        size = 86.dp
                    )
                    AnimatedBatteryRing(
                        label = "Case",
                        percent = state.caseBatteryPercent,
                        size = 76.dp
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.batteryNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
