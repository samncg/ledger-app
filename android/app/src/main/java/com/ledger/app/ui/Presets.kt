package com.ledger.app.ui

import com.ledger.app.data.AppTheme

/* ═══════════════════════════════════════════
   THEME PRESETS — exact colors from the web app
   ═══════════════════════════════════════════ */

val PRESETS: Map<String, AppTheme> = mapOf(
    "mono" to AppTheme(
        bg = "#000000",
        surface = "#000000",
        surface2 = "#0a0a0a",
        text = "#ffffff",
        textDim = "#9a9a9a",
        textMuted = "#5c5c5c",
        border = "rgba(255,255,255,0.10)",
        borderStrong = "rgba(255,255,255,0.22)",
        accent = "#ffffff",
        accentFg = "#000000",
        negative = "#ff5c5c",
        warning = "#e8c15a",
        positive = "#5bd488",
        catColors = mapOf(
            "food" to "#5b9fd4",
            "transport" to "#e8c15a",
            "social" to "#ff6b8b",
            "shopping" to "#a68bfa",
            "other" to "#5bd488"
        )
    ),
    "graphite" to AppTheme(
        bg = "#111318",
        surface = "#1a1d24",
        surface2 = "#23272f",
        text = "#e4e6ea",
        textDim = "#8b919e",
        textMuted = "#555b66",
        border = "rgba(255,255,255,0.07)",
        borderStrong = "rgba(255,255,255,0.14)",
        accent = "#8b9eff",
        accentFg = "#0d0f16",
        negative = "#f06b6b",
        warning = "#e3b341",
        positive = "#7ee787",
        catColors = mapOf(
            "food" to "#8b9eff",
            "transport" to "#e3b341",
            "social" to "#f06b6b",
            "shopping" to "#a371f7",
            "other" to "#768390"
        )
    ),
    "forest" to AppTheme(
        bg = "#0a100d",
        surface = "#131f19",
        surface2 = "#1b2b23",
        text = "#dfeae4",
        textDim = "#86a092",
        textMuted = "#4e6458",
        border = "rgba(255,255,255,0.06)",
        borderStrong = "rgba(255,255,255,0.14)",
        accent = "#6caf82",
        accentFg = "#0a100d",
        negative = "#d47070",
        warning = "#d4a55a",
        positive = "#6caf82",
        catColors = mapOf(
            "food" to "#6caf82",
            "transport" to "#d4a55a",
            "social" to "#d47070",
            "shopping" to "#7da29e",
            "other" to "#8a8f7d"
        )
    ),
    "paper" to AppTheme(
        bg = "#fafaf7",
        surface = "#ffffff",
        surface2 = "#f0f0eb",
        text = "#1a1a17",
        textDim = "#5a5a55",
        textMuted = "#9a9a95",
        border = "rgba(0,0,0,0.07)",
        borderStrong = "rgba(0,0,0,0.14)",
        accent = "#1a1a17",
        accentFg = "#fafaf7",
        negative = "#c04a30",
        warning = "#c07a20",
        positive = "#3a7a3a",
        catColors = mapOf(
            "food" to "#3a7a3a",
            "transport" to "#c07a20",
            "social" to "#c04a30",
            "shopping" to "#5c4a7a",
            "other" to "#6a6a65"
        )
    ),
    "daylight" to AppTheme(
        bg = "#f4f6f9",
        surface = "#ffffff",
        surface2 = "#eef1f6",
        text = "#111318",
        textDim = "#4a5568",
        textMuted = "#9099a8",
        border = "rgba(0,0,0,0.07)",
        borderStrong = "rgba(0,0,0,0.14)",
        accent = "#2563eb",
        accentFg = "#ffffff",
        negative = "#dc2626",
        warning = "#d97706",
        positive = "#16a34a",
        catColors = mapOf(
            "food" to "#2563eb",
            "transport" to "#d97706",
            "social" to "#dc2626",
            "shopping" to "#7c3aed",
            "other" to "#64748b"
        )
    ),
    "sakura" to AppTheme(
        bg = "#fdf4f7",
        surface = "#ffffff",
        surface2 = "#fbe8ef",
        text = "#3a2029",
        textDim = "#8a6570",
        textMuted = "#b49aa2",
        border = "rgba(58,32,41,0.08)",
        borderStrong = "rgba(58,32,41,0.16)",
        accent = "#d1476f",
        accentFg = "#ffffff",
        negative = "#c0392b",
        warning = "#a8761a",
        positive = "#3f8a5a",
        catColors = mapOf(
            "food" to "#d1476f",
            "transport" to "#a8761a",
            "social" to "#c0392b",
            "shopping" to "#9b5fb0",
            "other" to "#8a7170"
        )
    ),
    "linen" to AppTheme(
        bg = "#efeae0",
        surface = "#f8f4ea",
        surface2 = "#e3ddd0",
        text = "#1f1a12",
        textDim = "#5a5040",
        textMuted = "#9a9080",
        border = "rgba(31,26,18,0.08)",
        borderStrong = "rgba(31,26,18,0.16)",
        accent = "#8a5a3a",
        accentFg = "#f8f4ea",
        negative = "#a03828",
        warning = "#a06818",
        positive = "#4a7248",
        catColors = mapOf(
            "food" to "#8a5a3a",
            "transport" to "#a06818",
            "social" to "#a03828",
            "shopping" to "#6a5088",
            "other" to "#6a6055"
        )
    ),
)

val DEFAULT_THEME: AppTheme = PRESETS["paper"]!!

/**
 * The wallpaper the "auto" mode resolves to for a theme: white for the light-leaning presets
 * (paper, daylight, linen, sakura), black for the dark ones (mono, graphite, forest). A custom
 * theme (no matching preset) falls back to the lightness of its own background.
 */
fun autoWallpaperIsWhite(
    theme: AppTheme,
    categories: List<com.ledger.app.data.Category>,
): Boolean = when (activePresetKey(theme, categories)) {
    "paper", "daylight", "linen", "sakura" -> true
    "mono", "graphite", "forest" -> false
    else -> {
        val bg = parseColor(theme.bg)
        if (bg == null) true
        else (bg.red * 0.299f + bg.green * 0.587f + bg.blue * 0.114f) >= 0.5f
    }
}

/** Determine which preset (if any) matches the current theme exactly. */
fun activePresetKey(theme: AppTheme, categories: List<com.ledger.app.data.Category>): String? {
    for ((key, preset) in PRESETS) {
        val base = listOf(
            "bg",
            "surface",
            "surface2",
            "text",
            "textDim",
            "textMuted",
            "border",
            "borderStrong",
            "accent",
            "accentFg",
            "negative",
            "warning",
            "positive"
        )
            .all { k -> fieldOf(preset, k) == fieldOf(theme, k) }
        val catsMatch = categories.all { c -> preset.catColors[c.id] == theme.catColors[c.id] }
        if (base && catsMatch) return key
    }
    return null
}

private fun fieldOf(t: AppTheme, k: String): String = when (k) {
    "bg" -> t.bg; "surface" -> t.surface; "surface2" -> t.surface2; "text" -> t.text
    "textDim" -> t.textDim; "textMuted" -> t.textMuted; "border" -> t.border
    "borderStrong" -> t.borderStrong; "accent" -> t.accent; "accentFg" -> t.accentFg
    "negative" -> t.negative; "warning" -> t.warning; "positive" -> t.positive
    else -> ""
}

/* GitHub-style spending heatmap presets */
val HEAT_PRESETS: Map<String, Pair<String, Map<String, String>>> = mapOf(
    "github" to ("GitHub" to mapOf(
        "l0" to "transparent",
        "l1" to "#9be9a8",
        "l2" to "#40c463",
        "l3" to "#30a14e",
        "l4" to "#216e39"
    )),
    "githubDark" to ("GitHub dark" to mapOf(
        "l0" to "#161b22",
        "l1" to "#0e4429",
        "l2" to "#006d32",
        "l3" to "#26a641",
        "l4" to "#39d353"
    )),
    "ocean" to ("Ocean" to mapOf(
        "l0" to "transparent",
        "l1" to "#a8c8f0",
        "l2" to "#5b9fd4",
        "l3" to "#2f6fb8",
        "l4" to "#1e3f8f"
    )),
    "purple" to ("Purple" to mapOf(
        "l0" to "transparent",
        "l1" to "#d8c8f5",
        "l2" to "#b08cff",
        "l3" to "#8a5cf5",
        "l4" to "#5a2db0"
    )),
    "warm" to ("Warm" to mapOf(
        "l0" to "transparent",
        "l1" to "#ffdfb0",
        "l2" to "#ffb066",
        "l3" to "#f57c3d",
        "l4" to "#c2402a"
    )),
)

val HEAT_LEVELS = listOf("l0", "l1", "l2", "l3", "l4")

/* Font options — mapped to system font families (custom Google Fonts are web-only). */
data class FontOption(val id: String, val name: String)

val FONT_OPTIONS = listOf(
    FontOption("default", "System UI"),
    FontOption("sans", "Sans Serif"),
    FontOption("serif", "Serif"),
    FontOption("mono", "Monospace"),
    FontOption("cursive", "Cursive"),
    FontOption("condensed", "Condensed"),
    FontOption("light", "Light"),
    FontOption("medium", "Medium"),
    FontOption("black", "Black"),
    FontOption("serifMono", "Serif Mono"),
    FontOption("casual", "Casual"),
    FontOption("smallCaps", "Small Caps"),
)

val CAT_COLOR_PRESETS = listOf(
    null, "#ff8a3d", "#ff5c93", "#ffd93d", "#5bd488", "#5b9fd4", "#a68bfa", "#f5f5f5", "#2a2a2a",
)

/* Symbol choices for the new-category glyph field — text glyphs only (no colour emoji). */
val CAT_GLYPH_PRESETS = listOf(
    "◇", "◆", "●", "○", "◎", "◐", "□", "■", "▢", "▲",
    "△", "▼", "▽", "★", "☆", "✦", "✧", "✱", "✓", "✗",
    "✚", "✎", "⚑", "⌂", "∞", "§", "¶", "∅", "≠", "≈",
    "±", "÷", "×", "⇄", "➜", "♪", "⌘",
)

val FREQ_OPTIONS: Map<String, String> = linkedMapOf(
    "daily" to "Daily",
    "weekly" to "Weekly",
    "monthly" to "Monthly",
)
