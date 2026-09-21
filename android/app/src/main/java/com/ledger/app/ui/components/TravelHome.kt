package com.ledger.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.data.Expense
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.t
import com.ledger.app.util.fmt
import com.ledger.app.util.rateDecimals
import com.ledger.app.util.relativeDate
import com.ledger.app.util.symbol

/** One currency used on a trip, with its own subtotal. */
data class TravelCurrencyTotal(val code: String, val foreign: Double, val home: Double, val count: Int)

/**
 * Groups a trip's entries by the currency each was logged in. Entries keep their own
 * currency, so switching the trip's currency never re-expresses the older rows.
 */
fun travelCurrencyTotals(list: List<Expense>, fallbackCode: String): List<TravelCurrencyTotal> =
    list.groupBy { it.currency ?: fallbackCode }
        .map { (code, rows) ->
            TravelCurrencyTotal(
                code,
                rows.sumOf { it.foreignAmount ?: it.amount },
                rows.sumOf { it.amount },
                rows.size,
            )
        }
        .sortedByDescending { it.home }

private fun fmtRate(rate: Double): String = String.format("%.${rateDecimals(rate)}f", rate)

/**
 * Travel mode home — replaces the hero while a trip is active. Amounts are entered in the
 * foreign currency; every entry also carries its home-currency equivalent, so the budget
 * and statistics pages need no special casing.
 */
@Composable
fun TravelHero(s: LedgerState, onEnd: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val code = s.travel.currency.ifEmpty { "USD" }
    val totals = travelCurrencyTotals(s.travelList, code)
    val multi = totals.size > 1
    val spentHome = s.travelList.sumOf { it.amount }
    val todayHome = s.travelList.filter { it.date == s.today }.sumOf { it.amount }

    CardContainer(
        title = s.travel.name.ifEmpty { t("travel.active") },
        icon = Icons.Outlined.Flight,
        trailing = {
            Text(
                t("travel.end"),
                color = cs.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onEnd)
                    .padding(4.dp),
            )
        },
    ) {
        /* One currency: lead with what was actually spent abroad, and show the home
           equivalent beside it. Several: the home total is the only figure that still
           adds up, so it leads and each currency gets its own line. */
        Text(
            if (multi) fmt(spentHome, s.cur) else fmt(totals.firstOrNull()?.foreign ?: 0.0, code),
            fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(4.dp))
        if (multi) {
            totals.forEach { row ->
                Text(
                    "${fmt(row.foreign, row.code)} · ${row.count}",
                    fontSize = 12.5.sp,
                    color = cs.onSurfaceVariant,
                )
            }
        } else {
            Text(
                "${fmt(spentHome, s.cur)} · ${s.travelList.size} " +
                        if (s.travelList.size == 1) t("travel.entry") else t("travel.entries"),
                fontSize = 12.5.sp,
                color = cs.onSurfaceVariant,
            )
            if (s.travelRate > 0) {
                Text(
                    "${t("travel.rateHint", "foreign" to code, "home" to s.cur)} ${fmtRate(s.travelRate)}",
                    fontSize = 12.sp,
                    color = cs.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TravelStat(
                t("travel.spent"), fmt(spentHome, s.cur), "${s.travelList.size} ${
                    if (s.travelList.size == 1) t("travel.entry") else t("travel.entries")
                }"
            )
            TravelStat(
                t("travel.homeEquivalent"),
                fmt(spentHome, s.cur),
                t("travel.homeCurrencyIs", "home" to s.cur),
            )
            TravelStat(
                t("hero.today"),
                fmt(todayHome, s.cur),
                if (s.travelRate > 0) "1 $code ≈ ${fmt(s.travelRate, s.cur)}" else "—"
            )
        }
    }
}

@Composable
private fun TravelStat(label: String, value: String, note: String) {
    Column {
        Text(label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
        Text(
            note,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The trip's entries, newest first, each showing its own currency and the home equivalent. */
@Composable
fun TravelListCard(vm: LedgerViewModel, s: LedgerState) {
    val cs = MaterialTheme.colorScheme
    val code = s.travel.currency.ifEmpty { "USD" }
    val totals = travelCurrencyTotals(s.travelList, code)

    CardContainer(
        title = t("travel.title"),
        icon = Icons.Outlined.History,
        count = if (s.travelList.isNotEmpty()) "(${s.travelList.size})" else null,
    ) {
        if (s.travelList.isEmpty()) {
            EmptyState("✈", t("travel.noSpends"), t("travel.logSpend"))
            return@CardContainer
        }
        if (totals.size > 1) {
            SectionDesc(t("travel.currenciesUsedDesc"))
            totals.forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        row.code, Modifier.width(46.dp), fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        fmt(row.foreign, row.code),
                        Modifier.weight(1f),
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("≈ ${fmt(row.home, s.cur)}", fontSize = 12.sp, color = cs.onSurfaceVariant)
                    Text("${row.count}", fontSize = 11.sp, color = cs.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(Modifier.heightIn(max = 380.dp)) {
            items(s.travelList, key = { it.id }) { e ->
                val cat = s.cats.find { it.id == (e.categories.firstOrNull() ?: e.category) }
                val rowCur = e.currency ?: code
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(cat?.glyph ?: "·", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            cat?.label ?: (e.category ?: "—"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (e.note.isNotEmpty()) {
                            Text(
                                e.note,
                                fontSize = 11.sp,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            fmt(e.amount, s.cur),
                            fontSize = 10.5.sp, color = cs.onSurfaceVariant, fontFamily = FontFamily.Monospace,
                        )
                    }
                    Text(relativeDate(e.date, s.today), fontSize = 10.5.sp, color = cs.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        fmt(e.foreignAmount ?: e.amount, rowCur),
                        fontSize = 12.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { vm.removeExpense(e.id) }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            t("common.delete"),
                            Modifier.size(15.dp),
                            tint = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
