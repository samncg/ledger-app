package com.ledger.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.ledger.app.data.Piggy
import com.ledger.app.data.Prefs
import com.ledger.app.data.Repository
import com.ledger.app.data.defaultCatColors
import com.ledger.app.data.defaultCategories
import com.ledger.app.ui.parseColor
import com.ledger.app.ui.t
import com.ledger.app.util.addDays
import com.ledger.app.util.parseDateOrNull
import com.ledger.app.util.todayKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/* ═══════════════════════════════════════════
   WIDGET SUPPORT — shared plumbing for the
   home-screen widgets that sit alongside
   LedgerWidget (pie, budget, piggy, streak).
   ═══════════════════════════════════════════ */

private const val TAG = "LedgerWidget"
private const val MAX_PERIOD_DAYS = 400
private const val MAX_STREAK_DAYS = 3650
private val FALLBACK_CAT_COLOR = 0xFF7C8896.toInt()

/** Secondary text in the dark theme (the palette's dim covers light) and the over-budget red. */
val WIDGET_DIM = 0xFF9A9A9A.toInt()
val WIDGET_OVER = 0xFFFF5C5C.toInt()

/** The colours the pie's ring/legend paints with. The backdrop and the bars are drawables. */
data class WidgetPalette(val text: Int, val dim: Int, val accent: Int)

/** Dark keeps the shipped look; light swaps in dark text on a near-white panel. */
fun widgetPalette(dark: Boolean): WidgetPalette =
    if (dark) {
        WidgetPalette(text = 0xFFFFFFFF.toInt(), dim = WIDGET_DIM, accent = 0xFFFFFFFF.toInt())
    } else {
        WidgetPalette(text = 0xFF1A1A17.toInt(), dim = 0xFF6B6B66.toInt(), accent = 0xFF1A1A17.toInt())
    }

/** One pie/legend row: a category label, its ARGB colour and its share of period spend. */
data class WidgetSlice(val label: String, val value: Double, val pct: Double, val color: Int)

/**
 * The slice of persisted state the widgets render. Derived values mirror
 * `LedgerViewModel.computeDerived` so the numbers agree with the dashboard, and every
 * field has a safe default so a missing blob renders a placeholder instead of crashing.
 */
data class WidgetSnapshot(
    val ready: Boolean = false,
    val cur: String = "MYR",
    val catLabels: Map<String, String> = emptyMap(),
    val catColors: Map<String, String> = defaultCatColors,
    val categoryTotals: Map<String, Double> = emptyMap(),
    val periodSpent: Double = 0.0,
    /** 0 when no budget is set — never divide by it without checking. */
    val effectiveMonthlyBudget: Double = 0.0,
    /** Uncapped spend/budget ratio, so "over budget" reads as > 100. */
    val budgetPct: Double = 0.0,
    val piggyName: String = "Piggy bank",
    val piggySaved: Double = 0.0,
    val piggyTarget: Double = 0.0,
    /** Already capped at 100 for progress bars. */
    val piggyPct: Double = 0.0,
    val spendStreak: Int = 0,
    val loggedToday: Boolean = false,
    val pieThickness: Float = 3.6f,
    val pieGap: Float = 0f,
    /** Widget theme: true = dark (the shipped look), false = light. */
    val widgetDark: Boolean = true,
) {
    /** Up to [max] legend rows, biggest spend first; the tail is folded into "Other". */
    fun periodSlices(max: Int = 4): List<WidgetSlice> {
        val entries = categoryTotals.entries.filter { it.value > 0.0 }.sortedByDescending { it.value }
        val pool = entries.sumOf { it.value }
        if (pool <= 0.0) return emptyList()
        fun row(id: String, value: Double) =
            WidgetSlice(labelOf(catLabels, id), value, value / pool * 100.0, colorOf(catColors[id]))

        if (entries.size <= max) return entries.map { row(it.key, it.value) }
        val head = entries.take((max - 1).coerceAtLeast(1)).map { row(it.key, it.value) }
        val rest = entries.drop(max - 1).sumOf { it.value }
        return head + WidgetSlice(t("widget.other"), rest, rest / pool * 100.0, colorOf(catColors["other"]))
    }
}

private fun labelOf(labels: Map<String, String>, id: String): String =
    labels[id]?.takeIf { it.isNotBlank() } ?: id.replaceFirstChar { it.uppercaseChar() }

private fun colorOf(hex: String?): Int = parseColor(hex)?.toArgb() ?: FALLBACK_CAT_COLOR

/**
 * The style half of a snapshot for [LedgerWidget], which reads DataStore itself rather than
 * going through [loadWidgetSnapshot]. Never throws — a missing pref falls back to dark.
 */
fun widgetStyleFrom(prefs: Prefs?): WidgetSnapshot = WidgetSnapshot(widgetDark = prefs?.widgetDark ?: true)

/** Force a colour's alpha channel to [alpha] (0..255), keeping its RGB. */
internal fun withAlpha(color: Int, alpha: Int): Int =
    (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

/**
 * RemoteViews can't swap a `ProgressBar`'s `progressDrawable` at runtime (there is no
 * int-taking remotable setter for it), so every widget layout ships one bar per fill and the
 * provider toggles visibility instead. Drives each id in [bars] to the same [progress] and
 * shows only [shown] — all of them stay hidden when it is null.
 */
fun showOnlyBar(views: RemoteViews, bars: List<Int>, shown: Int?, progress: Int) {
    bars.forEach { id ->
        views.setProgressBar(id, 100, progress, false)
        views.setViewVisibility(id, if (id == shown) View.VISIBLE else View.GONE)
    }
}

/**
 * Read + derive the values the widgets need, off the same DataStore the app uses.
 * Never throws: an unreadable blob falls back to the defaults so a widget shows "—"
 * rather than taking the process down.
 */
suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
    val data = runCatching { Repository(context).load() }.getOrElse {
        Log.w(TAG, "Couldn't read widget data", it)
        return WidgetSnapshot()
    }

    val settings = data.settings
    val prefs = data.prefs
    /* A widget can be woken with the app process dead, so pick the stored language up
       here, before anything renders, instead of relying on the dashboard having run. */
    com.ledger.app.ui.Strings.setLang(prefs?.lang?.takeIf { it.isNotBlank() } ?: "en")
    val cur = prefs?.currency?.ifEmpty { "MYR" } ?: "MYR"
    val categories = data.categories ?: defaultCategories()
    val expenses = data.expenses.orEmpty()
    val today = todayKey()

    val catLabels = categories.associate { it.id to it.label }
    val catColors = data.theme?.catColors ?: defaultCatColors

    /* Period spend — the same window computeDerived walks: settings.startDate for
       periodDays days, ignoring days that haven't happened yet. The loop is clamped so a
       corrupt periodDays can't spin. */
    val periodDates = HashSet<String>()
    var spent = 0.0
    val start = settings?.let { parseDateOrNull(it.startDate) }
    if (settings != null && start != null) {
        val spentByDay = expenses.groupBy({ it.date }, { it.amount }).mapValues { (_, v) -> v.sum() }
        for (i in 0 until settings.periodDays.coerceIn(1, MAX_PERIOD_DAYS)) {
            val date = start.plusDays(i.toLong()).toString()
            if (date > today) continue
            periodDates += date
            spent += spentByDay[date] ?: 0.0
        }
    }
    val periodSpent = if (spent.isFinite()) spent else 0.0

    val categoryTotals = LinkedHashMap<String, Double>()
    for (e in expenses) {
        if (e.date !in periodDates) continue
        val cat = e.categories.firstOrNull() ?: e.category ?: "other"
        categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + e.amount
    }

    val topUpTotal = data.topUps.orEmpty().filter { it.amount.isFinite() }.sumOf { it.amount }
    val budget = settings?.let { it.monthlyBudget + topUpTotal } ?: 0.0
    val effectiveMonthlyBudget = if (budget.isFinite() && budget > 0.0) budget else 0.0

    val piggy = data.piggy ?: data.piggies?.firstOrNull() ?: Piggy()
    val piggySaved = if (piggy.saved.isFinite() && piggy.saved > 0.0) piggy.saved else 0.0
    val piggyTarget = if (piggy.target.isFinite() && piggy.target > 0.0) piggy.target else 0.0

    /* Daily spend streak — same rule as computeDerived: consecutive days with a logged
       expense, with prefs.streakGrace missed days forgiven. */
    val spendDays = expenses.mapNotNull { parseDateOrNull(it.date)?.toString() }.toHashSet()
    val loggedToday = today in spendDays
    val grace = (prefs?.streakGrace ?: 0).coerceIn(0, 60)
    var spendStreak = 0
    var cursor = if (loggedToday) today else addDays(today, -1)
    var misses = 0
    while (spendStreak < MAX_STREAK_DAYS) {
        if (cursor in spendDays) {
            spendStreak++
            cursor = addDays(cursor, -1)
        } else if (misses < grace) {
            misses++
            cursor = addDays(cursor, -1)
        } else break
    }

    return WidgetSnapshot(
        ready = settings != null,
        cur = cur,
        catLabels = catLabels,
        catColors = catColors,
        categoryTotals = categoryTotals,
        periodSpent = periodSpent,
        effectiveMonthlyBudget = effectiveMonthlyBudget,
        budgetPct = if (effectiveMonthlyBudget > 0.0) periodSpent / effectiveMonthlyBudget * 100.0 else 0.0,
        piggyName = piggy.name.ifBlank { "Piggy bank" },
        piggySaved = piggySaved,
        piggyTarget = piggyTarget,
        piggyPct = if (piggyTarget > 0.0) (piggySaved / piggyTarget * 100.0).coerceAtMost(100.0) else 0.0,
        spendStreak = spendStreak,
        loggedToday = loggedToday,
        pieThickness = prefs?.pieThickness?.takeIf { it.isFinite() } ?: 3.6f,
        pieGap = prefs?.pieGap?.takeIf { it.isFinite() } ?: 0f,
        widgetDark = prefs?.widgetDark ?: true,
    )
}

/**
 * Shared plumbing for the widgets: reads DataStore off the main thread (exactly like
 * [LedgerWidget]) and wires the tap-anywhere-to-open intent.
 */
abstract class BaseWidget : AppWidgetProvider() {

    /** Build this widget's RemoteViews. Runs off the main thread. */
    abstract suspend fun buildViews(context: Context): RemoteViews

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // onUpdate runs on the main thread — do the DataStore read off it.
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Never let a bad read take the process down; the widget keeps its old
                // content and the next tick tries again.
                val views = runCatching { buildViews(appContext) }
                    .onFailure { Log.w(TAG, "Couldn't render widget", it) }
                    .getOrNull()
                if (views != null) ids.forEach { manager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    /** Tap anywhere on the widget to open the app. */
    protected fun attachTap(context: Context, views: RemoteViews, rootId: Int) {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tap = PendingIntent.getActivity(
            context,
            rootId,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(rootId, tap)
    }
}

/**
 * Re-broadcasts APPWIDGET_UPDATE so placed widgets re-render from the latest data.
 * [LedgerWidget] keeps its own `refresh`; [NEW_WIDGETS] covers the four widgets added
 * alongside it, and the periodic worker refreshes both.
 */
object WidgetRefresher {

    val NEW_WIDGETS: List<Class<*>> = listOf(
        PieWidget::class.java,
        BudgetWidget::class.java,
        PiggyWidget::class.java,
        StreakWidget::class.java,
    )

    fun refreshNew(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        NEW_WIDGETS.forEach { cls ->
            val ids = manager.getAppWidgetIds(ComponentName(context, cls))
            if (ids.isEmpty()) return@forEach
            context.sendBroadcast(
                Intent(context, cls).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                },
            )
        }
    }
}
