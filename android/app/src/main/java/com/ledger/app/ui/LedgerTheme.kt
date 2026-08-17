package com.ledger.app.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.ledger.app.data.AppTheme

/* ═══════════════════════════════════════════
   THEME — maps the app's hex/rgba palette onto
   a Material 3 color scheme
   ═══════════════════════════════════════════ */

/** Parse "#rrggbb", "#rgb" or "rgba(r,g,b,a)" strings into a Compose Color. */
fun parseColor(s: String?): Color? {
    val t = s?.trim() ?: return null
    if (t.startsWith("#")) {
        val hex = t.removePrefix("#")
        return when (hex.length) {
            6 -> runCatching { Color(0xFF000000L or hex.toLong(16)) }.getOrNull()
            3 -> runCatching {
                val r = hex[0].digitToInt(16);
                val g = hex[1].digitToInt(16);
                val b = hex[2].digitToInt(16)
                Color(r * 17, g * 17, b * 17)
            }.getOrNull()

            else -> null
        }
    }
    if (t.startsWith("rgba(") && t.endsWith(")")) {
        val parts = t.removePrefix("rgba(").removeSuffix(")").split(",").map { it.trim() }
        if (parts.size == 4) {
            val r = parts[0].toIntOrNull() ?: return null
            val g = parts[1].toIntOrNull() ?: return null
            val b = parts[2].toIntOrNull() ?: return null
            val a = parts[3].toFloatOrNull() ?: 1f
            return runCatching { Color(r, g, b, (a * 255).toInt()) }.getOrNull()
        }
    }
    return null
}

private fun color(s: String?): Color = parseColor(s) ?: Color.Transparent

fun fontFor(id: String): FontFamily = when (id) {
    "sans" -> FontFamily.SansSerif
    "serif" -> FontFamily.Serif
    "mono" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    else -> FontFamily.Default
}

private fun typographyWith(font: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = font),
        displayMedium = base.displayMedium.copy(fontFamily = font),
        displaySmall = base.displaySmall.copy(fontFamily = font),
        headlineLarge = base.headlineLarge.copy(fontFamily = font),
        headlineMedium = base.headlineMedium.copy(fontFamily = font),
        headlineSmall = base.headlineSmall.copy(fontFamily = font),
        titleLarge = base.titleLarge.copy(fontFamily = font),
        titleMedium = base.titleMedium.copy(fontFamily = font),
        titleSmall = base.titleSmall.copy(fontFamily = font),
        bodyLarge = base.bodyLarge.copy(fontFamily = font),
        bodyMedium = base.bodyMedium.copy(fontFamily = font),
        bodySmall = base.bodySmall.copy(fontFamily = font),
        labelLarge = base.labelLarge.copy(fontFamily = font),
        labelMedium = base.labelMedium.copy(fontFamily = font),
        labelSmall = base.labelSmall.copy(fontFamily = font),
    )
}

private fun scheme(theme: AppTheme): androidx.compose.material3.ColorScheme {
    val bg = parseColor(theme.bg)
    val dark = bg?.let { it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f < 0.5f } ?: true
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = color(theme.accent),
        onPrimary = color(theme.accentFg),
        primaryContainer = color(theme.surface2),
        onPrimaryContainer = color(theme.text),
        secondary = color(theme.accent),
        onSecondary = color(theme.accentFg),
        secondaryContainer = color(theme.surface2),
        onSecondaryContainer = color(theme.textDim),
        tertiary = color(theme.accent),
        onTertiary = color(theme.accentFg),
        background = color(theme.bg),
        onBackground = color(theme.text),
        surface = color(theme.surface),
        onSurface = color(theme.text),
        surfaceVariant = color(theme.surface2),
        onSurfaceVariant = color(theme.textDim),
        surfaceContainer = color(theme.surface2),
        surfaceContainerHigh = color(theme.surface2),
        surfaceContainerHighest = color(theme.surface2),
        outline = color(theme.border),
        outlineVariant = color(theme.borderStrong),
        error = color(theme.negative),
        onError = color(theme.accentFg),
    )
}

@Composable
fun LedgerTheme(theme: AppTheme, fontId: String, content: @Composable () -> Unit) {
    val cs = scheme(theme)
    MaterialTheme(
        colorScheme = cs,
        typography = typographyWith(fontFor(fontId)),
    ) {
        // Raw Text/Icon default to black unless we provide a content color.
        CompositionLocalProvider(LocalContentColor provides cs.onSurface, content = content)
    }
}

/** Convenience: a Material style for the mono font used by amounts. */
object LedgerTextStyles {
    val Mono: TextStyle get() = TextStyle(fontFamily = FontFamily.Monospace)
}
