package com.ledger.app.util

/**
 * The logging streak: consecutive days with at least one logged spend, with `grace`
 * missed days forgiven. `graceRisk` is true when today's log is the last chance to keep
 * the run alive — nothing is logged today, yesterday was missed, and grace is what is
 * holding the streak together.
 */
data class StreakInfo(val current: Int, val best: Int, val loggedToday: Boolean, val graceRisk: Boolean)

/** Ceiling on either walk, so a corrupt date set can't keep the loop spinning. */
private const val MAX_STREAK_DAYS = 3650

/** Grace days a run may forgive; clamped so a bad pref can't swallow an unbounded gap. */
private const val MAX_GRACE = 60

fun streakInfo(dates: Collection<String>, today: String, grace: Int): StreakInfo {
    val spendDays = dates.mapNotNull { parseDateOrNull(it)?.toString() }.toHashSet()
    val loggedToday = today in spendDays
    val g = grace.coerceIn(0, MAX_GRACE)

    /* Walk back from today, or from yesterday when today isn't logged yet: a run stays
       alive until a whole day is missed. */
    var current = 0
    var cursor = if (loggedToday) today else addDays(today, -1)
    var misses = 0
    while (current < MAX_STREAK_DAYS) {
        if (cursor in spendDays) {
            current++
            cursor = addDays(cursor, -1)
        } else if (misses < g) {
            misses++
            cursor = addDays(cursor, -1)
        } else break
    }

    /* Longest run ever — only measured from the end of a run, so a run isn't counted twice. */
    var best = 0
    for (day in spendDays) {
        if (addDays(day, 1) in spendDays) continue
        var n = 0
        var cur = day
        var m = 0
        while (n < MAX_STREAK_DAYS) {
            if (cur in spendDays) {
                n++
                cur = addDays(cur, -1)
            } else if (m < g) {
                m++
                cur = addDays(cur, -1)
            } else break
        }
        if (n > best) best = n
    }

    /* Last chance for the run: nothing logged today, yesterday missed, and grace already
       carrying the streak — so logging today is the only way to keep it. */
    val graceRisk = !loggedToday && current > 0 && addDays(today, -1) !in spendDays
    return StreakInfo(current, best, loggedToday, graceRisk)
}
