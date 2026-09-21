package com.ledger.app.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
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
import com.ledger.app.data.BudgetAlertState
import com.ledger.app.data.Cat
import com.ledger.app.data.Category
import com.ledger.app.data.DayCell
import com.ledger.app.data.cleanTags
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
import com.ledger.app.data.Travel
import com.ledger.app.util.Fx
import com.ledger.app.data.defaultCategories
import com.ledger.app.data.expCats
import com.ledger.app.data.sanitizeSettings
import com.ledger.app.util.NotificationHelper
import com.ledger.app.util.advanceDate
import com.ledger.app.util.addDays
import com.ledger.app.util.bitmapToReceiptDataUrl
import com.ledger.app.util.dayDiff
import com.ledger.app.util.daysInMonth
import com.ledger.app.util.firstOfMonthKey
import com.ledger.app.util.fmt
import com.ledger.app.util.groupLabel
import com.ledger.app.util.monthLabel
import com.ledger.app.util.parseDate
import com.ledger.app.util.parseDateOrNull
import com.ledger.app.util.relativeDate
import com.ledger.app.util.todayKey
import com.ledger.app.util.uid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import java.time.LocalDate
import kotlin.math.abs
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
    /** Consecutive days with at least one logged expense. */
    val spendStreak: Int = 0,
    /** Longest such run ever — the streak card's best, so past runs aren't lost. */
    val bestStreak: Int = 0,
    val loggedToday: Boolean = false,
    val frequentEntries: List<FrequentEntry> = emptyList(),
    /* travel mode */
    val travel: Travel = Travel(),
    val travelActive: Boolean = false,
    /** Home-units-per-foreign-unit rate, or 0 when unset. */
    val travelRate: Double = 0.0,
    /** Entries logged since the trip started, newest first. */
    val travelList: List<Expense> = emptyList(),
    /** Note/token → category memory learned from the user's own expenses. */
    val catMemory: CatMemory = CatMemory(),
    val authUser: AuthUser? = null,
    val syncError: Boolean = false,
    val syncErrorMsg: String = "",
    val lastSyncedAt: Long = 0L,
    val isFirebaseConfigured: Boolean = FirebaseConfig.isConfigured,
    /** True when a stored blob couldn't be decoded — sync is paused to avoid overwriting it. */
    val dataCorrupt: Boolean = false,
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

/* ─── Monthly insights ─── */
data class InsightsData(
    val thisMonth: Double,
    val lastMonth: Double,
    val changePct: Double?,
    val biggestCategoryLabel: String?,
    val biggestCategoryDelta: Double,
    val avgPerDay: Double,
    val daysElapsed: Int,
    val projected: Double,
    val monthlyBudget: Double,
    val bestDay: Pair<String, Double>?,
    val worstDay: Pair<String, Double>?,
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
    val receipt: String? = null,
    val tags: List<String> = emptyList(),
    val currency: String? = null,
    val foreignAmount: Double? = null,
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
    var receipt by mutableStateOf<String?>(null)
    var tags by mutableStateOf<List<String>>(emptyList())

    /** "" | "loading" | "ok" | "ok:<date>" | "error" — the Travel tab's rate-sync state. */
    var rateStatus by mutableStateOf("")
        private set
    var editingId by mutableStateOf<String?>(null); private set

    /* ─── Tags ─── */
    fun addTag(v: String) {
        val tag = cleanTags(listOf(v)).firstOrNull() ?: return
        if (tag !in tags) tags = (tags + tag).take(8)
    }

    fun removeTag(tag: String) {
        tags = tags.filter { it != tag }
    }

    /* ─── Toast & confirm ─── */
    var toast by mutableStateOf<ToastMsg?>(null); private set
    var confirm by mutableStateOf<ConfirmReq?>(null)

    fun toggleSelCat(id: String) {
        // Single-select — a new choice replaces the previous one.
        selCats = listOf(id)
    }

    /** Note editor — auto-picks a category from your history/keywords as you type. */
    fun onNoteChange(v: String) {
        note = v
        if (editingId != null) return
        val st = _state.value
        val available = st.cats.map { it.id }.toSet()
        suggestCategory(v, available, st.catMemory)?.let { if (selCats.firstOrNull() != it) selCats = listOf(it) }
    }

    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var snapshotListener: ListenerRegistration? = null
    private var pushJob: Job? = null
    private var lastPushedJson: String? = null
    private var lastSyncPushTime: Long = 0L
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
        val rawSettings = d.settings
        val safeSettings = rawSettings?.let { sanitizeSettings(it) }
        if (safeSettings != null && safeSettings != rawSettings) {
            viewModelScope.launch { repo.saveSettings(safeSettings) }
        }
        var s = LedgerState(
            ready = true,
            theme = d.theme ?: DEFAULT_THEME,
            savedTheme = d.savedTheme,
            prefs = d.prefs ?: Prefs(),
            settings = safeSettings,
            expenses = normalizedExpenses,
            categories = d.categories ?: defaultCategories(),
            catBudgets = d.catBudgets ?: emptyMap(),
            topUps = d.topUps ?: emptyList(),
            balance = d.balance ?: Balance(),
            piggy = (d.piggies ?: d.piggy?.let { listOf(it) } ?: listOf(Piggy())).first(),
            piggies = d.piggies ?: d.piggy?.let { listOf(it) } ?: listOf(Piggy()),
            activePiggyId = (d.piggies ?: d.piggy?.let { listOf(it) } ?: listOf(Piggy())).first().id,
            recurring = d.recurring ?: emptyList(),
            dataCorrupt = d.corruptKeys.isNotEmpty(),
        )
        val materialized = runRecurring(s)
        if (materialized != null) s = materialized
        s = syncMonthlyPeriod(s)
        _state.value = computeDerived(s)
        if (materialized != null) persistSliceChanges(materialized)
        if (d.corruptKeys.isNotEmpty()) {
            // A corrupt blob decodes to null; treat that as "present but unreadable" rather
            // than "absent" so it isn't silently overwritten with defaults / cloud data.
            val which = d.corruptKeys.joinToString(", ") { it.removePrefix("ledger-") }
            showToast(t("toast.corruptData", "keys" to which), "error")
        } else {
            // Push any period rollover so the cloud doesn't keep the stale start date.
            triggerDebouncedPush()
        }

        if (s.prefs.notificationsEnabled) {
            NotificationHelper.scheduleDailyReminder(repo.appContext, s.prefs.reminderHour, s.prefs.reminderMinute)
        }
        checkBudgetAlerts()
    }

    /** Keep the budget period equal to the current calendar month: realign the start to
    the 1st when the month rolls over, and sync periodDays to the month length. */
    private suspend fun syncMonthlyPeriod(s: LedgerState): LedgerState {
        val settings = s.settings ?: return s
        val today = parseDate(todayKey())
        val real = daysInMonth(today)
        val firstToday = firstOfMonthKey(today)
        val firstStart = firstOfMonthKey(parseDateOrNull(settings.startDate) ?: today)
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
            val firstStart = firstOfMonthKey(parseDateOrNull(settings.startDate) ?: today)
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
        /* Keep the string table in step with the pref before anything renders. */
        Strings.setLang(s.prefs.lang)
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
            var running = 0.0
            val cells = (0 until s.settings.periodDays).map { i ->
                val date = addDays(s.settings.startDate, i)
                // A day is future if its date is after today, so a start date in the
                // future doesn't pull a not-yet-started day into spend/pace/streak maths.
                val isFuture = date > today
                val spent = spentByDay[date] ?: 0.0
                val delta = if (isFuture) 0.0 else dailyBudget - spent
                if (!isFuture) running += delta
                DayCell(date, spent, delta, isFuture, date == today)
            }
            Triple(cells, cells.count { !it.isFuture }, running)
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
        val heroLabel = if (heroMode == "balance") t("hero.labelBalance") else t("hero.labelAvailable")
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

        /* History-aware category memory for the log form's auto-pick. */
        val catMemory = buildCategoryMemory(s.expenses)

        val streak = if (s.settings == null) 0 else {
            var c = 0
            for (cell in dayCells.filter { !it.isFuture }.asReversed()) {
                if (cell.delta >= 0) c++ else break
            }
            c
        }

        /* Daily spend streak — consecutive days with at least one logged expense.
           `spendStreak` stays alive until a whole day is missed (today not being logged
           yet doesn't break it); `bestStreak` is the longest run ever. */
        val spendDays = s.expenses.map { it.date }.toHashSet()
        val loggedToday = today in spendDays
        /* A grace day forgives missed days inside a run — `grace` is the total number
           of skipped days tolerated in one streak, so a 2-day gap needs grace 2. */
        val grace = s.prefs.streakGrace.coerceAtLeast(0)
        var spendStreak = 0
        var cursor = if (loggedToday) today else addDays(today, -1)
        var misses = 0
        while (true) {
            if (cursor in spendDays) {
                spendStreak++
                cursor = addDays(cursor, -1)
            } else if (misses < grace) {
                misses++
                cursor = addDays(cursor, -1)
            } else break
        }
        var bestStreak = 0
        for (day in spendDays) {
            if (addDays(day, 1) in spendDays) continue // only measure from the end of a run
            var n = 0
            var cur = day
            var m = 0
            while (true) {
                if (cur in spendDays) {
                    n++
                    cur = addDays(cur, -1)
                } else if (m < grace) {
                    m++
                    cur = addDays(cur, -1)
                } else break
            }
            if (n > bestStreak) bestStreak = n
        }

        /* Travel mode — the trip page lists entries tagged "travel" since the trip
           started. `amount` is always the home-currency figure; `foreignAmount` is
           display-only, so budgets and statistics need no special casing. */
        val travel = s.prefs.travel
        val travelRate = if (travel.rate > 0) travel.rate else 0.0
        val travelActive = travel.active && travel.currency.isNotEmpty()
        val travelList =
            if (travelActive) s.expenses.filter {
                "travel" in it.tags && (travel.start.isEmpty() || it.date >= travel.start)
            }.sortedByDescending { it.date }
            else emptyList()

        return s.copy(
            cats = cats, cur = cur, balancesOn = balancesOn, heroMode = heroMode, today = today,
            topUpTotal = topUpTotal, effectiveMonthlyBudget = effectiveMonthlyBudget, dailyBudget = dailyBudget,
            dayCells = dayCells, elapsedDays = elapsedDays, runningBalance = runningBalance,
            todaySpent = todaySpent, todayRemaining = todayRemaining, bankedSoFar = bankedSoFar,
            bankBalance = bankBalance, todaySaved = todaySaved, heroLabel = heroLabel, heroValue = heroValue,
            piggyPct = piggyPct,
            periodSpent = periodSpent, budgetPctFull = budgetPctFull, avgDailySpend = avgDailySpend,
            daysOver = daysOver, projectedTotal = projectedTotal, projectedDelta = projectedDelta,
            streak = streak, spendStreak = spendStreak, bestStreak = bestStreak, loggedToday = loggedToday,
            travel = travel, travelActive = travelActive, travelRate = travelRate, travelList = travelList,
            frequentEntries = frequentEntries, catMemory = catMemory,
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
            "period" -> t("card.breakdown.rangeLabelPeriod"); "week" -> t("card.breakdown.rangeLabelWeek")
            "month" -> monthLabel(end, 0); "all" -> t("card.breakdown.rangeLabelAll")
            else -> t("card.breakdown.rangeLabelCustom")
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
        filterTags: List<String>,
        search: String,
        dateFrom: String,
        dateTo: String,
        sort: String,
        group: Boolean,
    ): HistoryData {
        var list: List<HistoryEntry> =
            s.expenses.map {
                HistoryEntry(
                    "expense",
                    it.id,
                    it.date,
                    it.amount,
                    it.note,
                    it.categories,
                    it.category,
                    it.receipt,
                    it.tags,
                    it.currency,
                    it.foreignAmount,
                )
            } +
                    s.topUps.map { HistoryEntry("topup", it.id, it.date, it.amount, it.note) }
        if (filterCats.isNotEmpty()) {
            list = list.filter { it.type == "expense" && entryCats(it).any { c -> filterCats.contains(c) } }
        }
        if (filterTags.isNotEmpty()) {
            list = list.filter { it.type == "expense" && it.tags.any { tg -> filterTags.contains(tg) } }
        }
        if (search.isNotBlank()) {
            val q = search.trim().lowercase()
            list = list.filter { e ->
                if (e.type == "topup") {
                    e.note.lowercase().contains(q) || "move to budget".contains(q) ||
                            "top up".contains(q) || "return to balance".contains(q)
                } else {
                    e.note.lowercase().contains(q) || entryCats(e).any { it.lowercase().contains(q) } ||
                            e.tags.any { it.lowercase().contains(q) }
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
                    (if (sort != "date-desc") 1 else 0) + filterCats.size + filterTags.size

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

    /**
     * Cursor (date key) the next occurrences are generated from. Tolerates blank or
     * malformed rule dates (from a backup / Firestore) by falling back to today, and
     * keeps monthly rules anchored on the original start day-of-month.
     */
    private fun ruleCursor(r: Rule, today: String): String {
        val last = r.last
        if (last != null) {
            val lastDate = parseDateOrNull(last) ?: return today
            return todayKey(advanceDate(lastDate, r.freq, parseDateOrNull(r.start) ?: lastDate))
        }
        return parseDateOrNull(r.start)?.let { todayKey(it) } ?: today
    }

    /** Materialize any due recurring entries. Returns the new state if changed. */
    private fun runRecurring(s: LedgerState, rules: List<Rule> = s.recurring): LedgerState? {
        val today = todayKey()
        val ex = s.expenses.toMutableList()
        val tu = s.topUps.toMutableList()
        var start = s.balance.start
        var changed = false
        val next = rules.map { r ->
            if (!r.active) return@map r
            val from = ruleCursor(r, today)
            val cursor = parseDateOrNull(from) ?: return@map r
            val end = parseDate(today)
            if (cursor.isAfter(end)) return@map r
            val step = parseDateOrNull(r.start) ?: cursor
            val occ = mutableListOf<String>()
            var cur = cursor
            while (!cur.isAfter(end)) {
                occ.add(todayKey(cur)); cur = advanceDate(cur, r.freq, step)
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
        showToast(t("toast.automatedEntriesAdded"), "success")
    }

    fun addAutomation(type: String, amountStr: String, catId: String, freq: String, start: String, note: String) {
        val v = amountStr.toDoubleOrNull() ?: return
        if (v <= 0) {
            showToast(t("toast.invalidAmount"), "error"); return
        }
        if (start.isEmpty()) {
            showToast(t("toast.pickStartDate"), "error"); return
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
        showToast(t("toast.automationAdded"), "success")
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
        if (removed != null) showToast(t("toast.automationRemoved"), "info", ToastAction(t("toast.actionUndo")) {
            val restored = _state.value.recurring + removed
            update { it.copy(recurring = restored) }
            viewModelScope.launch { repo.saveRecurring(restored) }
            showToast(t("toast.restored"), "success")
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
        if (!r.active) return t("card.auto.paused")
        val from = ruleCursor(r, _state.value.today)
        val diff = dayDiff(_state.value.today, from)
        return when {
            diff <= 0 -> t("card.auto.dueToday")
            diff == 1L -> t("card.auto.tomorrow")
            else -> t("card.auto.inDays", "n" to diff)
        }
    }

    /* ─── Setup ─── */

    fun saveSetup(budgetStr: String, daysStr: String, startDate: String, currency: String, balanceStr: String) {
        val s = _state.value
        val budget = budgetStr.toDoubleOrNull()
        val days = daysStr.toIntOrNull()
        if (budget == null || budget < 0 || days == null || days <= 0) {
            showToast(t("toast.invalidAmountPeriod"), "error"); return
        }
        var bal: Double? = null
        if (s.balancesOn) {
            bal = if (balanceStr.isBlank()) null else balanceStr.toDoubleOrNull()
            if (bal != null && (bal.isNaN() || bal < 0)) {
                showToast(t("toast.invalidBalance"), "error"); return
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
        showToast(if (s.settings != null) t("toast.budgetUpdated") else t("toast.budgetSaved"), "success")
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
            showToast(if (s.balancesOn) t("toast.invalidAmount") else t("toast.invalidTopUpAmount"), "error"); return
        }
        if (s.balancesOn && v > s.bankBalance) {
            showToast(t("toast.notEnoughForMove", "amount" to fmt(s.bankBalance, s.cur)), "error"); return
        }
        val entry = TopUp(uid(), v, s.today, note.trim())
        val next = s.topUps + entry
        update { it.copy(topUps = next) }
        viewModelScope.launch { repo.saveTopUps(next) }
        showToast(
            if (s.balancesOn) t("toast.movedToBudget", "amount" to fmt(v, s.cur))
            else t("toast.toppedUp", "amount" to fmt(v, s.cur)),
            "success",
        )
    }

    private fun addToBalance(amountStr: String) {
        val s = _state.value
        val v = amountStr.toDoubleOrNull() ?: return
        if (v <= 0) {
            showToast(t("toast.invalidAmount"), "error"); return
        }
        val next = s.balance.copy(start = s.balance.start + v)
        update { it.copy(balance = next) }
        viewModelScope.launch { repo.saveBalance(next) }
        showToast(t("toast.addedToBalance", "amount" to fmt(v, s.cur)), "success")
    }

    private fun returnToBalance(amountStr: String, note: String) {
        val s = _state.value
        val v = amountStr.toDoubleOrNull() ?: return
        if (v <= 0) {
            showToast(t("toast.invalidAmount"), "error"); return
        }
        if (s.topUpTotal <= 0) {
            showToast(
                t("toast.nothingToReturn"),
                "error"
            ); return
        }
        if (v > s.topUpTotal) {
            showToast(
                t("toast.returnTooMuch", "amount" to fmt(s.topUpTotal, s.cur)),
                "error"
            ); return
        }
        val entry = TopUp(uid(), -v, s.today, note.trim())
        val next = s.topUps + entry
        update { it.copy(topUps = next) }
        viewModelScope.launch { repo.saveTopUps(next) }
        showToast(t("toast.returnedToBalance", "amount" to fmt(v, s.cur)), "success")
    }

    private fun withdrawFromBalance(amountStr: String) {
        val s = _state.value
        val v = amountStr.toDoubleOrNull() ?: return
        if (v <= 0) {
            showToast(t("toast.invalidAmount"), "error"); return
        }
        if (s.bankBalance <= 0) {
            showToast(t("toast.nothingToWithdraw"), "error"); return
        }
        if (v > s.bankBalance) {
            showToast(t("toast.notEnoughForWithdraw", "amount" to fmt(s.bankBalance, s.cur)), "error"); return
        }
        val next = s.balance.copy(start = s.balance.start - v)
        update { it.copy(balance = next) }
        viewModelScope.launch { repo.saveBalance(next) }
        showToast(t("toast.withdrewFromBalance", "amount" to fmt(v, s.cur)), "success")
    }

    fun removeTopUp(id: String) {
        val s = _state.value
        val removed = s.topUps.find { it.id == id }
        val next = s.topUps.filter { it.id != id }
        update { it.copy(topUps = next) }
        viewModelScope.launch { repo.saveTopUps(next) }
        if (removed != null) showToast(t("toast.transferRemoved"), "info", ToastAction(t("toast.actionUndo")) {
            val restored = s.topUps.filter { it.id != id } + removed
            update { it.copy(topUps = restored) }
            viewModelScope.launch { repo.saveTopUps(restored) }
            showToast(t("toast.restored"), "success")
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
        showToast(t("toast.piggyCreated", "name" to newPiggy.name), "success")
    }

    fun renamePiggy(id: String, newName: String) {
        val s = _state.value
        val next = s.piggies.map { if (it.id == id) it.copy(name = newName.ifBlank { it.name }) else it }
        update { it.copy(piggies = next) }
        viewModelScope.launch { repo.savePiggies(next) }
        showToast(t("toast.piggyRenamed"), "success")
    }

    fun savePiggyTarget(id: String, amountStr: String) {
        val s = _state.value
        val v = amountStr.toDoubleOrNull()
        if (v == null || v.isNaN() || v < 0) {
            showToast(t("toast.invalidGoalAmount"), "error"); return
        }
        val next = s.piggies.map { if (it.id == id) it.copy(target = v) else it }
        update { it.copy(piggies = next) }
        viewModelScope.launch { repo.savePiggies(next) }
        showToast(
            if (v > 0) t("toast.savingsGoalSet", "amount" to fmt(v, s.cur)) else t("toast.savingsGoalCleared"),
            "success"
        )
    }

    fun depositPiggy(id: String, amountStr: String) {
        val s = _state.value
        val v = amountStr.toDoubleOrNull() ?: return
        if (v <= 0) {
            showToast(t("toast.invalidAmount"), "error"); return
        }
        if (v > s.bankBalance) {
            showToast(t("toast.notEnoughForAdd", "amount" to fmt(s.bankBalance, s.cur)), "error"); return
        }
        val targetPiggy = s.piggies.find { it.id == id } ?: s.activePiggy
        val prev = targetPiggy.saved
        val saved = prev + v
        val balance = s.balance.copy(start = s.balance.start - v)
        val next = s.piggies.map { if (it.id == targetPiggy.id) it.copy(saved = saved) else it }
        update { it.copy(balance = balance, piggies = next) }
        viewModelScope.launch { repo.saveBalance(balance); repo.savePiggies(next) }
        showToast(t("toast.addedToPiggy", "amount" to fmt(v, s.cur), "name" to targetPiggy.name), "success")
        if (targetPiggy.target > 0 && prev < targetPiggy.target && saved >= targetPiggy.target) {
            showToast(t("toast.goalComplete", "name" to targetPiggy.name), "success")
        }
    }

    fun breakPiggy(id: String) {
        val s = _state.value
        val targetPiggy = s.piggies.find { it.id == id } ?: s.activePiggy
        if (targetPiggy.saved <= 0) {
            showToast(t("toast.piggyEmpty", "name" to targetPiggy.name), "error"); return
        }
        confirm = ConfirmReq(
            title = t("confirm.breakPiggyTitle", "name" to targetPiggy.name),
            msg = t("confirm.breakPiggyMsg", "amount" to fmt(targetPiggy.saved, s.cur)),
            onConfirm = {
                val balance = s.balance.copy(start = s.balance.start + targetPiggy.saved)
                val next = s.piggies.map { if (it.id == targetPiggy.id) it.copy(saved = 0.0) else it }
                update { it.copy(balance = balance, piggies = next) }
                viewModelScope.launch { repo.saveBalance(balance); repo.savePiggies(next) }
                dismissConfirm()
                showToast(
                    t(
                        "toast.piggyBroken",
                        "name" to targetPiggy.name,
                        "amount" to fmt(targetPiggy.saved, s.cur)
                    ),
                    "success"
                )
            },
            onCancel = { dismissConfirm() },
        )
    }

    fun deletePiggy(id: String) {
        val s = _state.value
        if (s.piggies.size <= 1) {
            showToast(t("toast.cannotDeleteOnlyPiggy"), "error"); return
        }
        val targetPiggy = s.piggies.find { it.id == id } ?: return
        confirm = ConfirmReq(
            title = t("confirm.deletePiggyTitle", "name" to targetPiggy.name),
            msg = if (targetPiggy.saved > 0) t(
                "confirm.deletePiggyWithSavings",
                "amount" to fmt(targetPiggy.saved, s.cur)
            ) else t("confirm.deletePiggy", "name" to targetPiggy.name),
            onConfirm = {
                val balance =
                    if (targetPiggy.saved > 0) s.balance.copy(start = s.balance.start + targetPiggy.saved) else s.balance
                val next = s.piggies.filter { it.id != id }
                val newActiveId = if (s.activePiggyId == id) next.first().id else s.activePiggyId
                update { it.copy(balance = balance, piggies = next, activePiggyId = newActiveId) }
                viewModelScope.launch { repo.saveBalance(balance); repo.savePiggies(next) }
                dismissConfirm()
                showToast(t("toast.piggyDeleted", "name" to targetPiggy.name), "success")
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
        val raw = amount.toDoubleOrNull() ?: return
        if (raw <= 0) return
        val cat = selCats.firstOrNull() ?: "food"
        /* In travel mode the typed figure is the foreign amount; `amount` always stores
           the home-currency equivalent so budgets and statistics stay comparable. */
        val abroad = s.travelActive && s.travelRate > 0
        val v = if (abroad) raw * s.travelRate else raw
        val entry = Expense(
            id = uid(),
            date = entryDate.ifEmpty { s.today },
            amount = v,
            categories = listOf(cat),
            category = cat,
            note = note.trim(),
            receipt = receipt,
            tags = if (abroad) cleanTags(tags + listOf("travel", s.travel.name)) else cleanTags(tags),
            currency = if (abroad) s.travel.currency else null,
            foreignAmount = if (abroad) raw else null,
        )
        val next = s.expenses + entry
        update { it.copy(expenses = next) }
        amount = ""; note = ""; entryDate = todayKey(); receipt = null; tags = emptyList()
        val catName = s.cats.find { it.id == cat }?.label ?: cat
        showToast(
            if (abroad) t(
                "toast.loggedAbroad",
                "amount" to fmt(raw, s.travel.currency),
                "home" to fmt(v, s.cur),
                "category" to catName
            )
            else t("toast.logged", "amount" to fmt(v, s.cur), "category" to catName),
            "success"
        )
        viewModelScope.launch { repo.saveExpenses(next) }
        checkBudgetAlerts()
    }

    fun startEdit(e: HistoryEntry) {
        editingId = e.id
        /* Show the foreign figure when editing an entry that was logged abroad. */
        val shown = if (e.currency != null && e.foreignAmount != null) e.foreignAmount else e.amount
        amount = if (shown % 1.0 == 0.0) shown.toLong().toString() else shown.toString()
        note = e.note
        selCats = entryCats(e).take(1).ifEmpty { listOf("food") } // single-select
        entryDate = e.date
        receipt = e.receipt
        tags = cleanTags(e.tags)
    }

    fun updateExpense() {
        val s = _state.value
        val id = editingId ?: return
        val raw = amount.toDoubleOrNull() ?: return
        if (raw <= 0) return
        val cat = selCats.firstOrNull() ?: "food"
        val next = s.expenses.map { cur ->
            if (cur.id != id) cur
            else {
                val abroad = cur.currency != null && cur.foreignAmount != null
                val v = if (abroad && s.travelRate > 0) raw * s.travelRate else raw
                cur.copy(
                    amount = v,
                    categories = listOf(cat),
                    category = cat,
                    note = note.trim(),
                    date = entryDate.ifEmpty { s.today },
                    receipt = receipt,
                    tags = cleanTags(tags),
                    foreignAmount = if (abroad) raw else cur.foreignAmount,
                )
            }
        }
        update { it.copy(expenses = next) }
        cancelEdit()
        showToast(t("toast.spendUpdated"), "success")
        viewModelScope.launch { repo.saveExpenses(next) }
        checkBudgetAlerts()
    }

    fun cancelEdit() {
        editingId = null; amount = ""; note = ""; entryDate = todayKey(); receipt = null; tags = emptyList()
    }

    fun removeExpense(id: String) {
        val s = _state.value
        val removed = s.expenses.find { it.id == id }
        val removedIndex = s.expenses.indexOfFirst { it.id == id }
        val next = s.expenses.filter { it.id != id }
        if (editingId == id) cancelEdit()
        update { it.copy(expenses = next) }
        viewModelScope.launch { repo.saveExpenses(next) }
        if (removed != null) showToast(t("toast.spendRemoved"), "info", ToastAction(t("toast.actionUndo")) {
            // Re-insert at the original index so undo preserves ordering.
            val current = _state.value.expenses.filter { it.id != id }.toMutableList()
            current.add(removedIndex.coerceIn(0, current.size), removed)
            update { it.copy(expenses = current) }
            viewModelScope.launch { repo.saveExpenses(current) }
            showToast(t("toast.restored"), "success")
            checkBudgetAlerts()
        })
        checkBudgetAlerts()
    }

    fun duplicateExpense(id: String) {
        val s = _state.value
        val e = s.expenses.find { it.id == id } ?: return
        val entry = e.copy(id = uid(), date = s.today)
        val next = s.expenses + entry
        update { it.copy(expenses = next) }
        viewModelScope.launch { repo.saveExpenses(next) }
        showToast(t("toast.duplicated", "amount" to fmt(e.amount, s.cur)), "success")
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
            showToast(t("toast.enterCategoryName"), "error"); return
        }
        val id = n.lowercase().replace(Regex("\\s+"), "-").replace(Regex("[^a-z0-9-]"), "")
        if (id.isEmpty() || s.cats.any { it.id == id }) {
            showToast(t("toast.invalidCategoryName"), "error"); return
        }
        val next = s.categories + Category(id, n, glyph.ifEmpty { "★" })
        val theme = s.theme.copy(catColors = s.theme.catColors + (id to "#7c8896"))
        update { it.copy(categories = next, theme = theme) }
        viewModelScope.launch { repo.saveCategories(next); repo.saveTheme(theme) }
        showToast(t("toast.categoryAdded", "name" to n), "success")
    }

    fun removeCategory(id: String) {
        val s = _state.value
        if (s.expenses.any { expCats(it).contains(id) }) {
            showToast(t("toast.cannotDeleteCategory"), "error"); return
        }
        val next = s.categories.filter { it.id != id }
        update { it.copy(categories = next) }
        viewModelScope.launch { repo.saveCategories(next) }
        if (selCats.contains(id)) {
            selCats = (selCats.filter { it != id }).ifEmpty { listOf(next.firstOrNull()?.id ?: "food") }
        }
        showToast(t("toast.categoryRemoved"))
    }

    /* ─── Theme ─── */

    fun applyPreset(key: String) {
        val preset = PRESETS[key] ?: return
        update { it.copy(theme = preset, savedTheme = null) }
        viewModelScope.launch { repo.saveTheme(preset); repo.saveSavedTheme(null) }
        showToast(t("toast.themeApplied", "name" to key.replaceFirstChar { it.uppercase() }), "success")
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
        showToast(t("toast.themeReset"), "success")
    }

    /** Ending a trip changes nothing about the entries — confirm first so it isn't a surprise. */
    fun endTravel() {
        confirm = ConfirmReq(
            title = t("confirm.endTripTitle"),
            msg = t("confirm.endTripMsg"),
            onConfirm = {
                updatePrefs { it.copy(travel = it.travel.copy(active = false)) }
                dismissConfirm()
                showToast(t("toast.tripEnded"), "info")
            },
            onCancel = { dismissConfirm() },
        )
    }

    /**
     * Pulls the current market rate for the active trip. `auto` respects the user's
     * automatic-sync toggle. Refreshing only affects *future* entries: every logged spend
     * keeps the home-currency amount it was recorded with, so this never rewrites history.
     */
    fun refreshTravelRate(auto: Boolean = false) {
        val p = _state.value.prefs
        val tr = p.travel
        if (!tr.active || tr.currency.isBlank()) {
            rateStatus = ""
            return
        }
        if (auto && !tr.rateAuto) return
        rateStatus = "loading"
        viewModelScope.launch {
            val res = Fx.fetchRate(tr.currency, p.currency.ifEmpty { "MYR" })
            if (res == null) {
                rateStatus = "error"
                return@launch
            }
            val (rate, date) = res
            updatePrefs { it.copy(travel = it.travel.copy(rate = rate, rateUpdatedAt = date)) }
            rateStatus = if (date.isNotEmpty()) "ok:$date" else "ok"
        }
    }

    fun setLanguage(lang: String) = updatePrefs { it.copy(lang = lang) }

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
            val saved = s.savedTheme!!
            update { it.copy(theme = saved, savedTheme = null) }
            viewModelScope.launch { repo.saveTheme(saved); repo.saveSavedTheme(null) }
            showToast(t("toast.restoredPreviousTheme"), "success")
            return
        }
        val target = if (isDark(s.theme)) PRESETS["paper"]!! else PRESETS["mono"]!!
        update { it.copy(theme = target, savedTheme = s.theme) }
        viewModelScope.launch { repo.saveTheme(target); repo.saveSavedTheme(s.theme) }
        showToast(if (isDark(s.theme)) t("toast.paperThemeApplied") else t("toast.monoThemeApplied"), "success")
    }

    /* ─── Prefs ─── */

    fun updatePrefs(f: (Prefs) -> Prefs) {
        val next = f(_state.value.prefs)
        update { it.copy(prefs = next) }
        viewModelScope.launch { repo.savePrefs(next) }
    }

    /* ─── Reorderable cards ─── */

    private fun cardOrderBase(s: LedgerState): List<String> =
        com.ledger.app.data.mergeCardOrder(s.prefs.cardOrder)

    private fun cardOrderOf(s: LedgerState): List<String> =
        com.ledger.app.data.dashboardCardOrder(s.prefs.cardOrder, s.balancesOn)

    fun moveCard(id: String, dir: Int) {
        val s = _state.value
        // Index within the *visible* order the UI renders (hidden log/history cards are
        // filtered out), then swap the two cards' positions in the full persisted list
        // so hidden cards keep their slots and the move is never a no-op.
        val visible = cardOrderOf(s)
        val idx = visible.indexOf(id)
        if (idx < 0) return
        val swapWith = idx + dir
        if (swapWith < 0 || swapWith >= visible.size) return
        val other = visible[swapWith]
        val full = cardOrderBase(s).toMutableList()
        val i = full.indexOf(id)
        val j = full.indexOf(other)
        if (i < 0 || j < 0) return
        val tmp = full[i]; full[i] = full[j]; full[j] = tmp
        updatePrefs { it.copy(cardOrder = full) }
    }

    fun resetCardOrder() {
        updatePrefs { it.copy(cardOrder = com.ledger.app.data.defaultCardOrder) }
        showToast(t("toast.cardOrderReset"), "success")
    }

    /* ─── Wallpaper & Notifications ─── */

    fun setWallpaperFromUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val mime = context.contentResolver.getType(uri)
                if (mime?.startsWith("image/") != true) {
                    showToast(t("toast.chooseImageFile"), "error")
                    return@launch
                }
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val ext = "jpg"
                val file = java.io.File(context.filesDir, "wallpaper_${System.currentTimeMillis()}.$ext")
                context.filesDir.listFiles { _, name -> name.startsWith("wallpaper_") }?.forEach { it.delete() }

                inputStream.use { input ->
                    file.outputStream().use { out ->
                        input.copyTo(out)
                    }
                }
                val path = file.absolutePath
                updatePrefs { it.copy(wallpaper = path) }
                showToast(t("toast.wallpaperSet"), "success")
            } catch (e: Exception) {
                showToast(t("toast.wallpaperSetFailed"), "error")
            }
        }
    }

    fun clearWallpaper(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            context.filesDir.listFiles { _, name -> name.startsWith("wallpaper_") }?.forEach { it.delete() }
            updatePrefs { it.copy(wallpaper = null) }
            showToast(t("toast.wallpaperRemoved"), "success")
        }
    }

    /* ─── Receipt photos ─── */

    /** Decode, downscale and attach [uri] as the pending expense's receipt. */
    fun setReceiptFromUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@runCatching null
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: return@runCatching null
                    bitmapToReceiptDataUrl(bmp)
                }.getOrNull()
            }
            if (data != null) {
                receipt = data
                showToast(t("toast.receiptAttached"), "success")
            } else {
                showToast(t("toast.receiptReadFailed"), "error")
            }
        }
    }

    fun clearReceipt() {
        receipt = null
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
            showToast(t("toast.remindersOn"), "success")
        } else {
            NotificationHelper.cancelDailyReminder(context)
            showToast(t("toast.remindersOff"), "info")
        }
    }

    fun setReminderTime(hour: Int, minute: Int, context: android.content.Context) {
        updatePrefs { it.copy(reminderHour = hour, reminderMinute = minute) }
        if (_state.value.prefs.notificationsEnabled) {
            NotificationHelper.scheduleDailyReminder(context, hour, minute)
            showToast(t("toast.reminderTimeUpdated"), "success")
        }
    }

    fun toggleBudgetAlerts(enabled: Boolean) {
        updatePrefs { it.copy(budgetAlertsEnabled = enabled) }
        showToast(if (enabled) t("toast.budgetAlertsEnabled") else t("toast.budgetAlertsDisabled"), "info")
    }

    /* ─── Budget alerts (80% / 100%, once per period) ─── */

    /**
     * Fire notifications when the monthly budget or a per-category budget crosses 80%
     * and 100%. Each threshold fires at most once per period (the period is recorded
     * alongside the fired set), and the OS permission is respected inside
     * [NotificationHelper.showBudgetAlert].
     */
    private fun checkBudgetAlerts() {
        val s = _state.value
        if (!s.prefs.budgetAlertsEnabled) return
        val settings = s.settings ?: return
        if (s.effectiveMonthlyBudget <= 0 && s.catBudgets.isEmpty()) return
        val period = settings.startDate
        val context = repo.appContext
        viewModelScope.launch {
            val stored = repo.getBudgetAlerts()
            val fired = if (stored.period == period) stored.fired.toMutableSet() else mutableSetOf()
            val newlyCrossed = mutableListOf<Pair<String, String>>() // title to message

            fun check(id: String, label: String, spent: Double, budget: Double, isMonthly: Boolean) {
                if (budget <= 0) return
                val pct = spent / budget * 100
                for (threshold in listOf(80, 100)) {
                    if (pct + 0.0001 >= threshold && fired.add("$id:$threshold")) {
                        val scopeLabel = if (isMonthly) t("toast.monthlyBudget") else label
                        val title = if (threshold >= 100) t("toast.budgetExceeded") else t("toast.budgetAlert80")
                        newlyCrossed.add(
                            title to t(
                                "toast.budgetUsed",
                                "scope" to scopeLabel,
                                "spent" to fmt(spent, s.cur),
                                "budget" to fmt(budget, s.cur)
                            )
                        )
                    }
                }
            }

            if (s.effectiveMonthlyBudget > 0) {
                check("monthly", t("toast.thisMonth"), s.periodSpent, s.effectiveMonthlyBudget, true)
            }
            s.catBudgets.forEach { (catId, budget) ->
                val label = s.cats.find { it.id == catId }?.label ?: catId
                val spent = s.expenses
                    .filter { it.date >= period && it.date <= s.today && expCats(it).contains(catId) }
                    .sumOf { it.amount }
                check("cat:$catId", label, spent, budget, false)
            }

            if (newlyCrossed.isNotEmpty()) {
                repo.saveBudgetAlerts(BudgetAlertState(period, fired.toList()))
                newlyCrossed.forEach { (title, msg) ->
                    NotificationHelper.showBudgetAlert(context, title, msg)
                }
            }
        }
    }

    /* ─── Monthly insights (month-over-month) ─── */

    /** Calendar-month comparison of spending, derived here rather than in the composable. */
    fun insights(s: LedgerState): InsightsData {
        val today = parseDateOrNull(s.today) ?: LocalDate.now()
        val monthStart = firstOfMonthKey(today)
        val daysElapsed = today.dayOfMonth
        val daysTotal = daysInMonth(today)
        val prevMonth = today.withDayOfMonth(1).minusMonths(1)
        val prevStart = prevMonth.withDayOfMonth(1).toString()
        val prevEnd = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()).toString()

        val thisExp = s.expenses.filter { it.date >= monthStart && it.date <= s.today }
        val lastExp = s.expenses.filter { it.date >= prevStart && it.date <= prevEnd }
        val thisMonth = thisExp.sumOf { it.amount }
        val lastMonth = lastExp.sumOf { it.amount }
        val changePct = if (lastMonth > 0) (thisMonth - lastMonth) / lastMonth * 100 else null

        val thisByCat = thisExp.groupBy { expCats(it).firstOrNull() ?: "other" }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
        val lastByCat = lastExp.groupBy { expCats(it).firstOrNull() ?: "other" }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
        val biggestCatId = (thisByCat.keys + lastByCat.keys)
            .maxByOrNull { abs((thisByCat[it] ?: 0.0) - (lastByCat[it] ?: 0.0)) }
        val biggestDelta = biggestCatId?.let { (thisByCat[it] ?: 0.0) - (lastByCat[it] ?: 0.0) } ?: 0.0
        val biggestLabel = biggestCatId?.let { id -> s.cats.find { it.id == id }?.label ?: id }

        val avgPerDay = if (daysElapsed > 0) thisMonth / daysElapsed else 0.0
        val byDay = thisExp.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.amount } }

        return InsightsData(
            thisMonth = thisMonth,
            lastMonth = lastMonth,
            changePct = changePct,
            biggestCategoryLabel = if (biggestDelta != 0.0) biggestLabel else null,
            biggestCategoryDelta = biggestDelta,
            avgPerDay = avgPerDay,
            daysElapsed = daysElapsed,
            projected = avgPerDay * daysTotal,
            monthlyBudget = s.effectiveMonthlyBudget,
            bestDay = byDay.minByOrNull { it.value }?.toPair(),
            worstDay = byDay.maxByOrNull { it.value }?.toPair(),
        )
    }

    fun testReminderNotification(context: android.content.Context) {
        /* Preview the ordinary reminder (with the real streak); the grace-period variant is
           reserved for the day it actually applies. */
        NotificationHelper.showDailyReminder(context, _state.value.spendStreak, false)
        showToast(t("toast.testNotificationSent"), "success")
    }

    fun toggleGlass(enabled: Boolean) {
        updatePrefs { it.copy(glassEnabled = enabled) }
        showToast(if (enabled) t("toast.glassEnabled") else t("toast.glassDisabled"), "info")
    }

    fun updateGlassBlur(blur: Int) {
        updatePrefs { it.copy(glassBlur = blur.coerceIn(0, 24)) }
    }

    fun updateGlassOpacity(opacity: Int) {
        updatePrefs { it.copy(glassOpacity = opacity.coerceIn(20, 100)) }
    }

    fun updateGlassInnerOpacity(opacity: Int) {
        updatePrefs { it.copy(glassInnerOpacity = opacity.coerceIn(0, 100)) }
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
            title = t("confirm.clearAllTitle"),
            msg = t("confirm.clearAllMsg"),
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
                showToast(t("toast.allDataCleared"), "success")
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
            ?: return t("toast.importInvalidBackup")
        if (obj["expenses"] !is JsonArray) return t("toast.importInvalidBackup")
        val s = _state.value

        // Older/corrupted exports occasionally mangle the category key (e.g. "category'");
        // repair it so the expense still imports with its category.
        val expensesArr = obj["expenses"] as JsonArray
        val normalizedExpenses = JsonArray(expensesArr.map { normalizeExpenseElement(it) })
        val newExpenses = runCatching { json.decodeFromJsonElement<List<Expense>>(normalizedExpenses) }
            .getOrElse { return t("toast.importReadFailed", "detail" to it.message) }
        val finalExpenses = com.ledger.app.data.normalizeExpenses(newExpenses)

        var settings = s.settings
        obj["settings"]?.let { el ->
            if (el !is JsonNull && el is JsonObject &&
                el["monthlyBudget"] is JsonPrimitive && el["periodDays"] is JsonPrimitive && el["startDate"] is JsonPrimitive
            ) {
                settings = runCatching { json.decodeFromJsonElement<Settings>(el) }.getOrNull()
                    ?.let { sanitizeSettings(it) } ?: s.settings
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
                            // Only overwrite map/list prefs when the backup actually carries
                            // the key, so an older/partial backup can't reset custom values.
                            heatColors = if (el.containsKey("heatColors")) p.heatColors else prefs.heatColors,
                            font = el["font"]?.jsonPrimitive?.contentOrNull ?: prefs.font,
                            cardOrder = if (el.containsKey("cardOrder") && p.cardOrder.isNotEmpty())
                                p.cardOrder else prefs.cardOrder,
                            balancesEnabled = el["balancesEnabled"]?.jsonPrimitive?.booleanOrNull
                                ?: prefs.balancesEnabled,
                            overspendFromBalance = el["overspendFromBalance"]?.jsonPrimitive?.booleanOrNull
                                ?: prefs.overspendFromBalance,
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
        showToast(t("toast.restoredEntries", "count" to finalExpenses.size), "success")
        checkBudgetAlerts()
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
                lastPushedJson = null
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun attachFirestoreListener(uid: String) {
        detachFirestoreListener()
        // Switching accounts must not reuse the previous account's dedupe state.
        if (syncedUid != uid) lastPushedJson = null
        val db = FirebaseManager.getFirestore(repo.appContext) ?: return
        try {
            snapshotListener = db.collection("ledger").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("LedgerViewModel", "Sync listener error", error)
                        _state.value = _state.value.copy(
                            syncError = true,
                            syncErrorMsg = error.localizedMessage ?: t("toast.syncError")
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
                                applyRemote(data)
                                repo.setLastSync(uid, remoteAt)
                                _state.value = _state.value.copy(
                                    lastSyncedAt = remoteAt,
                                    syncError = false,
                                    syncErrorMsg = ""
                                )
                                showToast(t("toast.syncedFromCloud"), "success")
                            } else if (remoteAt > last) {
                                if (cloudLooksDefault && localHasData) {
                                    pushSync(uid)
                                } else {
                                    applyRemote(data)
                                    repo.setLastSync(uid, remoteAt)
                                    _state.value = _state.value.copy(
                                        lastSyncedAt = remoteAt,
                                        syncError = false,
                                        syncErrorMsg = ""
                                    )
                                    if (last == 0L) {
                                        showToast(t("toast.syncedFromCloud"), "success")
                                    }
                                }
                            } else if (remoteAt == 0L) {
                                if (cloudLooksDefault || localHasData) {
                                    safePushSync(uid)
                                } else {
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
                syncErrorMsg = e.localizedMessage ?: t("toast.syncSubscribeFailed")
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
                val safe = sanitizeSettings(it)
                s = s.copy(settings = safe)
                repo.saveSettings(safe)
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
            checkBudgetAlerts()
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
                    // The write didn't land — clear the dedupe marker so the same
                    // payload is retried instead of being skipped forever.
                    lastPushedJson = null
                    _state.value = _state.value.copy(
                        syncError = true,
                        syncErrorMsg = e.localizedMessage ?: t("toast.syncPushFailed")
                    )
                }
        } catch (e: Exception) {
            Log.w("LedgerViewModel", "Sync push failed", e)
            _state.value = _state.value.copy(
                syncError = true,
                syncErrorMsg = e.localizedMessage ?: t("toast.syncPushFailed")
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
        // A corrupt local blob is left untouched — never push defaults over it.
        if (_state.value.dataCorrupt) return
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
            showToast(t("toast.signInToSync"), "info")
            return
        }
        showToast(t("toast.syncing"), "info")
        lastPushedJson = null // Force push
        pushSync(user.uid)
    }

    fun signInGoogle(activity: Activity, launcher: ActivityResultLauncher<Intent>? = null) {
        if (!FirebaseConfig.isConfigured) {
            showToast(t("toast.syncNotConfigured"), "error")
            return
        }
        if (!FirebaseManager.init(activity)) {
            showToast(t("toast.firebaseLoadFailed"), "error")
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
            showToast(t("toast.firebaseLoadFailed"), "error")
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
                    showToast(t("toast.signedIn"), "success")
                }
                .addOnFailureListener { e ->
                    handleAuthError(e)
                }
        } else {
            auth.startActivityForSignInWithProvider(activity, provider)
                .addOnSuccessListener {
                    showToast(t("toast.signedIn"), "success")
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
                showToast(t("toast.signedIn"), "success")
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
            showToast(t("toast.signInCancelled"), "info")
        } else if (msg.contains("INVALID APP ID", ignoreCase = true) || msg.contains(
                "INVALID_APP_ID",
                ignoreCase = true
            )
        ) {
            showToast(
                t("toast.firebaseAndroidAppMissing"),
                "error"
            )
        } else {
            showToast(msg.ifEmpty { t("toast.signInFailed") }, "error")
        }
    }

    fun signOutGoogle() {
        try {
            FirebaseAuth.getInstance().signOut()
            showToast(t("toast.signedOut"), "info")
        } catch (e: Exception) {
            showToast(t("toast.signOutFailed"), "error")
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
