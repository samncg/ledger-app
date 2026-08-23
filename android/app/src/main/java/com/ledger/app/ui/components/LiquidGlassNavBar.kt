package com.ledger.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.effects.vibrancy
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.Shadow

/* ─── Liquid glass bottom pill navigation ───
   History · Log spend · Settings. A highlight thumb
   slides (switch-like) to the pressed segment. ─── */

private const val SEG_HISTORY = 0
private const val SEG_LOG = 1
private const val SEG_SETTINGS = 2

@Composable
fun LiquidGlassNavBar(
    onLogSpend: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val glass = LocalGlassStyle.current
    val backdrop = LocalGlassBackdrop.current
    var selected by remember { mutableIntStateOf(SEG_LOG) }

    val pillShape = RoundedCornerShape(100.dp)
    val accent = cs.primary
    val segmentWidth = 72.dp
    val pillWidth = segmentWidth * 3
    val pillHeight = 58.dp

    val thumbOffset by animateDpAsState(
        targetValue = segmentWidth * selected,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f),
        label = "pill-thumb",
    )

    val blurDp = glass.blur.coerceIn(0, 24).toFloat()
    val refraction = glass.refraction.coerceIn(0, 40).toFloat()
    val refractionHeight = glass.refractionHeight.coerceIn(0, 40).toFloat()

    val navBarModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { pillShape },
            effects = {
                vibrancy()
                blur(blurDp.dp.toPx())
                lens(
                    refractionHeight.dp.toPx(),
                    refraction.dp.toPx(),
                    depthEffect = glass.chromaticAberration
                )
            },
            highlight = { Highlight.Ambient },
            shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.22f)) },
            onDrawSurface = {
                drawRect(cs.surface.copy(alpha = (glass.opacity.coerceIn(20, 100) / 100f) * 0.35f))
            }
        )
    } else {
        Modifier
            .clip(pillShape)
            .background(cs.surface.copy(alpha = 0.65f))
            .border(1.dp, accent.copy(alpha = 0.3f), pillShape)
    }

    Box(
        modifier = modifier
            .width(pillWidth)
            .height(pillHeight)
            .then(navBarModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        // Sliding thumb indicator
        Box(
            Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .height(pillHeight)
                .padding(3.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(accent.copy(alpha = 0.22f))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(100.dp))
        )

        // 3 Nav segments
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavSegment(
                icon = Icons.Outlined.History,
                label = "History",
                selected = selected == SEG_HISTORY,
                primary = false,
                segmentWidth = segmentWidth
            ) {
                selected = SEG_HISTORY
                onOpenHistory()
            }
            NavSegment(
                icon = Icons.Outlined.Add,
                label = "Log spend",
                selected = selected == SEG_LOG,
                primary = true,
                segmentWidth = segmentWidth
            ) {
                selected = SEG_LOG
                onLogSpend()
            }
            NavSegment(
                icon = Icons.Outlined.Palette,
                label = "Settings",
                selected = selected == SEG_SETTINGS,
                primary = false,
                segmentWidth = segmentWidth
            ) {
                selected = SEG_SETTINGS
                onOpenDrawer()
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
        else -> cs.onSurfaceVariant
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
            color = if (selected || primary) cs.onSurface else cs.onSurfaceVariant,
        )
    }
}
