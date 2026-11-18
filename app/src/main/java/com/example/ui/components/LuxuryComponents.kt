package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.CapabilityStatus
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLuxuryTokens
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusMuted
import com.example.ui.theme.StatusRose
import com.example.ui.viewmodel.DynamicIslandStatus

@Composable
fun rememberAppHaptic(enabled: Boolean): (Boolean) -> Unit {
    val haptic = LocalHapticFeedback.current
    return { strong ->
        if (enabled) {
            haptic.performHapticFeedback(
                if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
        }
    }
}

@Composable
fun AtmosphericBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(tokens.backgroundTop, tokens.backgroundBottom)
                    )
                )
                if (tokens.themeStyle == AppThemeStyle.LIQUID_GLASS) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(tokens.shimmerHighlight, Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.12f),
                            radius = size.width * 0.85f
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.width * 0.85f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(tokens.shimmerHighlight.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.72f),
                            radius = size.width * 0.7f
                        ),
                        center = Offset(size.width * 0.1f, size.height * 0.72f),
                        radius = size.width * 0.7f
                    )
                } else {
                    // Brushed silver diagonal sheen for Black + Silver Luxury
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                tokens.shimmerHighlight,
                                Color.Transparent,
                                tokens.shimmerHighlight.copy(alpha = 0.06f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height * 0.45f)
                        )
                    )
                }
            }
    ) {
        content()
    }
}

@Composable
fun LuxuryGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalLuxuryTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.verticalGradient(
        colors = listOf(tokens.glassBorderTop, tokens.glassBorderBottom)
    )
    val cardModifier = modifier
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    tokens.glassSurface,
                    tokens.glassSurface.copy(alpha = if (tokens.isDark) 0.82f else 0.96f)
                )
            )
        )
        .border(width = 1.dp, brush = borderBrush, shape = shape)
        .let { m ->
            if (onClick != null) m.clickable(onClick = onClick) else m
        }

    Column(
        modifier = cardModifier.padding(18.dp),
        content = content
    )
}

@Composable
fun CapabilityStatusBadge(
    status: CapabilityStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (status) {
        CapabilityStatus.SUPPORTED -> Triple(
            StatusEmerald.copy(alpha = 0.16f),
            StatusEmerald,
            Icons.Rounded.CheckCircle
        )
        CapabilityStatus.PENDING_INTEGRATION -> Triple(
            StatusAmber.copy(alpha = 0.16f),
            StatusAmber,
            Icons.Rounded.HourglassTop
        )
        CapabilityStatus.UNSUPPORTED -> Triple(
            StatusMuted.copy(alpha = 0.16f),
            StatusMuted,
            Icons.Rounded.Info
        )
    }

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.8.dp, fg.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.badgeText,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = status.badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AnimatedBatteryRing(
    label: String,
    percent: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 86.dp
) {
    val tokens = LocalLuxuryTokens.current
    val targetProgress = ((percent ?: 0).coerceIn(0, 100)) / 100f
    val animatedSweep by animateFloatAsState(
        targetValue = targetProgress * 270f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "batterySweep"
    )

    val ringColor = when {
        percent == null -> StatusMuted.copy(alpha = 0.35f)
        percent <= 20 -> StatusRose
        percent <= 45 -> StatusAmber
        else -> tokens.accent
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 7.dp.toPx()
                val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
                val topLeft = Offset(strokePx / 2f, strokePx / 2f)

                drawArc(
                    color = tokens.glassBorderTop.copy(alpha = 0.25f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                if (percent != null && animatedSweep > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = 135f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = percent?.let { "$it%" } ?: "N/A",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (percent != null) MaterialTheme.colorScheme.onSurface else StatusMuted
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DynamicIslandPill(
    status: DynamicIslandStatus?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLuxuryTokens.current
    AnimatedVisibility(
        visible = status != null,
        enter = slideInVertically(animationSpec = spring(dampingRatio = 0.75f)) { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        status?.let { island ->
            val pillColor = if (tokens.isDark) Color(0xFF0A0C12) else Color(0xFF161B26)
            val borderColor = if (island.isAlert) StatusRose else tokens.accent
            Surface(
                shape = CircleShape,
                color = pillColor,
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.65f)),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (island.isAlert) Icons.Rounded.Warning else Icons.Rounded.BluetoothConnected,
                        contentDescription = null,
                        tint = if (island.isAlert) StatusRose else tokens.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = island.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = island.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB8C2D4),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss notification pill",
                            tint = Color(0xFFB8C2D4),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
