package com.ledger.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.Shadow

/* ─── Bottom pill navigation ───
   History · Log spend · Settings, drawn in the app window (correct backdrop
   sampling) and on top of the full-screen views. When a view is open the pill
   springs (bounce) into a circular glass close button. Uses blur-only glass so
   the morph doesn't re-run expensive per-frame refraction shaders. ─── */

private const val SEG_HISTORY = 0
private const val SEG_LOG = 1
private const val SEG_SETTINGS = 2

@Composable
fun LiquidGlassNavBar(
    onLogSpend: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDrawer: () -> Unit,
    onClose: () -> Unit,
    isOverlayOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val glass = LocalGlassStyle.current
    val backdrop = LocalGlassBackdrop.current
    var selected by remember { mutableIntStateOf(-1) }
    val tick = rememberHapticTick()

    LaunchedEffect(isOverlayOpen) {
        if (!isOverlayOpen) {
            selected = -1
        }
    }

    val pillShape = RoundedCornerShape(100.dp)
    val accent = cs.primary
    val segmentWidth = 72.dp
    val pillHeight = 58.dp

    val targetWidth = if (isOverlayOpen) 58.dp else segmentWidth * 3
    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "pill-width",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (selected in 0..2) segmentWidth * selected else segmentWidth,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f),
        label = "pill-thumb",
    )

    val blurDp = glass.blur.coerceIn(4, 24).toFloat()
    val containerColor = cs.surface.copy(alpha = (glass.opacity.coerceIn(20, 100) / 100f) * 0.5f)

    val pillModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { pillShape },
            effects = { blur(blurDp.dp.toPx()) },
            highlight = { Highlight.Ambient },
            shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.22f)) },
            onDrawSurface = { drawRect(containerColor) }
        )
    } else {
        Modifier.clip(pillShape).background(cs.surface.copy(alpha = 0.7f))
            .border(1.dp, accent.copy(alpha = 0.3f), pillShape)
    }

    Box(
        modifier
            .width(width)
            .height(pillHeight)
            .then(pillModifier)
            .clip(pillShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        AnimatedContent(
            targetState = isOverlayOpen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "pill-content",
        ) { overlay ->
            if (overlay) {
                Box(
                    Modifier.fillMaxSize().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { tick(); onClose() },
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, "Close", tint = cs.onSurface, modifier = Modifier.size(22.dp))
                }
            } else {
                // Glass highlighter thumb (blur-only liquid glass, accent tint)
                if (selected in 0..2) {
                    Box(
                        Modifier
                            .offset(x = thumbOffset)
                            .width(segmentWidth)
                            .height(pillHeight)
                            .padding(3.dp)
                            .let { tb ->
                                if (backdrop != null) {
                                    tb.drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedCornerShape(100.dp) },
                                        effects = { blur(blurDp.dp.toPx()) },
                                        onDrawSurface = { drawRect(accent.copy(alpha = 0.20f)) }
                                    )
                                } else {
                                    tb.clip(RoundedCornerShape(100.dp)).background(accent.copy(alpha = 0.2f))
                                }
                            }
                            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(100.dp)),
                    )
                }

                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavSegment(Icons.Outlined.History, "History", selected == SEG_HISTORY, false, segmentWidth) {
                        selected = SEG_HISTORY; onOpenHistory()
                    }
                    NavSegment(Icons.Outlined.Add, "Log spend", selected == SEG_LOG, true, segmentWidth) {
                        selected = SEG_LOG; onLogSpend()
                    }
                    NavSegment(Icons.Outlined.Palette, "Settings", selected == SEG_SETTINGS, false, segmentWidth) {
                        selected = SEG_SETTINGS; onOpenDrawer()
                    }
                }
            }
        }
    }
}

@Composable
private fun NavSegment(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    primary: Boolean,
    segmentWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val c = cs.primary
    val tick = rememberHapticTick()
    val tint = when {
        primary -> c
        selected -> c
        else -> cs.onSurface
    }
    Column(
        Modifier
            .width(segmentWidth)
            .clip(RoundedCornerShape(100.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { tick(); onClick() },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            label,
            fontSize = 9.5.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = cs.onSurface,
        )
    }
}
