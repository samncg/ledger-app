package com.ledger.app.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/* ═══════════════════════════════════════════
   DATA MODEL — same shapes as the web app's
   localStorage (ledger-* keys), so backups
   move between platforms unchanged.
   ═══════════════════════════════════════════ */

/**
 * Accepts string ids, or bare numbers — older backups used numeric ids
 * (e.g. 1785579808521 instead of "1785579808521abc1").
 */
object StringOrNumberSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrNumber", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)

    override fun deserialize(decoder: Decoder): String {
        val json = decoder as? JsonDecoder
        return if (json != null) {
            val el = json.decodeJsonElement()
            if (el is JsonPrimitive && el !is JsonNull) el.content else el.toString()
        } else {
            decoder.decodeString()
        }
    }
}

@Serializable
data class Expense(
    @Serializable(with = StringOrNumberSerializer::class)
    val id: String,
    val date: String,
    val amount: Double,
    val categories: List<String> = emptyList(),
    val category: String? = null,
    val note: String = "",
)

@Serializable
data class TopUp(
    @Serializable(with = StringOrNumberSerializer::class)
    val id: String,
    val amount: Double,
    val date: String,
    val note: String = "",
)

@Serializable
data class Balance(val start: Double = 0.0)

@Serializable
data class Piggy(
    @Serializable(with = StringOrNumberSerializer::class)
    val id: String = "default",
    val name: String = "Piggy bank",
    val target: Double = 0.0,
    val saved: Double = 0.0,
    val texture: String? = null,
    val soundId: String? = "coin",
    val soundCustom: String? = null,
)

@Serializable
data class Rule(
    @Serializable(with = StringOrNumberSerializer::class)
    val id: String,
    val type: String,      // "expense" | "budget" | "balance"
    val amount: Double,
    val category: String = "",
    val note: String = "",
    val freq: String,      // "daily" | "weekly" | "monthly"
    val start: String,
    val last: String? = null,
    val active: Boolean = true,
)

@Serializable
data class Settings(
    val monthlyBudget: Double,
    val periodDays: Int,
    val startDate: String,
)

@Serializable
data class Category(val id: String, val label: String, val glyph: String)

@Serializable
data class AppTheme(
    val bg: String = "#000000",
    val surface: String = "#000000",
    val surface2: String = "#0a0a0a",
    val text: String = "#ffffff",
    val textDim: String = "#9a9a9a",
    val textMuted: String = "#5c5c5c",
    val border: String = "rgba(255,255,255,0.10)",
    val borderStrong: String = "rgba(255,255,255,0.22)",
    val accent: String = "#ffffff",
    val accentFg: String = "#000000",
    val negative: String = "#ff5c5c",
    val warning: String = "#e8c15a",
    val positive: String = "#5bd488",
    val catColors: Map<String, String> = defaultCatColors,
)

@Serializable
data class Prefs(
    val currency: String = "MYR",
    val compact: Boolean = false,
    val pieThickness: Float = 3.6f,
    val pieGap: Float = 0f,
    val groupHistory: Boolean = true,
    val trendStyle: String = "line", // "line" | "heatmap"
    val heatColors: Map<String, String> = defaultHeatColors,
    val font: String = "default",    // default | sans | serif | mono | cursive
    val cardOrder: List<String> = defaultCardOrder,
    val balancesEnabled: Boolean = true,
    val heroMode: String = "daily",  // "daily" | "balance"
    val wallpaper: String? = null,
    val wallpaperDim: Int = 60,
    val wallBlur: Int = 0,
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val budgetAlertsEnabled: Boolean = true,
    val glassEnabled: Boolean = false,
    val glassBlur: Int = 8,
    val glassOpacity: Int = 76,
    val glassRefraction: Int = 24,
    val glassRefractionHeight: Int = 12,
    val glassChromaticAberration: Boolean = false,
)

@Serializable
data class AuthUser(
    val uid: String,
    val name: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
)

/* ─── Derived (non-persisted) types ─── */

data class Cat(val id: String, val label: String, val glyph: String, val color: String)

data class DayCell(
    val date: String,
    val spent: Double,
    val delta: Double,
    val isFuture: Boolean,
    val isToday: Boolean,
)

data class FrequentEntry(
    val category: String,
    val amount: Double,
    val note: String,
    val count: Int,
    val last: String,
)

/** Enforce single-category selection across all expenses */
fun normalizeExpense(e: Expense): Expense {
    val cat = (e.categories.firstOrNull() ?: e.category) ?: "food"
    return e.copy(category = cat, categories = listOf(cat))
}

fun normalizeExpenses(list: List<Expense>): List<Expense> = list.map(::normalizeExpense)

/** Effective category list for an expense — returns single category */
fun expCats(e: Expense): List<String> {
    val cat = (e.categories.firstOrNull() ?: e.category) ?: "food"
    return listOf(cat)
}

/* ─── Defaults ─── */

fun defaultCategories(): List<Category> = listOf(
    Category("food", "Food", "◇"),
    Category("transport", "Transport", "→"),
    Category("social", "Social", "◎"),
    Category("shopping", "Shopping", "□"),
    Category("other", "Other", "·"),
)

val defaultCatColors = mapOf(
    "food" to "#5b9fd4",
    "transport" to "#e8c15a",
    "social" to "#ff6b8b",
    "shopping" to "#a68bfa",
    "other" to "#5bd488",
)

val defaultHeatColors = mapOf(
    "l0" to "transparent",
    "l1" to "#9be9a8",
    "l2" to "#40c463",
    "l3" to "#30a14e",
    "l4" to "#216e39",
)

val defaultCardOrder = listOf("log", "breakdown", "trend", "history", "auto", "piggy", "backup")

/**
 * Cards rendered on the dashboard. Log-spend and History live behind their
 * top-bar buttons, so they're filtered out here; piggy hides when the bank
 * balance system is off.
 */
fun dashboardCardOrder(stored: List<String>, balancesOn: Boolean): List<String> {
    val base =
        if (stored.size == defaultCardOrder.size && stored.toSet() == defaultCardOrder.toSet()) stored else defaultCardOrder
    return base.filter { it != "log" && it != "history" }.filter { balancesOn || it != "piggy" }
}
