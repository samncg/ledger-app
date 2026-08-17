package com.ledger.app.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val label: String,
    val locale: Locale,
)

val CURRENCIES: Map<String, CurrencyInfo> = mapOf(
    "MYR" to CurrencyInfo("MYR", "RM", "Malaysian Ringgit", Locale("en", "MY")),
    "USD" to CurrencyInfo("USD", "$", "US Dollar", Locale.US),
    "EUR" to CurrencyInfo("EUR", "€", "Euro", Locale.GERMANY),
    "GBP" to CurrencyInfo("GBP", "£", "British Pound", Locale.UK),
    "SGD" to CurrencyInfo("SGD", "S$", "Singapore Dollar", Locale("en", "SG")),
    "JPY" to CurrencyInfo("JPY", "¥", "Japanese Yen", Locale.JAPAN),
    "CNY" to CurrencyInfo("CNY", "¥", "Chinese Yuan", Locale.CHINA),
    "INR" to CurrencyInfo("INR", "₹", "Indian Rupee", Locale("en", "IN")),
    "AUD" to CurrencyInfo("AUD", "A$", "Australian Dollar", Locale("en", "AU")),
    "CAD" to CurrencyInfo("CAD", "C$", "Canadian Dollar", Locale.CANADA),
)

/** Format a number like the web app: "RM 1,234.50", JPY with 0 decimals. */
fun fmt(n: Double, cur: String): String {
    val c = CURRENCIES[cur] ?: CURRENCIES["MYR"]!!
    val sign = if (n < 0) "-" else ""
    val decimals = if (c.code == "JPY") 0 else 2
    val nf = NumberFormat.getNumberInstance(c.locale).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }
    return "${c.symbol} $sign${nf.format(abs(n))}"
}

/** Plain symbol for quick-amount chips, e.g. "RM5". */
fun symbol(cur: String): String = (CURRENCIES[cur] ?: CURRENCIES["MYR"]!!).symbol
