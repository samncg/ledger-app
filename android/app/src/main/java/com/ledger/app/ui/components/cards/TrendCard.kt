package com.ledger.app.ui.components.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.TrendData
import com.ledger.app.ui.charts.ChartLegend
import com.ledger.app.ui.charts.Heatmap
import com.ledger.app.ui.charts.LineChart
import com.ledger.app.ui.components.CardContainer
import com.ledger.app.ui.components.CatChip
import com.ledger.app.ui.components.ChipFlow
import com.ledger.app.ui.components.RangeTabs
import com.ledger.app.util.fmt

/* Spending trend — line chart or GitHub-style heatmap */
@Composable
fun TrendCard(vm: LedgerViewModel, s: LedgerState) {
    val heatmap = s.prefs.trendStyle == "heatmap"
    val ranges = if (heatmap) listOf(30 to "30d", 90 to "90d", 365 to "1y") else listOf(7 to "7d", 14 to "14d", 30 to "30d")
    var range by remember { mutableStateOf(if (heatmap) 30 else 14) }
    var series by remember { mutableStateOf(listOf("__total__")) }

    /* Keep the range sensible for the chosen style (mirrors the web app). */
    LaunchedEffect(heatmap) {
        if (heatmap && range < 30) range = 30
        if (!heatmap && range > 30) range = 30
    }

    val data: TrendData = remember(s, range, series) { vm.trend(s, range, series) }

    CardContainer(
        title = if (heatmap) "Spending heatmap" else "Spending trend",
        icon = Icons.Outlined.TrendingUp,
        trailing = { RangeTabs(ranges.map { it.first.toString() to it.second }, range.toString()) { range = it.toInt() } },
    ) {
        if (heatmap) {
            Heatmap(data.heat, s.prefs.heatColors, s.today) { fmt(it, s.cur) }
        } else {
            ChipFlow {
                CatChip("Total", s.theme.accent, series.contains("__total__")) { toggle(series, "__total__") { series = it } }
                s.cats.forEach { c ->
                    CatChip(c.label, c.color, series.contains(c.id)) { toggle(series, c.id) { series = it } }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Pick one or more — each category draws its own line.", fontSize = 11.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            LineChart(data, s.today) { fmt(it, s.cur) }
            ChartLegend(
                items = data.series.map { it.color to it.label } +
                    if (s.dailyBudget > 0) listOf(s.theme.warning to "Allowance (${fmt(s.dailyBudget, s.cur)})") else emptyList(),
            )
        }
    }
}

private fun toggle(current: List<String>, id: String, set: (List<String>) -> Unit) {
    set(if (current.contains(id)) current.filter { it != id } else current + id)
}
