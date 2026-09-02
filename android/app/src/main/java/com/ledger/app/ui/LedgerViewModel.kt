package com.ledger.app.ui

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.ledger.app.data.AppTheme
import com.ledger.app.data.AuthUser
import com.ledger.app.data.Balance
import com.ledger.app.data.Cat
import com.ledger.app.data.Category
import com.ledger.app.data.DayCell
import com.ledger.app.data.Expense
import com.ledger.app.data.FirebaseConfig
import com.ledger.app.data.FirebaseManager
import com.ledger.app.data.FirebaseSyncSerializer
import com.ledger.app.data.FrequentEntry
import com.ledger.app.data.Piggy
import com.ledger.app.data.Prefs
import com.ledger.app.data.Repository
import com.ledger.app.data.Rule
import com.ledger.app.data.Settings
import com.ledger.app.data.TopUp
import com.ledger.app.data.defaultCategories
import com.ledger.app.data.expCats
import com.ledger.app.util.NotificationHelper
import com.ledger.app.util.advanceDate
import com.ledger.app.util.addDays
import com.ledger.app.util.dayDiff
import com.ledger.app.util.daysInMonth
import com.ledger.app.util.firstOfMonthKey
import com.ledger.app.util.fmt
import com.ledger.app.util.groupLabel
import com.ledger.app.util.monthLabel
import com.ledger.app.util.parseDate
import com.ledger.app.util.relativeDate
import com.ledger.app.util.todayKey
import com.ledger.app.util.uid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/* ═══════════════════════════════════════════
   STATE
   ═══════════════════════════════════════════ */

data class LedgerState(
    val ready: Boolean = false,
    val theme: AppTheme = DEFAULT_THEME,
    val savedTheme: AppTheme? = null,
    val prefs: Prefs = Prefs(),
    val settings: Settings? = null,
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = defaultCategories(),
    val catBudgets: Map<String, Double> = emptyMap(),
    val topUps: List<TopUp> = emptyList(),
    val balance: Balance = Balance(),
    val piggy: Piggy = Piggy(),
    val piggies: List<Piggy> = listOf(Piggy()),
    val activePiggyId: String = "default",
    val recurring: List<Rule> = emptyList(),
    /* derived */
    val cats: List<Cat> = emptyList(),
    val cur: String = "MYR",
    val balancesOn: Boolean = true,
    val heroMode: String = "daily",
    val today: String = "",
    val topUpTotal: Double = 0.0,
    val effectiveMonthlyBudget: Double = 0.0,
    val dailyBudget: Double = 0.0,
    val dayCells: List<DayCell> = emptyList(),
    val elapsedDays: Int = 0,
    val runningBalance: Double = 0.0,
    val todaySpent: Double = 0.0,
    val todayRemaining: Double = 0.0,
    val bankedSoFar: Double = 0.0,
    val bankBalance: Double = 0.0,
    val todaySaved: Double = 0.0,
    val heroLabel: String = "",
    val heroValue: Double = 0.0,
    val piggyPct: Double = 0.0,
    val periodSpent: Double = 0.0,
    val budgetPctFull: Double = 0.0,
    val avgDailySpend: Double = 0.0,
    val daysOver: Int = 0,
    val projectedTotal: Double = 0.0,
    val projectedDelta: Double = 0.0,
    val streak: Int = 0,
    val frequentEntries: List<FrequentEntry> = emptyList(),
    val authUser: AuthUser? = null,
    val syncError: Boolean = false,
    val syncErrorMsg: String = "",
    val lastSyncedAt: Long = 0L,
    val isFirebaseConfigured: Boolean = FirebaseConfig.isConfigured,
) {
    val activePiggy: Piggy get() = piggies.find { it.id == activePiggyId } ?: piggies.firstOrNull() ?: Piggy()
}

data class ToastMsg(val id: Long, val msg: String, val type: String, val action: ToastAction? = null)
data class ToastAction(val label: String, val run: () -> Unit)
data class ConfirmReq(val title: String, val msg: String, val onConfirm: () -> Unit, val onCancel: () -> Unit)

/* ─── Breakdown ─── */
data class PieSlice(
    val id: String,
    val label: String,
    val color: String,
    val value: Double,
    val pct: Double,
    val dash: Double,
    val offset: Double
)

data class BreakdownData(
    val txnCount: Int,
    val categoryTotals: Map<String, Double>,
    val maxCategory: Double,
    val biggestInRange: Expense?,
    val totalSpent: Double,
    val rangeDays: Long,
    val rangeBudget: Double,
    val budgetPct: Double,
    val avgPerDay: Double,
    val topCategory: Cat?,
    val rangeLabel: String,
    val pieSlices: List<PieSlice>,
)

/* ─── Trend ─── */
data class TrendDay(val date: String, var total: Double, val byCat: MutableMap<String, Double>)
data class TrendSeries(val id: String, val label: String, val color: String, val value: (TrendDay) -> Double)
data class HeatCell(val date: String, val spent: Double, val level: Int)
data class HeatData(val weeks: Int, val total: Double, val cells: List<HeatCell>)
data class TrendData(
    val days: List<TrendDay>,
    val series: List<TrendSeries>,
    val max: Double,
    val budget: Double,
    val heat: HeatData
)

/* ─── History ─── */
data class HistoryEntry(
    val type: String, // "expense" | "topup"
    val id: String,
    val date: String,
    val amount: Double,
    val note: String,
    val categories: List<String> = emptyList(),
    val category: String? = null,
)

data class HistoryGroup(val label: String?, val items: List<HistoryEntry>, val total: Double)
data class HistoryData(
    val entries: List<HistoryEntry>,
    val spentTotal: Double,
    val toppedTotal: Double,
    val activeFilterCount: Int,
    val groups: List<HistoryGroup>,
)

fun entryCats(e: HistoryEntry): List<String> =
    if (e.categories.isNotEmpty()) e.categories else e.category?.let { listOf(it) } ?: emptyList()

/* ═══════════════════════════════════════════
   VIEW MODEL — state machine ported from the
   web app's App() component
   ═══════════════════════════════════════════ */

class LedgerViewModel(private val repo: Repository) : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true; coerceInputValues = true; isLenient =
        true
    }

    private val _state = MutableStateFlow(LedgerState())
    val state: StateFlow<LedgerState> = _state

    /* ─── Log form (shared by LogCard and HistoryCard) ─── */
    var amount by mutableStateOf("")
    var note by mutableStateOf("")
    var entryDate by mutableStateOf(todayKey())
    var selCats by mutableStateOf(listOf("food"))
    var editingId by mutableStateOf<String?>(null); private set

    /* ─── Toast & confirm ─── */
    var toast by mutableStateOf<ToastMsg?>(null); private set
    var confirm by mutableStateOf<ConfirmReq?>(null)

    fun toggleSelCat(id: String) {
        // Single-select — a new choice replaces the previous one.
        selCats = listOf(id)
    }

    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var snapshotListener: ListenerRegistration? = null
    private var pushJob: Job? = null
    private var lastPushedJson: String? = null
    private var lastSyncPushTime: Long = 0L
    private var skipNextPush: Boolean = false
    private var syncedUid: String? = null

    init {
        viewModelScope.launch {
            load()
            initFirebaseSync()
        }
    }

    /* ─── Loading & persistence plumbing ─── */

    private suspend fun load() {
        val d = repo.load()
        val rawExpenses = d.expenses ?: emptyList()
        val normalizedExpenses = com.ledger.app.data.normalizeExpenses(rawExpenses)
        if (rawExpenses != normalizedExpenses) {
            viewModelScope.launch { repo.saveExpenses(normalizedExpenses) }
        }
        var s = LedgerState(
            ready = true,
            theme = d.theme ?: DEFAULT_THEME,
            savedTheme = d.savedTheme,
            prefs = d.prefs ?: Prefs(),
            settings = d.settings,
            expenses = normalizedExpenses,
            categories = d.categories ?: defaultCategories(),
            catBudgets = d.catBudgets ?: emptyMap(),
            topUps = d.topUps ?: emptyList(),
            balance = d.balance ?: Balance(),
            piggy = (d.piggies ?: d.piggy?.let { listOf(it) } ?: listOf(Piggy())).first(),
            piggies = d.piggies ?: d.piggy?.let { listOf(it) } ?: listOf(Piggy()),
            activePiggyId = (d.piggies ?: d.piggy?.let { listOf(it) } ?: listOf(Piggy())).first().id,
            recurring = d.recurring ?: emptyList(),
        )
        val materialized = runRecurring(s)
        if (materialized != null) s = materialized
        s = syncMonthlyPeriod(s)
        _state.value = computeDerived(s)
        if (materialized != null) persistSliceChanges(materialized)
        // Push any period rollover so the cloud doesn't keep the stale start date.
        triggerDebouncedPush()

        if (s.prefs.notificationsEnabled) {
            NotificationHelper.scheduleDailyReminder(repo.appContext, s.prefs.reminderHour, s.prefs.reminderMinute)
        }
    }

    /** Keep the budget period equal to the current calendar month: realign the start to
    the 1st when the month rolls over, and sync periodDays to the month length. */
    private suspend fun syncMonthlyPeriod(s: LedgerState): LedgerState {
        val settings = s.settings ?: return s
        val today = parseDate(todayKey())
        val real = daysInMonth(today)
        val firstToday = firstOfMonthKey(today)
        val firstStart = firstOfMonthKey(parseDate(settings.startDate))
        // If we've crossed into a new month, the old period is stale — roll it over.
        val needRealign = firstStart != firstToday
        if (!needRealign && settings.periodDays == real) return s
        val next = settings.copy(
            periodDays = real,
            startDate = if (needRealign) firstToday else settings.startDate
        )
        repo.saveSettings(next)
        return s.copy(settings = next)
    }

    private fun update(f: (LedgerState) -> LedgerState) {
        _state.value = computeDerived(f(_state.value))
        triggerDebouncedPush()
    }

    private suspend fun persistSliceChanges(s: LedgerState) {
        repo.saveExpenses(s.expenses)
        repo.saveTopUps(s.topUps)
        repo.saveBalance(s.balance)
        repo.saveRecurring(s.recurring)
    }

    /** Recompute all derived values (call on resume so the date rolls over). */
    fun refresh() {
        update { it }
        val cur = _state.value
        cur.settings?.let { settings ->
            val today = parseDate(todayKey())
            val real = daysInMonth(today)
            val firstToday = firstOfMonthKey(today)
            val firstStart = firstOfMonthKey(parseDate(settings.startDate))
            val needRealign = firstStart != firstToday
            if (needRealign || settings.periodDays != real) {
                val next = settings.copy(
                    periodDays = real,
                    startDate = if (needRealign) firstToday else settings.startDate
                )
                update { it.copy(settings = next) }
                viewModelScope.launch { repo.saveSettings(next) }
            }
        }
    }

    /* ─── Derived data (ported 1:1 from the web app) ─── */

    private fun computeDerived(s: LedgerState): LedgerState {
        val today = todayKey()
        val cur = s.prefs.currency.ifEmpty { "MYR" }
        val balancesOn = s.prefs.balancesEnabled
        val heroMode = if (balancesOn && s.prefs.heroMode == "balance") "balance" else "daily"
        val cats = s.categories.map { Cat(it.id, it.label, it.glyph, s.theme.catColors[it.id] ?: "#7c8896") }
        val topUpTotal = s.topUps.sumOf { it.amount }
        val effectiveMonthlyBudget = s.settings?.let { it.monthlyBudget + topUpTotal } ?: 0.0
        val dailyBudget = s.settings?.let { effectiveMonthlyBudget / it.periodDays } ?: 0.0
        val spentByDay = s.expenses.groupBy({ it.date }, { it.amount }).mapValues { (_, v) -> v.sum() }

        val (dayCells, elapsedDays, runningBalance) = if (s.settings == null) {
            Triple(emptyList<DayCell>(), 0, 0.0)
        } else {
            val elapsed =
                (dayDiff(s.settings.startDate, today) + 1).coerceIn(1L, s.settings.periodDays.toLong()).toInt()
            var running = 0.0
            val cells = (0 until s.settings.periodDays).map { i ->
                val date = addDays(s.settings.startDate, i)
                val isFuture = i >= elapsed
                val spent = spentByDay[date] ?: 0.0
                val delta = if (isFuture) 0.0 else dailyBudget - spent
                if (!isFuture) running += delta
                DayCell(date, spent, delta, isFuture, date == today)
            }
            Triple(cells, elapsed, running)
        }

        val todaySpent = spentByDay[today] ?: 0.0
        val todayRemaining = dailyBudget - todaySpent

        /* Bank balance = starting money, plus the leftover allowance banked at the end
           of each day, minus money moved over to the monthly budget.
           Leftovers use the CURRENT net dailyBudget (monthlyBudget + net topUps), so it
           always agrees with the "Daily allowance" shown — no per-day topUp inflation. */
        val bankedSoFar = if (s.settings == null) 0.0 else {
            var banked = 0.0
            for (c in dayCells) {
                if (c.isFuture) break
                val left = dailyBudget - c.spent
                // Overspends either drain the bank balance (bank the negative leftover)
                // or come out of the total budget (bank only the positive leftover),
                // controlled by prefs.overspendFromBalance.
                banked += if (s.prefs.overspendFromBalance) left else max(0.0, left)
            }
            banked
        }
        val bankBalance = s.balance.start - topUpTotal + bankedSoFar
        val todaySaved = max(0.0, todayRemaining)
        val activePiggy = s.piggies.find { it.id == s.activePiggyId } ?: s.piggies.firstOrNull() ?: Piggy()
        val piggyPct = if (activePiggy.target > 0) min(100.0, activePiggy.saved / activePiggy.target * 100) else 0.0
        val heroLabel = if (heroMode == "balance") "Balance" else "Available today"
        val heroValue = if (heroMode == "balance") bankBalance else todayRemaining
        val periodSpent = dayCells.filter { !it.isFuture }.sumOf { it.spent }
        val budgetPctFull =
            if (effectiveMonthlyBudget > 0) min(100.0, periodSpent / effectiveMonthlyBudget * 100) else 0.0

        val avgDailySpend = if (s.settings != null && elapsedDays > 0)
            dayCells.filter { !it.isFuture }.sumOf { it.spent } / elapsedDays else 0.0
        val daysOver = dayCells.count { !it.isFuture && it.delta < 0 }
        val projectedTotal = s.settings?.let { avgDailySpend * it.periodDays } ?: 0.0
        val projectedDelta = s.settings?.let { effectiveMonthlyBudget - projectedTotal } ?: 0.0

        /* Frequent / smart quick-log suggestions */
        val frequentEntries = s.expenses
            .groupBy { "${it.category}|${it.amount}|${it.note.trim().lowercase()}" }
            .map { (_, es) ->
                val e0 = es.first()
                FrequentEntry(e0.category ?: "", e0.amount, e0.note, es.size, es.maxOf { it.date })
            }
            .filter { it.count > 1 }
            .sortedWith(compareByDescending<FrequentEntry> { it.count }.thenByDescending { it.last })
            .take(4)

        val streak = if (s.settings == null) 0 else {
            var c = 0
            for (cell in dayCells.filter { !it.isFuture }.asReversed()) {
                if (cell.delta >= 0) c++ else break
            }
            c
        }

        return s.copy(
            cats = cats, cur = cur, balancesOn = balancesOn, heroMode = heroMode, today = today,
            topUpTotal = topUpTotal, effectiveMonthlyBudget = effectiveMonthlyBudget, dailyBudget = dailyBudget,
            dayCells = dayCells, elapsedDays = elapsedDays, runningBalance = runningBalance,
            todaySpent = todaySpent, todayRemaining = todayRemaining, bankedSoFar = bankedSoFar,
            bankBalance = bankBalance, todaySaved = todaySaved, heroLabel = heroLabel, heroValue = heroValue,
            piggyPct = piggyPct,
            periodSpent = periodSpent, budgetPctFull = budgetPctFull, avgDailySpend = avgDailySpend,
            daysOver = daysOver, projectedTotal = projectedTotal, projectedDelta = projectedDelta,
            streak = streak, frequentEntries = frequentEntries,
        )
    }

    /* ─── Breakdown (range-driven) ─── */
    fun breakdown(s: LedgerState, range: String, from: String, to: String, end: String = s.today): BreakdownData {
        val start = when (range) {
            "week" -> addDays(end, -6)
            "month" -> firstOfMonthKey(parseDate(end))
            "all" -> null
            "custom" -> from.ifEmpty { null }
            else -> s.settings?.startDate ?: end
        }
        val effEnd = if (range == "custom") to.ifEmpty { end } else end
        val ovExp = s.expenses.filter { e -> (start == null || e.date >= start) && e.date <= effEnd }

        val totals = linkedMapOf<String, Double>().apply { s.cats.forEach { put(it.id, 0.0) } }
        for (e in ovExp) for (c in expCats(e)) totals[c] = (totals[c] ?: 0.0) + e.amount

        val maxCategory = max(1.0, totals.values.maxOrNull() ?: 0.0)
        val biggest = ovExp.maxByOrNull { it.amount }
        val totalSpent = ovExp.sumOf { it.amount }
        val rangeDays = when {
            start != null -> max(1L, dayDiff(start, effEnd) + 1)
            ovExp.isEmpty() -> 1L
            else -> max(1L, dayDiff(ovExp.minOf { it.date }, effEnd) + 1)
        }
        val rangeBudget = s.dailyBudget * rangeDays
        val budgetPct = if (rangeBudget > 0) totalSpent / rangeBudget * 100 else 0.0
        val avgPerDay = totalSpent / rangeDays
        val topCategory = s.cats.filter { (totals[it.id] ?: 0.0) > 0 }.maxByOrNull { totals[it.id] ?: 0.0 }
        val rangeLabel = when (range) {
            "period" -> "this budget period"; "week" -> "the last 7 days"
            "month" -> monthLabel(end, 0); "all" -> "all logged history"
            else -> "the selected range"
        }
        val pool = totals.values.sum()
        val pieSlices = if (pool <= 0) emptyList() else {
            var cumulative = 0.0
            s.cats.map { c ->
                val v = totals[c.id] ?: 0.0
                val frac = v / pool
                val dash = frac * 100
                val slice = PieSlice(c.id, c.label, c.color, v, frac * 100, dash, cumulative)
                cumulative += dash
                slice
            }.filter { it.value > 0 }
        }
        return BreakdownData(
            ovExp.size, totals, maxCategory, biggest, totalSpent, rangeDays, rangeBudget,
            budgetPct, avgPerDay, topCategory, rangeLabel, pieSlices,
        )
    }

    /* ─── Trend chart data ─── */
    fun trend(s: LedgerState, range: Int, series: List<String>, end: String = s.today): TrendData {
        val map = linkedMapOf<String, TrendDay>()
        for (i in range - 1 downTo 0) {
            val date = addDays(end, -i)
            map[date] = TrendDay(date, 0.0, mutableMapOf())
        }
        for (e in s.expenses) {
            val d = map[e.date] ?: continue
            d.total += e.amount
            for (c in expCats(e)) d.byCat[c] = (d.byCat[c] ?: 0.0) + e.amount
        }
        val list = mutableListOf<TrendSeries>()
        if (series.contains("__total__")) list.add(TrendSeries("__total__", "Total", s.theme.accent) { d -> d.total })
        for (id in series) {
            if (id == "__total__") continue
            val c = s.cats.find { it.id == id } ?: continue
            list.add(TrendSeries(id, c.label, c.color) { d -> d.byCat[id] ?: 0.0 })
        }
        val maxValue =
            max(s.dailyBudget * 1.3, map.values.flatMap { d -> list.map { it.value(d) } }.maxOrNull() ?: 0.0).let {
                max(
                    it,
                    1.0
                )
            }

        /* GitHub-style spending heatmap data (weeks as columns, Mon–Sun rows) */
        val endDate = parseDate(end)
        val start = endDate.minusDays((range - 1).toLong())
        val first =
            start.minusDays((((start.dayOfWeek.value % 7) + 6) % 7).toLong()) // Monday on/before the range start
        val spentByDay = s.expenses.groupBy({ it.date }, { it.amount }).mapValues { (_, v) -> v.sum() }
        val cells = mutableListOf<HeatCell>()
        var total = 0.0;
        var mx = 0.0
        var d = first
        while (!d.isAfter(endDate)) {
            val date = todayKey(d)
            val spent = spentByDay[date] ?: 0.0
            total += spent
            if (spent > mx) mx = spent
            cells.add(HeatCell(date, spent, 0))
            d = d.plusDays(1)
        }
        val top = if (mx > 0) mx else 1.0
        val leveled =
            cells.map { it.copy(level = if (it.spent == 0.0) 0 else min(4, max(1, ceil(it.spent / top * 4).toInt()))) }
        return TrendData(
            map.values.toList(), list, maxValue, s.dailyBudget,
            HeatData(ceil(leveled.size / 7.0).toInt(), total, leveled),
        )
    }

    /* ─── History (merge + filter + sort + group) ─── */
    fun history(
        s: LedgerState,
        filterCats: List<String>,
        search: String,
        dateFrom: String,
        dateTo: String,
        sort: String,
        group: Boolean,
    ): HistoryData {
        var list: List<HistoryEntry> =
            s.expenses.map { HistoryEntry("expense", it.id, it.date, it.amount, it.note, it.categories, it.category) } +
                    s.topUps.map { HistoryEntry("topup", it.id, it.date, it.amount, it.note) }
        if (filterCats.isNotEmpty()) {
            list = list.filter { it.type == "expense" && entryCats(it).any { c -> filterCats.contains(c) } }
        }
        if (search.isNotBlank()) {
            val q = search.trim().lowercase()
            list = list.filter { e ->
                if (e.type == "topup") {
                    e.note.lowercase().contains(q) || "move to budget".contains(q) ||
                            "top up".contains(q) || "return to balance".contains(q)
                } else {
                    e.note.lowercase().contains(q) || entryCats(e).any { it.lowercase().contains(q) }
                }
            }
        }
        if (dateFrom.isNotEmpty()) list = list.filter { it.date >= dateFrom }
        if (dateTo.isNotEmpty()) list = list.filter { it.date <= dateTo }
        list = when (sort) {
            "date-asc" -> list.sortedWith(compareBy({ it.date }, { it.id }))
            "amount-desc" -> list.sortedByDescending { it.amount }
            "amount-asc" -> list.sortedBy { it.amount }
            else -> list.sortedWith(compareByDescending<HistoryEntry> { it.date }.thenByDescending { it.id })
        }
        val spentTotal = list.filter { it.type != "topup" }.sumOf { it.amount }
        val toppedTotal = list.filter { it.type == "topup" }.sumOf { it.amount }
        val activeFilterCount =
            (if (search.isNotBlank()) 1 else 0) + (if (dateFrom.isNotEmpty() || dateTo.isNotEmpty()) 1 else 0) +
                    (if (sort != "date-desc") 1 else 0) + filterCats.size

        val groups: List<HistoryGroup> = if (!group || sort.startsWith("amount")) {
            listOf(HistoryGroup(null, list, 0.0))
        } else {
            val order = mutableListOf<String>()
            val byLabel = linkedMapOf<String, MutableList<HistoryEntry>>()
            for (e in list) {
                val label = groupLabel(e.date, s.today)
                if (!byLabel.containsKey(label)) {
                    byLabel[label] = mutableListOf(); order.add(label)
                }
                byLabel.getValue(label).add(e)
            }
            order.map { label ->
                val items = byLabel.getValue(label)
                HistoryGroup(label, items, items.filter { it.type != "topup" }.sumOf { it.amount })
            }
        }
        return HistoryData(list, spentTotal, toppedTotal, activeFilterCount, groups)
    }

    /* ─── Automations (recurring entries) ─── */

    /** Materialize any due recurring entries. Returns the new state if changed. */
    private fun runRecurring(s: LedgerState, rules: List<Rule> = s.recurring): LedgerState? {
        val today = todayKey()
        val ex = s.expenses.toMutableList()
        val tu = s.topUps.toMutableList()
        var start = s.balance.start
        var changed = false
        val next = rules.map { r ->
            if (!r.active) return@map r
            val from = r.last?.let { addDays(it, 1) } ?: r.start.ifEmpty { today }
            val cursor = parseDate(from)
            val end = parseDate(today)
            if (cursor.isAfter(end)) return@map r
            val occ = mutableListOf<String>()
            var cur = cursor
            while (!cur.isAfter(end)) {
                occ.add(todayKey(cur)); cur = advanceDate(cur, r.freq)
            }
            for (day in occ) {
                when (r.type) {
                    "expense" -> {
                        val cat = r.category.ifEmpty { "other" }
                        ex.add(
                            Expense(
                                uid(),
                                day,
                                r.amount,
                                listOf(cat),
                                cat,
                                if (r.note.isNotEmpty()) "${r.note} (auto)" else ""
                            )
                        )
                    }

                    "budget" -> tu.add(TopUp(uid(), r.amount, day, if (r.note.isNotEmpty()) "${r.note} (auto)" else ""))
                    else -> start += r.amount
                }
            }
            changed = true
            r.copy(last = occ.last())
        }
        if (!changed) return null
        return s.copy(expenses = ex, topUps = tu, balance = s.balance.copy(start = start), recurring = next)
    }

    fun runRecurringNow() {
        val materialized = runRecurring(_state.value) ?: return
        update { materialized }
        viewModelScope.launch { persistSliceChanges(materialized) }
        showToast("Automated entries added.", "success")
    }

    fun addAutomation(type: String, amountStr: String, catId: String, freq: String, start: String, note: String) {
        val v = amountStr.toDoubleOrNull() ?: return
        if (v <= 0) {
            showToast("Enter a valid amount.", "error"); return
        }
        if (start.isEmpty()) {
            showToast("Pick a start date.", "error"); return
        }
        val rule = Rule(
            uid(),
            type,
            v,
            if (type == "expense") catId else "",
            note.trim(),
            freq,
            start.ifEmpty { _state.value.today },
            null,
            true
        )
        val next = _state.value.recurring + rule
        update { it.copy(recurring = next) }
        viewModelScope.launch { repo.saveRecurring(next) }
        showToast("Automation added.", "success")
        val materialized = runRecurring(_state.value, next) // backfill occurrences up to today
        if (materialized != null) {
            update { materialized }
            viewModelScope.launch { persistSliceChanges(materialized) }
        }
    }

    fun removeAutomation(id: String) {
        val removed = _state.value.recurring.find { it.id == id }
        val next = _state.value.recurring.filter { it.id != id }
        update { it.copy(recurring = next) }
        viewModelScope.launch { repo.saveRecurring(next) }
        if (removed != null) showToast("Automation removed.", "info", ToastAction("Undo") {
            val restored = _state.value.recurring + removed
            update { it.copy(recurring = restored) }
            viewModelScope.launch { repo.saveRecurring(restored) }
            showToast("Restored.", "success")
        })
    }

    fun toggleAutomation(id: String) {
        val r = _state.value.recurring.find { it.id == id } ?: return
        val nextRule = r.copy(active = !r.active)
        val next = _state.value.recurring.map { if (it.id == id) nextRule else it }
        update { it.copy(recurring = next) }
        viewModelScope.launch { repo.saveRecurring(next) }
        if (nextRule.active) {
            val materialized = runRecurring(_state.value, next)
            if (materialized != null) {
                update { materialized }
                viewModelScope.launch { persistSliceChanges(materialized) }
            }
        }
    }

    fun nextRun(r: Rule): String {
        if (!r.active) return "Paused"
        val from = r.last?.let { addDays(it, 1) } ?: r.start.ifEmpty { _state.value.today }
        val diff = dayDiff(_state.value.today, from)
        return when {
            diff <= 0 -> "Due today"
            diff == 1L -> "Tomorrow"
            else -> "in ${diff}d"
        }
    }

    /* ─── Setup ─── */

    fun saveSetup(budgetStr: String, daysStr: String, startDate: String, currency: String, balanceStr: String) {
        val s = _state.value
        val budget = budgetStr.toDoubleOrNull()
        val days = daysStr.toIntOrNull()
        if (budget == null || budget < 0 || days == null || days <= 0) {
            showToast("Enter a valid amount and period.", "error"); return
        }
        var bal: Double? = null
        if (s.balancesOn) {
            bal = if (balanceStr.isBlank()) null else balanceStr.toDoubleOrNull()
            if (bal != null && (bal.isNaN() || bal < 0)) {
                showToast("Enter a valid balance.", "error"); return
            }
        }
        val settings = Settings(budget, days, startDate.ifEmpty { firstOfMonthKey() })
        var prefs = s.prefs
        if (s.settings == null) prefs = prefs.copy(currency = currency)
        var balance = s.balance
        if (s.balancesOn) {
            if (bal != null) balance = balance.copy(start = bal)
            else if (s.settings == null) balance = Balance(0.0)
        }
        update { it.copy(settings = settings, prefs = prefs, balance = balance) }
        viewModelScope.launch {
            repo.saveSettings(settings); repo.savePrefs(prefs); repo.saveBalance(balance)
        }
        showToast(if (s.settings != null) "Budget updated." else "Budget saved. Start tracking!", "success")
    }

    /* ─── Move money & balance ─── */

    fun submitMoney(mode: String, amountStr: String, note: String) {
        when (mode) {
            "budget" -> addTopUp(amountStr, note)
            "return" -> returnToBalance(amountStr, note)
            "withdraw" -> withdrawFromBalance(amountStr)
            else -> addToBalance(amountStr)
        }
    }

    private fun addTopUp(amountStr: String, note: String) {
        val s = _state.value
        val v = amountStr.toDoubleOrNull() ?: return
        if (v <= 0) {
            showToast(if (s.balancesOn) "Enter a valid amount." else "Enter a valid top-up amount.", "error"); return
        }
        if (s.balancesOn && v > s.bankBalance) {
            showToast("Not enough balance — move at most ${fmt(s.bankBalance, s.cur)}.", "error"); return
        }
        val entry = TopUp(uid(), v, s.today, note.trim())
        val next = s.topUps + entry
        update { it.copy(topUps = next) }
        viewModelScope.launch { repo.saveTopUps(next) }
        showToast(
            if (s.balancesOn) "Moved ${fmt(v, s.cur)} from your balance to this month's budget."
            else "Topped up ${fmt(v, s.cur)} — added to your monthly budget.",
            "success",
        )
    }

    private fun addToBalance(amountStr: String) {
        val s = _state.value
        val v = amountStr.toDoubleOrNull() ?: return
        if (v <= 0) {
            showToast("Enter a valid amount.", "error"); return
        }
        val next = s.balance.copy(start = s.balance.start + v)
        update { it.copy(balance = next) }
        viewModelScope.launch { repo.saveBalance(next) }
        showToast("Added ${fmt(v, s.cur)} to your balance.", "success")
    }

    private fun returnToBalance(amountStr: String, note: String) {
        val s = _state.value
        val v = amountStr.toDoubleOrNull() ?: return
        if (v <= 0) {
            showToast("Enter a valid amount.", "error"); return
        }
        if (s.topUpTotal <= 0) {
            showToast(
                "Nothing to return — you haven't moved money to the budget. Please move some first.",
                "error"
            ); return
        }
        if (v > s.topUpTotal) {
            showToast(
                "Can't return more than the ${fmt(s.topUpTotal, s.cur)} you moved to the budget.",
                "error"
            ); return
        }
        val entry = TopUp(uid(), -v, s.today, note.trim())
        val next = s.topUps + entry
        update { it.copy(topUps = next) }
        viewModelScope.launch { repo.saveTopUps(next) }
        showToast("Returned ${fmt(v, s.cur)} from your budget to your balance.", "success")
    }

    private fun withdrawFromBalance(amountStr: String) {
        val s = _state.value
        val v = amountStr.toDoubleOrNull() ?: return
        if (v <= 0) {
            showToast("Enter a valid amount.", "error"); return
        }
        if (s.bankBalance <= 0) {
            showToast("Nothing to withdraw — your balance is empty. Move money into it first.", "error"); return
        }
        if (v > s.bankBalance) {
            showToast("Not enough balance — withdraw at most ${fmt(s.bankBalance, s.cur)}.", "error"); return
        }
        val next = s.balance.copy(start = s.balance.start - v)
        update { it.copy(balance = next) }
        viewModelScope.launch { repo.saveBalance(next) }
        showToast("Withdrew ${fmt(v, s.cur)} from your balance.", "success")
    }

    fun removeTopUp(id: String) {
        val s = _state.value
        val removed = s.topUps.find { it.id == id }
        val next = s.topUps.filter { it.id != id }
        update { it.copy(topUps = next) }
        viewModelScope.launch { repo.saveTopUps(next) }
        if (removed != null) showToast("Transfer removed.", "info", ToastAction("Undo") {
            val restored = s.topUps.filter { it.id != id } + removed
            update { it.copy(topUps = restored) }
            viewModelScope.launch { repo.saveTopUps(restored) }
            showToast("Restored.", "success")
        })
    }

    /* ─── Piggy bank operations ─── */

    fun selectPiggy(id: String) {
        update { it.copy(activePiggyId = id) }
    }

    fun addPiggy(name: String, target: Double = 0.0, texture: String? = null, soundId: String = "coin") {
        val s = _state.value
        val newId = uid()
        val newPiggy = Piggy(
            id = newId,
            name = name.ifBlank { "Piggy #${s.piggies.size + 1}" },
            target = target,
            saved = 0.0,
            texture = texture,
            soundId = soundId,
            soundCustom = null
        )
        val next = s.piggies + newPiggy
        update { it.copy(piggies = next, activePiggyId = newId) }
        viewModelScope.launch { repo.savePiggies(next) }
        showToast("Created ${newPiggy.name}.", "success")
    }

    fun renamePiggy(id: String, newName: String) {
        val s = _state.value
        val next = s.piggies.map { if (it.id == id) it.copy(name = newName.ifBlank { it.name }) else it }
        update { it.copy(piggies = next) }
        viewModelScope.launch { repo.savePiggies(next) }
        showToast("Piggy bank renamed.", "success")
    }

    fun savePiggyTarget(id: String, amountStr: String) {
        val s = _state.value
        val v = amountStr.toDoubleOrNull()
        if (v == null || v.isNaN() || v < 0) {
            showToast("Enter a valid goal amount.", "error"); return
        }
        val next = s.piggies.map { if (it.id == id) it.copy(target = v) else it }
        update { it.copy(piggies = next) }
        viewModelScope.launch { repo.savePiggies(next) }
        showToast(if (v > 0) "Savings goal set to ${fmt(v, s.cur)}." else "Savings goal cleared.", "success")
    }

    fun depositPiggy(id: String, amountStr: String) {
        val s = _state.value
        val v = amountStr.toDoubleOrNull() ?: return
        if (v <= 0) {
            showToast("Enter a valid amount.", "error"); return
        }
        if (v > s.bankBalance) {
            showToast("Not enough balance — add at most ${fmt(s.bankBalance, s.cur)}.", "error"); return
        }
        val targetPiggy = s.piggies.find { it.id == id } ?: s.activePiggy
        val prev = targetPiggy.saved
        val saved = prev + v
        val balance = s.balance.copy(start = s.balance.start - v)
        val next = s.piggies.map { if (it.id == targetPiggy.id) it.copy(saved = saved) else it }
        update { it.copy(balance = balance, piggies = next) }
        viewModelScope.launch { repo.saveBalance(balance); repo.savePiggies(next) }
        showToast("Added ${fmt(v, s.cur)} to ${targetPiggy.name}.", "success")
        if (targetPiggy.target > 0 && prev < targetPiggy.target && saved >= targetPiggy.target) {
            showToast("Goal complete for ${targetPiggy.name}! 🎉", "success")
        }
    }

    fun breakPiggy(id: String) {
        val s = _state.value
        val targetPiggy = s.piggies.find { it.id == id } ?: s.activePiggy
        if (targetPiggy.saved <= 0) {
            showToast("${targetPiggy.name} is empty.", "error"); return
        }
        confirm = ConfirmReq(
            title = "Break ${targetPiggy.name}?",
            msg = "All ${fmt(targetPiggy.saved, s.cur)} moves back to your balance.",
            onConfirm = {
                val balance = s.balance.copy(start = s.balance.start + targetPiggy.saved)
                val next = s.piggies.map { if (it.id == targetPiggy.id) it.copy(saved = 0.0) else it }
                update { it.copy(balance = balance, piggies = next) }
                viewModelScope.launch { repo.saveBalance(balance); repo.savePiggies(next) }
                dismissConfirm()
                showToast(
                    "Broke ${targetPiggy.name} — ${fmt(targetPiggy.saved, s.cur)} back to your balance.",
                    "success"
                )
            },
            onCancel = { dismissConfirm() },
        )
    }

    fun deletePiggy(id: String) {
        val s = _state.value
        if (s.piggies.size <= 1) {
            showToast("Cannot delete the only piggy bank.", "error"); return
        }
        val targetPiggy = s.piggies.find { it.id == id } ?: return
        confirm = ConfirmReq(
            title = "Delete ${targetPiggy.name}?",
            msg = if (targetPiggy.saved > 0) "All ${
                fmt(
                    targetPiggy.saved,
                    s.cur
                )
            } saved in this piggy bank will move back to your balance." else "Are you sure you want to delete ${targetPiggy.name}?",
            onConfirm = {
                val balance =
                    if (targetPiggy.saved > 0) s.balance.copy(start = s.balance.start + targetPiggy.saved) else s.balance
                val next = s.piggies.filter { it.id != id }
                val newActiveId = if (s.activePiggyId == id) next.first().id else s.activePiggyId
                update { it.copy(balance = balance, piggies = next, activePiggyId = newActiveId) }
                viewModelScope.launch { repo.saveBalance(balance); repo.savePiggies(next) }
                dismissConfirm()
                showToast("Deleted ${targetPiggy.name}.", "success")
            },
            onCancel = { dismissConfirm() },
        )
    }

    fun updatePiggyTexture(id: String, texture: String?) {
        val s = _state.value
        val next = s.piggies.map { if (it.id == id) it.copy(texture = texture) else it }
        update { it.copy(piggies = next) }
        viewModelScope.launch { repo.savePiggies(next) }
    }

    fun updatePiggySound(id: String, soundId: String) {
        val s = _state.value
        val next = s.piggies.map { if (it.id == id) it.copy(soundId = soundId) else it }
        update { it.copy(piggies = next) }
        viewModelScope.launch { repo.savePiggies(next) }
    }

    /* ─── CRUD ─── */

    fun addExpense() {
        val s = _state.value
        val v = amount.toDoubleOrNull() ?: return
        if (v <= 0) return
        val cat = selCats.firstOrNull() ?: "food"
        val entry = Expense(uid(), entryDate.ifEmpty { s.today }, v, listOf(cat), cat, note.trim())
        val next = s.expenses + entry
        update { it.copy(expenses = next) }
        amount = ""; note = ""; entryDate = todayKey()
        val catName = s.cats.find { it.id == cat }?.label ?: cat
        showToast("Logged ${fmt(v, s.cur)} in $catName.", "success")
        viewModelScope.launch { repo.saveExpenses(next) }
    }

    fun startEdit(e: HistoryEntry) {
        editingId = e.id
        amount = if (e.amount % 1.0 == 0.0) e.amount.toLong().toString() else e.amount.toString()
        note = e.note
        selCats = entryCats(e).take(1).ifEmpty { listOf("food") } // single-select
        entryDate = e.date
    }

    fun updateExpense() {
        val s = _state.value
        val id = editingId ?: return
        val v = amount.toDoubleOrNull() ?: return
        if (v <= 0) return
        val cat = selCats.firstOrNull() ?: "food"
        val next = s.expenses.map {
            if (it.id == id) it.copy(
                amount = v,
                categories = listOf(cat),
                category = cat,
                note = note.trim(),
                date = entryDate.ifEmpty { s.today })
            else it
        }
        update { it.copy(expenses = next) }
        cancelEdit()
        showToast("Spend updated.", "success")
        viewModelScope.launch { repo.saveExpenses(next) }
    }

    fun cancelEdit() {
        editingId = null; amount = ""; note = ""; entryDate = todayKey()
    }

    fun removeExpense(id: String) {
        val s = _state.value
        val removed = s.expenses.find { it.id == id }
        val next = s.expenses.filter { it.id != id }
        if (editingId == id) cancelEdit()
        update { it.copy(expenses = next) }
        viewModelScope.launch { repo.saveExpenses(next) }
        if (removed != null) showToast("Spend removed.", "info", ToastAction("Undo") {
            val restored = (s.expenses.filter { it.id != id } + removed).distinctBy { it.id }
            update { it.copy(expenses = restored) }
            viewModelScope.launch { repo.saveExpenses(restored) }
            showToast("Restored.", "success")
        })
    }

    fun duplicateExpense(id: String) {
        val s = _state.value
        val e = s.expenses.find { it.id == id } ?: return
        val entry = e.copy(id = uid(), date = s.today)
        val next = s.expenses + entry
        update { it.copy(expenses = next) }
        viewModelScope.launch { repo.saveExpenses(next) }
        showToast("Duplicated ${fmt(e.amount, s.cur)}.", "success")
    }

    fun saveCatBudgets(next: Map<String, Double>) {
        update { it.copy(catBudgets = next) }
        viewModelScope.launch { repo.saveCatBudgets(next) }
    }

    /* ─── Categories ─── */

    fun addCategory(name: String, glyph: String) {
        val s = _state.value
        val n = name.trim()
        if (n.isEmpty()) {
            showToast("Enter a category name.", "error"); return
        }
        val id = n.lowercase().replace(Regex("\\s+"), "-").replace(Regex("[^a-z0-9-]"), "")
        if (id.isEmpty() || s.cats.any { it.id == id }) {
            showToast("Invalid or duplicate name.", "error"); return
        }
        val next = s.categories + Category(id, n, glyph.ifEmpty { "★" })
        val theme = s.theme.copy(catColors = s.theme.catColors + (id to "#7c8896"))
        update { it.copy(categories = next, theme = theme) }
        viewModelScope.launch { repo.saveCategories(next); repo.saveTheme(theme) }
        showToast("Category \"$n\" added.", "success")
    }

    fun removeCategory(id: String) {
        val s = _state.value
        if (s.expenses.any { expCats(it).contains(id) }) {
            showToast("Can't delete — expenses use this category.", "error"); return
        }
        val next = s.categories.filter { it.id != id }
        update { it.copy(categories = next) }
        viewModelScope.launch { repo.saveCategories(next) }
        if (selCats.contains(id)) {
            selCats = (selCats.filter { it != id }).ifEmpty { listOf(next.firstOrNull()?.id ?: "food") }
        }
        showToast("Category removed.")
    }

    /* ─── Theme ─── */

    fun applyPreset(key: String) {
        val preset = PRESETS[key] ?: return
        update { it.copy(theme = preset, savedTheme = null) }
        viewModelScope.launch { repo.saveTheme(preset); repo.saveSavedTheme(null) }
        showToast("${key.replaceFirstChar { it.uppercase() }} theme applied.", "success")
    }

    fun updateColor(k: String, v: String) {
        update { it.copy(theme = setThemeField(it.theme, k, v), savedTheme = null) }
        viewModelScope.launch { repo.saveTheme(_state.value.theme); repo.saveSavedTheme(null) }
    }

    fun updateCatColor(catId: String, v: String) {
        update { it.copy(theme = it.theme.copy(catColors = it.theme.catColors + (catId to v)), savedTheme = null) }
        viewModelScope.launch { repo.saveTheme(_state.value.theme); repo.saveSavedTheme(null) }
    }

    fun resetTheme() {
        update { it.copy(theme = DEFAULT_THEME, savedTheme = null) }
        viewModelScope.launch { repo.saveTheme(DEFAULT_THEME); repo.saveSavedTheme(null) }
        showToast("Theme reset.", "success")
    }

    private fun setThemeField(t: AppTheme, k: String, v: String): AppTheme = when (k) {
        "bg" -> t.copy(bg = v); "surface" -> t.copy(surface = v); "surface2" -> t.copy(surface2 = v)
        "text" -> t.copy(text = v); "textDim" -> t.copy(textDim = v); "textMuted" -> t.copy(textMuted = v)
        "border" -> t.copy(border = v); "borderStrong" -> t.copy(borderStrong = v)
        "accent" -> t.copy(accent = v); "accentFg" -> t.copy(accentFg = v)
        "negative" -> t.copy(negative = v); "warning" -> t.copy(warning = v); "positive" -> t.copy(positive = v)
        else -> t
    }

    fun isDark(theme: AppTheme): Boolean {
        val hex = theme.bg.removePrefix("#")
        if (hex.length != 6) return true
        val r = hex.substring(0, 2).toIntOrNull(16) ?: 0
        val g = hex.substring(2, 4).toIntOrNull(16) ?: 0
        val b = hex.substring(4, 6).toIntOrNull(16) ?: 0
        return (r * 0.299 + g * 0.587 + b * 0.114) < 128
    }

    fun toggleLightDark() {
        val s = _state.value
        if (s.savedTheme != null) {
            val t = s.savedTheme!!
            update { it.copy(theme = t, savedTheme = null) }
            viewModelScope.launch { repo.saveTheme(t); repo.saveSavedTheme(null) }
            showToast("Restored previous theme.", "success")
            return
        }
        val target = if (isDark(s.theme)) PRESETS["paper"]!! else PRESETS["mono"]!!
        update { it.copy(theme = target, savedTheme = s.theme) }
        viewModelScope.launch { repo.saveTheme(target); repo.saveSavedTheme(s.theme) }
        showToast(if (isDark(s.theme)) "Paper theme applied." else "Mono theme applied.", "success")
    }

    /* ─── Prefs ─── */

    fun updatePrefs(f: (Prefs) -> Prefs) {
        val next = f(_state.value.prefs)
        update { it.copy(prefs = next) }
        viewModelScope.launch { repo.savePrefs(next) }
    }

    /* ─── Reorderable cards ─── */

    private fun cardOrderBase(s: LedgerState): List<String> =
        if (s.prefs.cardOrder.size == com.ledger.app.data.defaultCardOrder.size &&
            s.prefs.cardOrder.toSet() == com.ledger.app.data.defaultCardOrder.toSet()
        ) s.prefs.cardOrder else com.ledger.app.data.defaultCardOrder

    private fun cardOrderOf(s: LedgerState): List<String> =
        com.ledger.app.data.dashboardCardOrder(s.prefs.cardOrder, s.balancesOn)

    fun moveCard(id: String, dir: Int) {
        val s = _state.value
        val cur = cardOrderBase(s)
        val idx = cur.indexOf(id)
        val swapWith = idx + dir
        if (swapWith < 0 || swapWith >= cur.size) return
        val next = cur.toMutableList()
        val tmp = next[idx]; next[idx] = next[swapWith]; next[swapWith] = tmp
        updatePrefs { it.copy(cardOrder = next) }
    }

    fun resetCardOrder() {
        updatePrefs { it.copy(cardOrder = com.ledger.app.data.defaultCardOrder) }
        showToast("Card order reset.", "success")
    }

    /* ─── Wallpaper & Notifications ─── */

    fun setWallpaperFromUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val mime = context.contentResolver.getType(uri)
                if (mime?.startsWith("image/") != true) {
                    showToast("Please choose an image file.", "error")
                    return@launch
                }
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val ext = "jpg"
                val file = java.io.File(context.filesDir, "wallpaper_${System.currentTimeMillis()}.$ext")
                context.filesDir.listFiles { _, name -> name.startsWith("wallpaper_") }?.forEach { it.delete() }

                file.outputStream().use { out ->
                    inputStream.copyTo(out)
                }
                val path = file.absolutePath
                updatePrefs { it.copy(wallpaper = path) }
                showToast("Wallpaper set.", "success")
            } catch (e: Exception) {
                showToast("Couldn't set wallpaper.", "error")
            }
        }
    }

    fun clearWallpaper(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            context.filesDir.listFiles { _, name -> name.startsWith("wallpaper_") }?.forEach { it.delete() }
            updatePrefs { it.copy(wallpaper = null) }
            showToast("Wallpaper removed.", "success")
        }
    }

    fun updateWallpaperDim(dim: Int) {
        updatePrefs { it.copy(wallpaperDim = dim.coerceIn(0, 90)) }
    }

    fun updateWallBlur(blur: Int) {
        updatePrefs { it.copy(wallBlur = blur.coerceIn(0, 20)) }
    }

    fun toggleNotifications(enabled: Boolean, context: android.content.Context) {
        updatePrefs { it.copy(notificationsEnabled = enabled) }
        if (enabled) {
            NotificationHelper.scheduleDailyReminder(
                context,
                _state.value.prefs.reminderHour,
                _state.value.prefs.reminderMinute
            )
            showToast("Daily reminders turned on.", "success")
        } else {
            NotificationHelper.cancelDailyReminder(context)
            showToast("Reminders turned off.", "info")
        }
    }

    fun setReminderTime(hour: Int, minute: Int, context: android.content.Context) {
        updatePrefs { it.copy(reminderHour = hour, reminderMinute = minute) }
        if (_state.value.prefs.notificationsEnabled) {
            NotificationHelper.scheduleDailyReminder(context, hour, minute)
            showToast("Reminder time updated.", "success")
        }
    }

    fun toggleBudgetAlerts(enabled: Boolean) {
        updatePrefs { it.copy(budgetAlertsEnabled = enabled) }
        showToast(if (enabled) "Budget alert warnings enabled." else "Budget alerts disabled.", "info")
    }

    fun testReminderNotification(context: android.content.Context) {
        NotificationHelper.showDailyReminder(context)
        showToast("Test notification sent.", "success")
    }

    fun toggleGlass(enabled: Boolean) {
        updatePrefs { it.copy(glassEnabled = enabled) }
        showToast(if (enabled) "Liquid glass enabled." else "Liquid glass disabled.", "info")
    }

    fun updateGlassBlur(blur: Int) {
        updatePrefs { it.copy(glassBlur = blur.coerceIn(0, 24)) }
    }

    fun updateGlassOpacity(opacity: Int) {
        updatePrefs { it.copy(glassOpacity = opacity.coerceIn(20, 100)) }
    }

    fun updateGlassRefraction(refraction: Int) {
        updatePrefs { it.copy(glassRefraction = refraction.coerceIn(0, 40)) }
    }

    fun updateGlassRefractionHeight(height: Int) {
        updatePrefs { it.copy(glassRefractionHeight = height.coerceIn(0, 40)) }
    }

    fun updateGlassChromaticAberration(amount: Int) {
        updatePrefs { it.copy(glassChromaticAmount = amount.coerceIn(0, 100)) }
    }

    /* ─── Clear all ─── */

    fun clearAll() {
        val s = _state.value
        confirm = ConfirmReq(
            title = "Delete everything?",
            msg = "This removes all logged expenses, budget settings, and preferences. Download a backup first if you want to keep your data.",
            onConfirm = {
                val freshPiggy = Piggy()
                update {
                    it.copy(
                        expenses = emptyList(),
                        settings = null,
                        categories = defaultCategories(),
                        catBudgets = emptyMap(),
                        topUps = emptyList(),
                        balance = Balance(0.0),
                        piggy = freshPiggy,
                        piggies = listOf(freshPiggy),
                        activePiggyId = freshPiggy.id,
                        recurring = emptyList()
                    )
                }
                viewModelScope.launch {
                    repo.saveExpenses(emptyList()); repo.saveSettings(null); repo.saveCategories(defaultCategories())
                    repo.saveCatBudgets(emptyMap()); repo.saveTopUps(emptyList()); repo.saveBalance(Balance(0.0))
                    repo.savePiggies(listOf(freshPiggy)); repo.saveRecurring(emptyList())
                }
                dismissConfirm()
                showToast("All data cleared.", "success")
            },
            onCancel = { dismissConfirm() },
        )
    }

    /* ─── Backup (JSON + CSV, same formats as the web app) ─── */

    fun exportJson(): String = buildJsonObject {
        put("type", "ledger-backup")
        put("version", 6)
        put("exportedAt", java.time.OffsetDateTime.now().toString())
        val s = _state.value
        put("settings", s.settings?.let { json.encodeToJsonElement(it) } ?: JsonNull)
        put("expenses", json.encodeToJsonElement(s.expenses))
        put("categories", json.encodeToJsonElement(s.categories))
        put("catBudgets", json.encodeToJsonElement(s.catBudgets))
        put("topUps", json.encodeToJsonElement(s.topUps))
        put("balance", json.encodeToJsonElement(s.balance))
        put("piggy", json.encodeToJsonElement(s.piggies.firstOrNull() ?: Piggy()))
        put("piggies", json.encodeToJsonElement(s.piggies))
        put("recurring", json.encodeToJsonElement(s.recurring))
        put("prefs", json.encodeToJsonElement(s.prefs))
    }.toString()

    fun exportCsv(): String {
        val s = _state.value
        val rows = mutableListOf<List<String>>(listOf("Date", "Amount", "Currency", "Category", "Note"))
        val sorted = s.expenses.sortedWith(compareBy({ it.date }, { it.id }))
        for (e in sorted) {
            val catL = expCats(e).map { id -> s.cats.find { it.id == id }?.label ?: id }.joinToString(" + ")
            rows.add(listOf(e.date, "%.2f".format(e.amount), s.cur, catL, e.note.replace("\"", "\"\"")))
        }
        return rows.joinToString("\n") { row -> row.joinToString(",") { cell -> "\"$cell\"" } }
    }

    /** Import a web/Android backup JSON. Returns an error message, or null on success. */
    fun importData(raw: String): String? {
        val obj = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
            ?: return "Not a valid ledger backup."
        if (obj["expenses"] !is JsonArray) return "Not a valid ledger backup."
        val s = _state.value

        // Older/corrupted exports occasionally mangle the category key (e.g. "category'");
        // repair it so the expense still imports with its category.
        val expensesArr = obj["expenses"] as JsonArray
        val normalizedExpenses = JsonArray(expensesArr.map { normalizeExpenseElement(it) })
        val newExpenses = runCatching { json.decodeFromJsonElement<List<Expense>>(normalizedExpenses) }
            .getOrElse { return "Couldn't read that file. (${it.message})" }
        val finalExpenses = com.ledger.app.data.normalizeExpenses(newExpenses)

        var settings = s.settings
        obj["settings"]?.let { el ->
            if (el !is JsonNull && el is JsonObject &&
                el["monthlyBudget"] is JsonPrimitive && el["periodDays"] is JsonPrimitive && el["startDate"] is JsonPrimitive
            ) {
                settings = runCatching { json.decodeFromJsonElement<Settings>(el) }.getOrNull() ?: s.settings
            }
        }

        var categories = s.categories
        obj["categories"]?.let { el ->
            if (el is JsonArray) categories =
                runCatching { json.decodeFromJsonElement<List<Category>>(el) }.getOrNull() ?: s.categories
        }

        var catBudgets = s.catBudgets
        obj["catBudgets"]?.let { el ->
            if (el is JsonObject) catBudgets =
                runCatching { json.decodeFromJsonElement<Map<String, Double>>(el) }.getOrNull() ?: s.catBudgets
        }

        var topUps = s.topUps
        obj["topUps"]?.let { el ->
            if (el is JsonArray) topUps =
                runCatching { json.decodeFromJsonElement<List<TopUp>>(el) }.getOrNull() ?: s.topUps
        }

        var balance = s.balance
        obj["balance"]?.let { el ->
            if (el is JsonObject && el["start"] is JsonPrimitive) balance =
                runCatching { json.decodeFromJsonElement<Balance>(el) }.getOrNull() ?: s.balance
        }

        var piggies = s.piggies
        obj["piggies"]?.let { el ->
            if (el is JsonArray) piggies =
                runCatching { json.decodeFromJsonElement<List<Piggy>>(el) }.getOrNull() ?: s.piggies
        } ?: obj["piggy"]?.let { el ->
            if (el is JsonObject && el["target"] is JsonPrimitive && el["saved"] is JsonPrimitive) {
                val single = runCatching { json.decodeFromJsonElement<Piggy>(el) }.getOrNull()
                if (single != null) piggies = listOf(single)
            }
        }
        val piggy = piggies.firstOrNull() ?: Piggy()

        var recurring = s.recurring
        obj["recurring"]?.let { el ->
            if (el is JsonArray) recurring =
                runCatching { json.decodeFromJsonElement<List<Rule>>(el) }.getOrNull() ?: s.recurring
        }

        /* Prefs merge — imported keys win, missing keys keep current values. */
        var prefs = s.prefs
        obj["prefs"]?.let { el ->
            if (el is JsonObject) {
                val p = runCatching { json.decodeFromJsonElement<Prefs>(el) }.getOrNull()
                if (p != null) {
                    runCatching {
                        prefs = prefs.copy(
                            currency = el["currency"]?.jsonPrimitive?.contentOrNull ?: prefs.currency,
                            compact = el["compact"]?.jsonPrimitive?.booleanOrNull ?: prefs.compact,
                            pieThickness = el["pieThickness"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
                                ?: prefs.pieThickness,
                            pieGap = el["pieGap"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: prefs.pieGap,
                            groupHistory = el["groupHistory"]?.jsonPrimitive?.booleanOrNull ?: prefs.groupHistory,
                            trendStyle = el["trendStyle"]?.jsonPrimitive?.contentOrNull ?: prefs.trendStyle,
                            heatColors = p.heatColors,
                            font = el["font"]?.jsonPrimitive?.contentOrNull ?: prefs.font,
                            cardOrder = p.cardOrder,
                            balancesEnabled = el["balancesEnabled"]?.jsonPrimitive?.booleanOrNull
                                ?: prefs.balancesEnabled,
                            heroMode = el["heroMode"]?.jsonPrimitive?.contentOrNull ?: prefs.heroMode,
                        )
                    }
                }
            }
        }

        update {
            it.copy(
                settings = settings, expenses = finalExpenses, categories = categories,
                catBudgets = catBudgets, topUps = topUps, balance = balance,
                piggy = piggy, piggies = piggies, activePiggyId = piggy.id,
                recurring = recurring, prefs = prefs,
            )
        }
        viewModelScope.launch {
            repo.saveSettings(settings); repo.saveExpenses(finalExpenses); repo.saveCategories(categories)
            repo.saveCatBudgets(catBudgets); repo.saveTopUps(topUps); repo.saveBalance(balance)
            repo.savePiggies(piggies); repo.saveRecurring(recurring); repo.savePrefs(prefs)
        }
        showToast("Restored ${finalExpenses.size} entries.", "success")
        return null
    }

    /* ─── Cloud sync (Firebase + Google Auth) ─── */

    private fun initFirebaseSync() {
        if (!FirebaseConfig.isConfigured) return
        val auth = FirebaseManager.getAuth(repo.appContext) ?: return
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val authUser = if (user != null) {
                AuthUser(
                    uid = user.uid,
                    name = user.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl?.toString()
                )
            } else null

            _state.value = _state.value.copy(
                authUser = authUser,
                syncError = if (authUser == null) false else _state.value.syncError,
                syncErrorMsg = if (authUser == null) "" else _state.value.syncErrorMsg
            )

            if (authUser != null) {
                attachFirestoreListener(authUser.uid)
            } else {
                detachFirestoreListener()
                syncedUid = null
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun attachFirestoreListener(uid: String) {
        detachFirestoreListener()
        val db = FirebaseManager.getFirestore(repo.appContext) ?: return
        try {
            snapshotListener = db.collection("ledger").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("LedgerViewModel", "Sync listener error", error)
                        _state.value = _state.value.copy(
                            syncError = true,
                            syncErrorMsg = error.localizedMessage ?: "Sync error"
                        )
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.data ?: emptyMap<String, Any?>()
                        viewModelScope.launch {
                            val last = repo.getLastSync(uid)
                            val local = _state.value
                            val localHasData =
                                local.settings != null || local.expenses.isNotEmpty() || local.topUps.isNotEmpty()
                            val rawExpenses = data["expenses"] as? List<*>
                            val rawTopUps = data["topUps"] as? List<*>
                            val cloudLooksDefault =
                                rawExpenses.isNullOrEmpty() && rawTopUps.isNullOrEmpty() && data["settings"] == null

                            val at = snapshot.get("updatedAt")
                            val remoteAt = when (at) {
                                is com.google.firebase.Timestamp -> at.toDate().time
                                is Number -> at.toLong()
                                else -> 0L
                            }

                            // If the user is on the intro screen (no local settings yet) and the
                            // cloud doc has a real save, load it immediately so the intro auto-closes.
                            val cloudHasSettings = data["settings"] != null
                            val localSettingsNull = _state.value.settings == null
                            if (cloudHasSettings && localSettingsNull) {
                                skipNextPush = true
                                applyRemote(data)
                                repo.setLastSync(uid, remoteAt)
                                _state.value = _state.value.copy(
                                    lastSyncedAt = remoteAt,
                                    syncError = false,
                                    syncErrorMsg = ""
                                )
                                showToast("Synced from cloud.", "success")
                            } else if (remoteAt > last) {
                                if (cloudLooksDefault && localHasData) {
                                    pushSync(uid)
                                } else {
                                    skipNextPush = true
                                    applyRemote(data)
                                    repo.setLastSync(uid, remoteAt)
                                    _state.value = _state.value.copy(
                                        lastSyncedAt = remoteAt,
                                        syncError = false,
                                        syncErrorMsg = ""
                                    )
                                    if (last == 0L) {
                                        showToast("Synced from cloud.", "success")
                                    }
                                }
                            } else if (remoteAt == 0L) {
                                if (cloudLooksDefault || localHasData) {
                                    safePushSync(uid)
                                } else {
                                    skipNextPush = true
                                    applyRemote(data)
                                }
                            } else if (remoteAt < last) {
                                safePushSync(uid)
                            }
                        }
                    } else {
                        safePushSync(uid)
                    }
                    syncedUid = uid
                }
        } catch (e: Exception) {
            Log.w("LedgerViewModel", "Sync subscribe failed", e)
            _state.value = _state.value.copy(
                syncError = true,
                syncErrorMsg = e.localizedMessage ?: "Sync subscribe failed"
            )
        }
    }

    private fun detachFirestoreListener() {
        snapshotListener?.remove()
        snapshotListener = null
    }

    private suspend fun applyRemote(data: Map<String, Any?>) {
        try {
            val parsed = FirebaseSyncSerializer.parseRemote(data, _state.value.prefs, _state.value.theme)
            var s = _state.value

            parsed.expenses?.let {
                val norm = com.ledger.app.data.normalizeExpenses(it)
                s = s.copy(expenses = norm)
                repo.saveExpenses(norm)
            }
            parsed.topUps?.let {
                s = s.copy(topUps = it)
                repo.saveTopUps(it)
            }
            parsed.balance?.let {
                s = s.copy(balance = it)
                repo.saveBalance(it)
            }
            parsed.piggies?.let {
                val activeId = if (it.any { p -> p.id == s.activePiggyId }) s.activePiggyId else it.first().id
                s = s.copy(piggies = it, piggy = it.first(), activePiggyId = activeId)
                repo.savePiggies(it)
            }
            parsed.recurring?.let {
                s = s.copy(recurring = it)
                repo.saveRecurring(it)
            }
            parsed.settings?.let {
                s = s.copy(settings = it)
                repo.saveSettings(it)
            }
            parsed.categories?.let {
                s = s.copy(categories = it)
                repo.saveCategories(it)
            }
            parsed.catBudgets?.let {
                s = s.copy(catBudgets = it)
                repo.saveCatBudgets(it)
            }
            parsed.prefs?.let {
                s = s.copy(prefs = it)
                repo.savePrefs(it)
            }
            parsed.theme?.let {
                s = s.copy(theme = it)
                repo.saveTheme(it)
            }
            if (parsed.hasSavedThemeKey) {
                s = s.copy(savedTheme = parsed.savedTheme)
                repo.saveSavedTheme(parsed.savedTheme)
            }

            _state.value = computeDerived(s)
        } catch (e: Exception) {
            Log.w("LedgerViewModel", "applyRemote failed", e)
        }
    }

    fun pushSync(targetUid: String? = null) {
        val uid = targetUid ?: _state.value.authUser?.uid ?: return
        val db = FirebaseManager.getFirestore(repo.appContext) ?: return
        try {
            val s = _state.value
            val payload = FirebaseSyncSerializer.buildPayload(
                expenses = s.expenses,
                topUps = s.topUps,
                balance = s.balance,
                piggies = s.piggies,
                recurring = s.recurring,
                settings = s.settings,
                categories = s.categories,
                catBudgets = s.catBudgets,
                prefs = s.prefs,
                theme = s.theme,
                savedTheme = s.savedTheme
            )

            val jsonStr = payload.toString()
            if (jsonStr == lastPushedJson) return
            lastPushedJson = jsonStr

            val writePayload = payload.toMutableMap()
            writePayload["updatedAt"] = FieldValue.serverTimestamp()

            db.collection("ledger").document(uid).set(writePayload)
                .addOnSuccessListener {
                    val now = System.currentTimeMillis()
                    viewModelScope.launch {
                        repo.setLastSync(uid, now)
                    }
                    _state.value = _state.value.copy(
                        syncError = false,
                        syncErrorMsg = "",
                        lastSyncedAt = now
                    )
                }
                .addOnFailureListener { e ->
                    Log.w("LedgerViewModel", "Sync push failed", e)
                    _state.value = _state.value.copy(
                        syncError = true,
                        syncErrorMsg = e.localizedMessage ?: "Sync push failed"
                    )
                }
        } catch (e: Exception) {
            Log.w("LedgerViewModel", "Sync push failed", e)
            _state.value = _state.value.copy(
                syncError = true,
                syncErrorMsg = e.localizedMessage ?: "Sync push failed"
            )
        }
    }

    private fun safePushSync(uid: String) {
        val now = System.currentTimeMillis()
        if (now - lastSyncPushTime < 3000L) return
        lastSyncPushTime = now
        pushSync(uid)
    }

    private fun triggerDebouncedPush() {
        if (skipNextPush) {
            skipNextPush = false
            return
        }
        val user = _state.value.authUser ?: return
        if (syncedUid != user.uid) return
        pushJob?.cancel()
        pushJob = viewModelScope.launch {
            delay(1500L)
            pushSync(user.uid)
        }
    }

    fun manualSync() {
        val user = _state.value.authUser
        if (user == null) {
            showToast("Sign in with Google to sync.", "info")
            return
        }
        showToast("Syncing...", "info")
        lastPushedJson = null // Force push
        pushSync(user.uid)
    }

    fun signInGoogle(activity: Activity, launcher: ActivityResultLauncher<Intent>? = null) {
        if (!FirebaseConfig.isConfigured) {
            showToast("Sync isn't configured yet — see the Sync section in settings.", "error")
            return
        }
        if (!FirebaseManager.init(activity)) {
            showToast("Couldn't load Firebase — check your internet connection and reload.", "error")
            return
        }

        if (FirebaseConfig.webClientId.isNotBlank() && launcher != null) {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(FirebaseConfig.webClientId)
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(activity, gso)
                launcher.launch(client.signInIntent)
                return
            } catch (e: Exception) {
                Log.w("LedgerViewModel", "GoogleSignIn launch failed, falling back to OAuth provider", e)
            }
        }

        launchOAuthProvider(activity)
    }

    fun launchOAuthProvider(activity: Activity) {
        if (!FirebaseManager.init(activity)) {
            showToast("Couldn't load Firebase — check your internet connection and reload.", "error")
            return
        }

        val auth = FirebaseAuth.getInstance()
        val provider = OAuthProvider.newBuilder("google.com")
            .addCustomParameter("prompt", "select_account")
            .build()

        val pendingResultTask = auth.pendingAuthResult
        if (pendingResultTask != null) {
            pendingResultTask
                .addOnSuccessListener {
                    showToast("Signed in — syncing is on.", "success")
                }
                .addOnFailureListener { e ->
                    handleAuthError(e)
                }
        } else {
            auth.startActivityForSignInWithProvider(activity, provider)
                .addOnSuccessListener {
                    showToast("Signed in — syncing is on.", "success")
                }
                .addOnFailureListener { e ->
                    handleAuthError(e)
                }
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        val auth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                showToast("Signed in — syncing is on.", "success")
            }
            .addOnFailureListener { e ->
                handleAuthError(e)
            }
    }

    private fun handleAuthError(e: Exception) {
        val msg = e.localizedMessage ?: ""
        if (msg.contains("cancelled", ignoreCase = true) || msg.contains("canceled", ignoreCase = true) || msg.contains(
                "12501"
            ) || msg.contains("closed", ignoreCase = true)
        ) {
            showToast("Sign-in cancelled.", "info")
        } else if (msg.contains("INVALID APP ID", ignoreCase = true) || msg.contains(
                "INVALID_APP_ID",
                ignoreCase = true
            )
        ) {
            showToast(
                "Firebase: Add Android app (com.ledger.app) in Firebase Console, or set androidAppId in FirebaseConfig.kt.",
                "error"
            )
        } else {
            showToast(msg.ifEmpty { "Sign-in failed." }, "error")
        }
    }

    fun signOutGoogle() {
        try {
            FirebaseAuth.getInstance().signOut()
            showToast("Signed out. Changes stay on this device.", "info")
        } catch (e: Exception) {
            showToast("Sign out failed.", "error")
        }
    }

    override fun onCleared() {
        super.onCleared()
        detachFirestoreListener()
        authListener?.let {
            FirebaseAuth.getInstance().removeAuthStateListener(it)
        }
    }

    /* ─── Toast & confirm ─── */

    fun showToast(msg: String, type: String = "info", action: ToastAction? = null) {
        val id = System.currentTimeMillis()
        toast = ToastMsg(id, msg, type, action)
        viewModelScope.launch {
            delay(if (action != null) 6000 else 3500)
            if (toast?.id == id) toast = null
        }
    }

    fun dismissToast() {
        toast = null
    }

    fun runToastAction() {
        toast?.action?.run()
        toast = null
    }

    fun dismissConfirm() {
        confirm = null
    }
}

/**
 * Repairs expense elements from older/corrupted exports: if the `category` key
 * is missing but a mangled variant exists (e.g. "category'"), use it.
 */
private fun normalizeExpenseElement(el: JsonElement): JsonElement {
    if (el !is JsonObject) return el
    if (el.containsKey("category") || el.containsKey("categories")) return el
    val typo = el.entries.firstOrNull { (k, _) -> k.startsWith("category") } ?: return el
    return JsonObject(buildMap {
        putAll(el)
        put("category", typo.value)
    })
}
