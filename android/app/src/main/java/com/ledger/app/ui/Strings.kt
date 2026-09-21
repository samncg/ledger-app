package com.ledger.app.ui

import androidx.compose.runtime.mutableStateOf
import com.ledger.app.ui.locales.ES_STRINGS
import com.ledger.app.ui.locales.JA_STRINGS
import com.ledger.app.ui.locales.KO_STRINGS
import com.ledger.app.ui.locales.RU_STRINGS
import com.ledger.app.ui.locales.TH_STRINGS
import com.ledger.app.ui.locales.ZH_STRINGS
import com.ledger.app.ui.strings.APP_STRINGS
import com.ledger.app.ui.strings.CARD_STRINGS
import com.ledger.app.ui.strings.SHELL_STRINGS

/**
 * UI string table, mirroring the web app's `src/lib/i18n.js`.
 *
 * `EN` is the source of truth; every other dictionary is a partial override and any
 * missing key falls back to English. Placeholders use the same `{name}` syntax as the
 * web app, filled by [t]'s `vars`.
 *
 * The active language is a plain module variable rather than Compose state: the
 * dashboard calls [setLang] from the prefs it already observes, so any recomposition
 * triggered by a language change sees the new value.
 */
object Strings {
    val LANGS = listOf(
        "en" to "English",
        "es" to "Español",
        "zh" to "简体中文",
        "ru" to "Русский",
        "th" to "ไทย",
        "ja" to "日本語",
        "ko" to "한국어",
    )

    private val EN: Map<String, String> = mapOf(
        /* Top bar */
        "top.quickActions" to "Quick actions",
        "top.toggleTheme" to "Toggle light/dark",
        "top.customize" to "Customize",

        /* Log card */
        "log.title" to "Log a spend",
        "log.edit" to "Edit spend",
        "log.editing" to "Editing",
        "log.frequent" to "Frequent",
        "log.amount" to "Amount",
        "log.notePlaceholder" to "Note (optional)",
        "log.date" to "Date",
        "log.receipt" to "Receipt",
        "log.optional" to "optional",
        "log.attachPhoto" to "Attach photo",
        "log.remove" to "Remove",
        "log.tags" to "Tags",
        "log.tagPlaceholder" to "Add a tag and press Enter",
        "log.removeTag" to "Remove tag",
        "log.categories" to "Categories",
        "log.pickOne" to "pick one",
        "log.addSpend" to "Add spend",
        "log.update" to "Update",
        "log.cancel" to "Cancel",

        /* History */
        "history.title" to "History",
        "history.resetFilters" to "Reset filters",
        "history.all" to "All",
        "history.sortFilter" to "Sort & filter",
        "history.hide" to "Hide",
        "history.search" to "Search notes, categories or tags…",
        "history.newest" to "Newest first",
        "history.oldest" to "Oldest first",
        "history.amountDown" to "Amount ↓",
        "history.amountUp" to "Amount ↑",
        "history.from" to "From",
        "history.to" to "To",
        "history.tags" to "Tags",
        "history.any" to "Any",
        "history.entry" to "entry",
        "history.entries" to "entries",
        "history.spent" to "spent",
        "history.movedToBudget" to "moved to budget",
        "history.toppedUp" to "topped up",
        "history.categoryFilter" to "category filter",
        "history.categoryFilters" to "category filters",
        "history.tagFilter" to "tag filter",
        "history.tagFilters" to "tag filters",
        "history.noSpends" to "No spends yet. Add your first one above!",
        "history.noMatch" to "No entries match your filters.",
        "history.dataStays" to "Data stays on your device.",
        "history.tryAdjust" to "Try adjusting search or dates.",
        "history.transfersHidden" to "Transfers are hidden while a category filter is active.",
        "history.moveToBudget" to "Move to budget",
        "history.topUp" to "Top up",
        "history.returnToBalance" to "Return to balance",

        /* Hero */
        "hero.dailyAllowance" to "Daily allowance",
        "hero.savedToBalance" to "Saved to balance",
        "hero.leftoverBanked" to "Leftover allowance banked so far",
        "hero.rollover" to "Rollover",
        "hero.totalOver" to "Total over",
        "hero.unspentCarries" to "Unspent allowance carries over",
        "hero.spentOverAllowance" to "Spent over allowance",
        "hero.avgPerDay" to "Avg spending / day",
        "hero.underAllowance" to "Under allowance",
        "hero.overAllowance" to "Over allowance",
        "hero.projectedTotal" to "Projected total",
        "hero.overIfPace" to "{amount} over if pace holds",
        "hero.leftIfPace" to "{amount} left if pace holds",
        "hero.budgetProgress" to "Budget progress",
        "hero.left" to "{amount} left",
        "hero.over" to "{amount} over",
        "hero.dailySpendPeriod" to "Daily spend · this period",
        "hero.dayOf" to "Day {day} / {total}",
        "hero.moveMoney" to "Move money",
        "hero.topUp" to "Top up",
        "hero.savedToday" to "{amount} saved today",
        "hero.daysUnder" to "{days}d under budget",
        "hero.overTodayBy" to "Over today’s allowance by {amount}",
        "hero.under" to "Under",
        "hero.near" to "Near",
        "hero.over2" to "Over",
        "hero.today" to "Today",

        /* Drawer tabs */
        "tab.theme" to "Theme",
        "tab.chart" to "Chart",
        "tab.cats" to "Categories",
        "tab.prefs" to "Prefs",

        /* Drawer sections */
        "sec.wallpaper" to "Wallpaper (Local)",
        "sec.weather" to "Weather effects",
        "sec.glass" to "Glass & transparency",
        "sec.presets" to "Presets",
        "sec.interface" to "Interface",
        "sec.status" to "Status",
        "sec.typography" to "Typography",
        "sec.cardPanels" to "Card panels",
        "sec.cardLayout" to "Card layout",
        "sec.desktopCat" to "Desktop cat",
        "sec.preferences" to "Preferences",
        "sec.balance" to "Balance",
        "sec.currency" to "Currency",
        "sec.alerts" to "Alerts",
        "sec.appLock" to "App lock",
        "sec.cloudSync" to "Cloud sync",
        "sec.shortcuts" to "Keyboard shortcuts",
        "sec.streaks" to "Streaks",
        "sec.language" to "Language",
        "sec.travel" to "Travel mode",

        /* Preferences */
        "pref.compact" to "Compact density",
        "pref.compactDesc" to "Tighter spacing throughout the app.",
        "pref.groupHistory" to "Group history by date",
        "pref.groupHistoryDesc" to "Show Today, Yesterday, This week, and monthly headers.",
        "pref.tilt" to "3D tilt panels",
        "pref.tiltDesc" to "Cards and the hero lean toward your cursor. Mouse only.",
        "pref.balances" to "Bank balance system",
        "pref.balancesOn" to "On — keep a balance, move money to your budget, and bank leftover allowance at the end of each day.",
        "pref.balancesOff" to "Off — plain budgeting without a balance or transfers.",
        "pref.heroShows" to "Hero shows",
        "pref.heroDaily" to "Daily allowance",
        "pref.heroBalance" to "Balance",
        "pref.budgetAlerts" to "Budget alerts",
        "pref.budgetAlertsDesc" to "Warn once at 80% and once at 100% of each category budget and the monthly budget.",
        "pref.enableNotifications" to "Enable system notifications",
        "pref.streakGrace" to "Streak grace days",
        "pref.streakGraceDesc" to "Missed days forgiven inside a streak — a skipped day keeps the run alive.",
        "pref.graceOff" to "Off",
        "pref.edgeBlur" to "Edge blur & fade",
        "pref.edgeBlurDesc" to "Softens the top and bottom of the screen as content scrolls under them.",
        "pref.language" to "Language",

        /* Travel mode */
        "travel.title" to "Travel mode",
        "travel.desc" to "Log spends in a foreign currency. Each entry is converted to your home currency and tagged “travel”.",
        "travel.toggle" to "Travel mode",
        "travel.tripName" to "Trip name",
        "travel.tripNamePlaceholder" to "e.g. Tokyo",
        "travel.currency" to "Foreign currency",
        "travel.rate" to "Exchange rate",
        "travel.rateHint" to "1 {foreign} = {home}",
        "travel.start" to "Start travel mode",
        "travel.end" to "End trip",
        "travel.active" to "Travelling",
        "travel.spent" to "Spent abroad",
        "travel.equivalent" to "≈ {amount} in {home}",
        "travel.homeEquivalent" to "In home currency",
        "travel.logSpend" to "Log a travel spend",
        "travel.foreignAmount" to "Amount in {currency}",
        "travel.noSpends" to "No travel spends yet.",
        "travel.entry" to "travel entry",
        "travel.entries" to "travel entries",
        "travel.needsSetup" to "Set a currency and rate to start travel mode.",

        /* Setup */
        "setup.welcome" to "Welcome to Ledger",
        "setup.sub" to "Set your budget to get started. Everything stays on your device.",
        "setup.monthlyBudget" to "Monthly budget",
        "setup.periodDays" to "Period length (days)",
        "setup.startDate" to "Start date",
        "setup.startingBalance" to "Starting balance",
        "setup.currency" to "Currency",
        "setup.getStarted" to "Get started",

        /* Area fragments — kept in their own modules so each surface can be translated
           in isolation. Later spreads win, so a fragment may override a core key. */
    ) + CARD_STRINGS + SHELL_STRINGS + APP_STRINGS

    private val DICTS: Map<String, Map<String, String>> = mapOf(
        "en" to EN,
        "es" to ES_STRINGS,
        "zh" to ZH_STRINGS,
        "ru" to RU_STRINGS,
        "th" to TH_STRINGS,
        "ja" to JA_STRINGS,
        "ko" to KO_STRINGS,
    )

    /* Held in Compose snapshot state so that any composable calling t() during composition
       automatically recomposes when the language changes — even one whose own parameters
       didn't change (the always-visible nav bar, for instance). */
    private var lang = mutableStateOf("en")

    fun setLang(l: String) {
        lang.value = if (DICTS.containsKey(l)) l else "en"
    }

    fun currentLang(): String = lang.value

    /** Translates [key], falling back to English and finally to the key itself. */
    fun t(key: String, vararg vars: Pair<String, Any?>): String {
        var s = DICTS[lang.value]?.get(key) ?: EN[key] ?: return key
        for ((k, v) in vars) s = s.replace("{$k}", v?.toString() ?: "")
        return s
    }
}

/** Shorthand so call sites read `t("log.title")`. */
fun t(key: String, vararg vars: Pair<String, Any?>): String = Strings.t(key, *vars)
