package com.ledger.app.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.BreakdownData
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.charts.CatBarRow
import com.ledger.app.ui.charts.PieChart
import com.ledger.app.ui.components.AppTextField
import com.ledger.app.ui.components.Btn
import com.ledger.app.ui.components.CardContainer
import com.ledger.app.ui.components.DateField
import com.ledger.app.ui.components.FieldLabel
import com.ledger.app.ui.components.MonthSelector
import com.ledger.app.ui.components.RangeTabs
import com.ledger.app.ui.components.StatDivider
import com.ledger.app.ui.parseColor
import com.ledger.app.util.monthEndKey
import com.ledger.app.util.monthLabel

/* Category breakdown — range tabs, donut, per-category bars + budgets */
@Composable
fun BreakdownCard(vm: LedgerViewModel, s: LedgerState) {
    val cs = MaterialTheme.colorScheme
    val ranges = listOf("period" to "Period", "week" to "7d", "month" to "Month", "all" to "All", "custom" to "Custom")
    var range by remember { mutableStateOf("period") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var editingBudgets by remember { mutableStateOf(false) }
    var drafts by remember { mutableStateOf(s.catBudgets.mapValues { (_, v) -> v.toString() }) }
    var monthOffset by remember { mutableIntStateOf(0) }

    // Browsing a previous month shows that whole month; the current month keeps the range tabs.
    val browsingPast = monthOffset > 0
    val end = monthEndKey(s.today, monthOffset)
    val effRange = if (browsingPast) "month" else range
    val data: BreakdownData = remember(s, effRange, from, to, end) { vm.breakdown(s, effRange, from, to, end) }
    val positive = parseColor(s.theme.positive) ?: cs.primary
    val warning = parseColor(s.theme.warning) ?: cs.primary
    val negative = parseColor(s.theme.negative) ?: cs.primary

    CardContainer(
        title = "Category breakdown",
        icon = Icons.Outlined.PieChart,
        trailing = {
            if (editingBudgets) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LinkText("Save") {
                        val next = drafts.mapNotNull { (id, v) ->
                            val n = v.toDoubleOrNull()
                            if (n != null && n > 0) id to n else null
                        }.toMap()
                        vm.saveCatBudgets(next)
                        editingBudgets = false
                    }
                    LinkText("Cancel") { editingBudgets = false }
                }
            } else {
                LinkText("Budgets") {
                    drafts = s.catBudgets.mapValues { (_, v) -> v.toString() }; editingBudgets = true
                }
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonthSelector(base = s.today, monthOffset = monthOffset) { monthOffset = it }
            if (browsingPast) {
                Text("This month", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (browsingPast) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Showing ${monthLabel(s.today, monthOffset)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(Modifier.height(10.dp))
            RangeTabs(ranges, range) { range = it }
        }

        if (!browsingPast && range == "custom") {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("From")
                    DateField(value = from, onChange = { from = it }, maxDate = to.ifEmpty { s.today }, placeholder = "Start date")
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("To")
                    DateField(value = to, onChange = { to = it }, maxDate = s.today, placeholder = "Max date")
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TotalsBox("Total spent", fmtD(s, data.totalSpent), data.rangeLabel, Modifier.weight(1f))
            StatDivider(vertical = true, modifier = Modifier.padding(vertical = 2.dp))
            val pctColor = when {
                data.budgetPct >= 100 -> negative
                data.budgetPct >= 75 -> warning
                else -> cs.onSurface
            }
            TotalsBox(
                "% of allowance", "${String.format("%.1f", data.budgetPct)}%",
                "vs ${fmtD(s, data.rangeBudget)} · ${data.rangeDays}d",
                Modifier.weight(1f),
                valueColor = pctColor,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Insight("Avg spending/day", fmtD(s, data.avgPerDay), Modifier.weight(1.3f))
            StatDivider(vertical = true, modifier = Modifier.padding(vertical = 2.dp))
            Insight("Txns", data.txnCount.toString(), Modifier.weight(0.8f))
            StatDivider(vertical = true, modifier = Modifier.padding(vertical = 2.dp))
            Insight("Biggest", data.biggestInRange?.let { fmtD(s, it.amount) } ?: "—", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PieChart(
                slices = data.pieSlices,
                thickness = s.prefs.pieThickness,
                gap = s.prefs.pieGap,
                centerValue = fmtD(s, data.totalSpent),
                centerSub = if (data.txnCount == 0) "no spending" else "spent",
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                if (data.pieSlices.isEmpty()) {
                    s.cats.forEach { c ->
                        PieLegendRow(c.label, c.color, fmtD(s, 0.0))
                    }
                } else {
                    data.pieSlices.forEach { slice ->
                        PieLegendRow(slice.label, slice.color, fmtD(s, slice.value))
                    }
                }
            }
        }

        if (editingBudgets) {
            Spacer(Modifier.height(14.dp))
            s.cats.forEach { c ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(c.label, Modifier.weight(1f), fontSize = 12.5.sp)
                    AppTextField(
                        value = drafts[c.id] ?: "",
                        onChange = { drafts = drafts + (c.id to it) },
                        modifier = Modifier.width(110.dp), mono = true, numeric = true,
                        placeholder = "No limit",
                        onDone = {
                            vm.saveCatBudgets(drafts.mapNotNull { (id, v) ->
                                v.toDoubleOrNull()?.takeIf { it > 0 }?.let { id to it }
                            }.toMap()); editingBudgets = false
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        } else {
            Spacer(Modifier.height(16.dp))
            s.cats.forEach { c ->
                val budget = s.catBudgets[c.id]
                val total = data.categoryTotals[c.id] ?: 0.0
                val over = budget != null && budget > 0 && total > budget
                val fillPct = if (budget != null && budget > 0) (total / budget * 100).toFloat().coerceIn(0f, 100f)
                else (total / data.maxCategory * 100).toFloat().coerceIn(0f, 100f)
                val fillColor = if (over) negative else parseColor(c.color) ?: cs.primary
                CatBarRow(
                    label = c.label,
                    color = c.color,
                    value = fmtD(s, total),
                    fillPct = fillPct / 100f,
                    fillColor = fillColor,
                    valueTrailing = if (budget != null && budget > 0) {
                        {
                            Text(
                                " / ${fmtD(s, budget)}",
                                fontSize = 10.5.sp,
                                color = if (over) negative else cs.onSurfaceVariant,
                            )
                        }
                    } else null,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun PieLegendRow(label: String, color: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(parseColor(color) ?: cs.primary),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            Modifier.weight(1f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(value, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun TotalsBox(
    label: String,
    valueText: String,
    note: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color? = null,
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier) {
        Text(label, fontSize = 10.5.sp, letterSpacing = 0.4.sp, color = cs.onSurfaceVariant)
        Text(
            valueText,
            fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            color = valueColor ?: cs.onSurface,
        )
        Text(note, fontSize = 10.sp, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun Insight(label: String, value: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier) {
        Text(label, fontSize = 10.5.sp, color = cs.onSurfaceVariant)
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

private fun fmtD(s: LedgerState, v: Double): String = com.ledger.app.util.fmt(v, s.cur)
