package com.ledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.drawPlainBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.effects.vibrancy
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.Shadow

/* ═══════════════════════════════════════════
   LIQUID GLASS — shader-backed glass helpers
   Uses the KMP Liquid Glass `drawBackdrop` (AGSL
   refraction + blur + vibrancy) over a captured
   backdrop layer provided at the app root.
   ═══════════════════════════════════════════ */

data class GlassStyle(
    val enabled: Boolean = false,
    val screensGlass: Boolean = false,
    val insideGlass: Boolean = false,
    val blur: Int = 8,
    val opacity: Int = 76,
    val refraction: Int = 24,
    val refractionHeight: Int = 12,
    val chromaticAberration: Float = 0f,
)

/** App-wide glass settings, provided once at the root so every card reads them. */
val LocalGlassStyle = staticCompositionLocalOf { GlassStyle() }

/** The captured backdrop (wallpaper / background) that glass elements sample. */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun GlassSurface(
    style: GlassStyle,
    baseColor: Color,
    accentColor: Color,
    shape: Shape = RoundedCornerShape(18.dp),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = LocalGlassBackdrop.current
    val alpha = (style.opacity.coerceIn(20, 100)) / 100f
    val blurRadius = style.blur.coerceIn(0, 24)
    val refraction = style.refraction.coerceIn(0, 40).toFloat()
    val refractionHeight = style.refractionHeight.coerceIn(0, 40).toFloat()
    val chromatic = style.chromaticAberration.coerceIn(0f, 1f)

    val glassModifier = if (backdrop != null && style.enabled) {
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(blurRadius.dp.toPx())
                    lens(
                        (refractionHeight * (1f + chromatic * 0.5f)).dp.toPx(),
                        (refraction * (1f + chromatic * 0.5f)).dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = chromatic > 0f
                    )
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.16f)) },
                onDrawSurface = {
                    drawRect(baseColor.copy(alpha = alpha * 0.35f))
                }
            )
            .border(1.dp, accentColor.copy(alpha = 0.28f), shape)
            .clip(shape)
    } else {
        Modifier
            .clip(shape)
            .background(baseColor)
            .border(1.dp, accentColor.copy(alpha = 0.22f), shape)
    }

    Box(
        modifier = modifier.then(glassModifier),
        content = content
    )
}

/** Full-screen wrapper for the History/Log-spend views.
Two modes when "Liquid glass screens" is on:
- backdrop (default): the screen gets a light frosted blur; inner cards stay solid.
- inside: the screen is a flat light background and the inner cards become liquid glass.
Uses the safe blur-only recipe on the full-screen backdrop (no refraction shader). */
@Composable
fun GlassScreenBackground(content: @Composable BoxScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    val glass = LocalGlassStyle.current
    val backdrop = LocalGlassBackdrop.current
    val insideGlass = glass.insideGlass

    if (glass.screensGlass && backdrop != null && !insideGlass) {
        // BACKDROP mode: light frosted blur over the wallpaper, cards stay solid.
        val blurRadius = glass.blur.coerceIn(4, 24)
        // Light, translucent tint — keeps the screen airy instead of dark.
        val tintAlpha = 0.12f + (glass.opacity.coerceIn(20, 100) / 100f) * 0.22f
        Box(
            Modifier
                .fillMaxSize()
                .drawPlainBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = {
                        blur(blurRadius.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.10f))
                        drawRect(cs.background.copy(alpha = tintAlpha))
                    }
                ),
        ) {
            CompositionLocalProvider(LocalGlassStyle provides GlassStyle()) { content() }
        }
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .background(cs.background)
        ) {
            if (glass.insideGlass && glass.screensGlass && backdrop != null) {
                // INSIDE mode: flat bg + the inner cards become liquid glass.
                CompositionLocalProvider(
                    LocalGlassStyle provides GlassStyle(
                        enabled = true,
                        blur = glass.blur,
                        opacity = glass.opacity,
                        refraction = glass.refraction,
                        refractionHeight = glass.refractionHeight,
                        chromaticAberration = glass.chromaticAberration,
                    )
                ) { content() }
            } else {
                CompositionLocalProvider(LocalGlassStyle provides GlassStyle()) { content() }
            }
        }
    }
}

/** Soft separator (horizontal or vertical) between adjacent stats. */
@Composable
fun StatDivider(
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val c = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
    if (vertical) {
        Box(modifier.fillMaxHeight().width(1.dp).background(c.copy(alpha = 0.6f)))
    } else {
        Box(modifier.fillMaxWidth().height(1.dp).background(c.copy(alpha = 0.6f)))
    }
}
