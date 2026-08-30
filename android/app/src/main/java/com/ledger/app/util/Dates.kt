package com.ledger.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Date helpers — mirrors the web app's date utilities (ISO "yyyy-MM-dd" keys). */
val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun todayKey(d: LocalDate = LocalDate.now()): String = d.format(ISO)

fun parseDate(s: String): LocalDate = LocalDate.parse(s)

fun addDays(dateStr: String, n: Int): String = parseDate(dateStr).plusDays(n.toLong()).format(ISO)

fun daysInMonth(d: LocalDate = LocalDate.now()): Int = d.lengthOfMonth()

fun firstOfMonthKey(d: LocalDate = LocalDate.now()): String = d.withDayOfMonth(1).format(ISO)

/** First day of the month `offset` months before `base` (offset 0 = base's month). */
fun monthStartKey(base: String, offset: Int): String =
    parseDate(base).withDayOfMonth(1).minusMonths(offset.toLong()).format(ISO)

/** Last day of the month `offset` months before `base`; offset 0 ends on `base` (today). */
fun monthEndKey(base: String, offset: Int): String {
    val m = parseDate(base).withDayOfMonth(1).minusMonths(offset.toLong())
    return if (offset == 0) base else m.withDayOfMonth(m.lengthOfMonth()).format(ISO)
}

/** Human-friendly month label, e.g. "August 2026". */
fun monthLabel(base: String, offset: Int): String =
    parseDate(base).withDayOfMonth(1).minusMonths(offset.toLong())
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

/** days between a and b (b - a), in days. */
fun dayDiff(a: String, b: String): Long = ChronoUnit.DAYS.between(parseDate(a), parseDate(b))

fun relativeDate(dateStr: String, today: String): String {
    val diff = dayDiff(today, dateStr)
    return when {
        diff == 0L -> "Today"
        diff == -1L -> "Yesterday"
        diff == 1L -> "Tomorrow"
        diff > -7 && diff < 0 -> "${-diff}d ago"
        else -> dateStr.substring(5).replace("-", "/")
    }
}

fun groupLabel(dateStr: String, today: String): String {
    val diff = dayDiff(today, dateStr)
    return when {
        diff == 0L -> "Today"
        diff == -1L -> "Yesterday"
        diff >= -6 && diff < 0 -> "This week"
        else -> parseDate(dateStr).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }
}

fun uid(): String = System.currentTimeMillis().toString() + (1000..9999).random()

/** Advance a date by a frequency — used by automations. */
fun advanceDate(d: LocalDate, freq: String): LocalDate = when (freq) {
    "weekly" -> d.plusWeeks(1)
    "monthly" -> d.plusMonths(1) // LocalDate clamps (e.g. 31st → last day of month)
    else -> d.plusDays(1)
}

/** Convert a UTC-midnight epoch millis (DatePicker) to a date key. */
fun dateKeyFromMillis(millis: Long?): String =
    millis?.let { LocalDate.ofEpochDay(it / 86_400_000L).format(ISO) } ?: ""

/** Convert a date key to UTC-midnight epoch millis (DatePicker initial value). */
fun millisFromDateKey(key: String): Long? = runCatching { parseDate(key).toEpochDay() * 86_400_000L }.getOrNull()
