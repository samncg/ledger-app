package com.ledger.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Exchange rates from the European Central Bank's daily reference set, served by
 * frankfurter.app. No API key, and it covers every currency this app offers. The request
 * carries only the two currency codes — no amounts, notes or identifiers leave the device.
 *
 * A fetched rate is only ever used for *new* entries: every logged spend stores the
 * home-currency amount it was converted at, so refreshing a rate never rewrites history.
 */
object Fx {

    /** Home-currency units per 1 unit of [from], or null when the rate can't be fetched. */
    suspend fun fetchRate(from: String, to: String): Pair<Double, String>? = withContext(Dispatchers.IO) {
        if (from.isBlank() || to.isBlank()) return@withContext null
        if (from == to) return@withContext 1.0 to ""
        runCatching {
            val conn =
                (URL("https://api.frankfurter.app/latest?from=$from&to=$to").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val rate = json.optJSONObject("rates")?.optDouble(to, Double.NaN) ?: Double.NaN
                if (rate.isFinite() && rate > 0) rate to json.optString("date", "") else null
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }
}

/** Decimals needed to show a rate without collapsing it to "0.03". */
fun rateDecimals(rate: Double): Int {
    val n = kotlin.math.abs(rate)
    return when {
        n == 0.0 -> 2
        n >= 100 -> 2
        n >= 1 -> 4
        n >= 0.01 -> 5
        else -> 6
    }
}
