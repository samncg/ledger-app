package com.ledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.glass.ChromaticAberrationMode
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.glass.GlassStyle as HazeGlassStyle

/* ═══════════════════════════════════════════
   LIQUID GLASS — Haze-backed glass helpers
   Uses Haze 2 (`haze-glass`) for refraction +
   depth blur + dispersion + lift over a captured
   source layer provided at the app root.
   ═══════════════════════════════════════════ */

data class GlassStyle(
    val enabled: Boolean = false,
    val screensGlass: Boolean = false,
    val insideGlass: Boolean = false,
    val blur: Int = 0,
    val barBlur: Int = 0,    // blur for the non-card UI glass (bottom bar, its bubbles)
    val barGlass: Boolean = true, // liquid glass for the bottom bar and its bubbles
    val opacity: Int = 100,
    val barOpacity: Int = 0, // how solid that non-card glass is, set apart from the cards
    val refraction: Int = 14,
    val refractionHeight: Int = 14,
    val chromaticAberration: Float = 0.5f,
    val innerOpacity: Int = 40, // fill opacity for panels nested inside a glass card
)

/**
 * Fill for a panel nested inside a card (stat tiles, strip backgrounds, chips).
 * Opaque on the themed surface normally, but frosted when "Liquid glass cards" is on
 * so the card's glass shows through instead of a solid dark box. The strength of that
 * frost is user-tunable via `innerOpacity` (0 = fully clear, 100 = solid).
 */
@Composable
fun innerSurfaceColor(): Color {
    val cs = MaterialTheme.colorScheme
    val glass = LocalGlassStyle.current
    if (!glass.enabled) return cs.surfaceVariant
    val alpha = glass.innerOpacity.coerceIn(0, 100) / 100f
    return if (alpha >= 1f) cs.surfaceVariant else cs.surfaceVariant.copy(alpha = alpha)
}

/** App-wide glass settings, provided once at the root so every card reads them. */
val LocalGlassStyle = staticCompositionLocalOf { GlassStyle() }

/** The Haze state capturing the wallpaper/background that glass elements sample. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Rendering budget for the glass effects in this subtree. Defaults to Haze's own `Default`
 * (Balanced) profile; the full-screen glass views drop to `Performance` because inner glass
 * over a whole scrolling screen is where the cost actually bites.
 */
val LocalGlassPerformance = staticCompositionLocalOf { HazePerformanceMode.Default }

/** How far the glass elements float above the content, i.e. the drop shadow they cast. */
internal val GlassLift = 6.dp

/**
 * Translate the app's [GlassStyle] settings into an immutable Haze [HazeGlassStyle].
 *
 * Haze validates its optics when the Style is created, so every value is clamped to a
 * conservative range first — a bad value here would otherwise throw and take the app down.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
internal fun rememberHazeGlassStyle(
    style: GlassStyle,
    shape: RoundedCornerShape,
    containerColor: Color,
    /* The card tint is kept faint so the refraction carries the look; the bars use their own. */
    tintScale: Float = 0.35f,
    blurOverride: Int? = null,
    refractionOverride: Float? = null,
    refractionHeightOverride: Float? = null,
): HazeGlassStyle {
    val blurDp = (blurOverride ?: style.blur).coerceIn(0, 40).toFloat()
    val refraction = (refractionOverride ?: style.refraction.toFloat()).coerceIn(0f, 40f)
    val height = (refractionHeightOverride ?: style.refractionHeight.toFloat()).coerceIn(0f, 40f)
    val chroma = style.chromaticAberration.coerceIn(0f, 1f)
    val alpha = (style.opacity.coerceIn(20, 100) / 100f) * tintScale
    return remember(shape, containerColor, blurDp, refraction, height, chroma, alpha) {
        HazeGlassStyle {
            shape(shape)
            tint(containerColor.copy(alpha = alpha.coerceIn(0f, 1f)))
            optics(
                blurRadius = blurDp.dp,
                refractionStrength = (refraction / 20f).coerceIn(0.001f, 1f),
                refractionHeightFraction = (height / 100f).coerceIn(0.001f, 1f),
                refractionDisplacement = refraction.dp,
                depth = 0.5f,
            )
            chromaticAberrationStrength((chroma * 1.8f).coerceIn(0f, 1f))
            chromaticAberrationMode(
                if (chroma > 0.001f) ChromaticAberrationMode.Full else ChromaticAberrationMode.Simple
            )
            specularIntensity(0.5f)
            ambientResponse(0.2f)
            /* The dark shape-following rim that gives the glass its edge — the "lift". */
            edgeShadow(Color.Black.copy(alpha = 0.16f))
            edgeSoftness(1.dp)
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun GlassSurface(
    style: GlassStyle,
    baseColor: Color,
    accentColor: Color,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val haze = LocalHazeState.current
    val glassStyle = rememberHazeGlassStyle(style, shape, baseColor)

    val glassModifier = if (haze != null && style.enabled) {
        Modifier
            /* A soft drop shadow under the glass reads as the card floating above the page. */
            .shadow(elevation = GlassLift, shape = shape, clip = false)
            .hazeGlass(
                input = HazeInput.Sources(haze),
                style = glassStyle,
                performanceMode = LocalGlassPerformance.current,
            )
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
@OptIn(ExperimentalHazeApi::class)
@Composable
fun GlassScreenBackground(content: @Composable BoxScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    val glass = LocalGlassStyle.current
    val haze = LocalHazeState.current
    val insideGlass = glass.insideGlass

    if (glass.screensGlass && haze != null && !insideGlass) {
        // BACKDROP mode: light frosted blur over the wallpaper, cards stay solid.
        val blurRadius = glass.blur.coerceIn(4, 40)
        // Light, translucent tint — keeps the screen airy instead of dark.
        val tintAlpha = 0.12f + (glass.opacity.coerceIn(20, 100) / 100f) * 0.22f
        val blurStyle = remember(blurRadius) {
            HazeBlurStyle { blurRadius(blurRadius.dp) }
        }
        Box(
            Modifier
                .fillMaxSize()
                .hazeBlur(
                    input = HazeInput.Sources(haze),
                    style = blurStyle,
                    performanceMode = HazePerformanceMode.Performance,
                ),
        ) {
            /* The frosted wash, drawn under the screen's own content. */
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.10f))
                    .background(cs.background.copy(alpha = tintAlpha))
            )
            CompositionLocalProvider(LocalGlassStyle provides GlassStyle()) { content() }
        }
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .background(cs.background)
        ) {
            if (glass.insideGlass && glass.screensGlass && haze != null) {
                // INSIDE mode: flat bg + the inner cards become liquid glass.
                CompositionLocalProvider(
                    LocalGlassStyle provides GlassStyle(
                        enabled = true,
                        blur = glass.blur,
                        opacity = glass.opacity,
                        refraction = glass.refraction,
                        refractionHeight = glass.refractionHeight,
                        chromaticAberration = glass.chromaticAberration,
                        innerOpacity = glass.innerOpacity,
                    ),
                    /* A full screen of inner glass is the heavy case, so render it at the cheaper
                       budget — this is what stops the Log a spend screen stuttering on scroll. */
                    LocalGlassPerformance provides HazePerformanceMode.Performance,
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
