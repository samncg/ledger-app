package com.ledger.app.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.CardContainer
import com.ledger.app.ui.components.EmptyState
import com.ledger.app.ui.components.innerSurfaceColor
import com.ledger.app.ui.parseColor
import com.ledger.app.ui.t
import com.ledger.app.util.fmt
import com.ledger.app.util.relativeDate
import kotlin.math.abs

/* Monthly insights — month-over-month comparison, pace and best/worst days */
@Composable
fun InsightsCard(vm: LedgerViewModel, s: LedgerState) {
    val cs = MaterialTheme.colorScheme
    val data = remember(s) { vm.insights(s) }
    val positive = parseColor(s.theme.positive) ?: cs.primary
    val negative = parseColor(s.theme.negative) ?: cs.error
    val warning = parseColor(s.theme.warning) ?: cs.primary

    CardContainer(title = t("card.insights.title"), icon = Icons.Outlined.Insights) {
        if (s.expenses.isEmpty() && data.lastMonth == 0.0) {
            EmptyState(
                "◔",
                t("card.insights.emptyTitle"),
                t("card.insights.emptyDesc"),
            )
            return@CardContainer
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(t("card.insights.thisMonth"), fontSize = 11.sp, color = cs.onSurfaceVariant)
                Text(
                    fmt(data.thisMonth, s.cur),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val pct = data.changePct
                val up = (pct ?: 0.0) > 0
                Text(
                    when {
                        pct == null -> "—"
                        pct == 0.0 -> t("card.insights.noChange")
                        else -> "${if (up) "+" else ""}${"%.0f".format(pct)}%"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        pct == null -> cs.onSurfaceVariant
                        up -> negative
                        else -> positive
                    },
                )
                Text(
                    t("card.insights.vsLastMonth", "amount" to fmt(data.lastMonth, s.cur)),
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        InsightRow(
            t("card.insights.avgPerDay"),
            fmt(data.avgPerDay, s.cur),
            t(
                if (data.daysElapsed == 1) "card.insights.daySoFar" else "card.insights.daysSoFar",
                "days" to data.daysElapsed
            ),
        )
        data.biggestCategoryLabel?.let { label ->
            val d = data.biggestCategoryDelta
            InsightRow(
                t("card.insights.biggestChange"),
                label,
                t(
                    "card.insights.deltaVsLastMonth",
                    "sign" to (if (d >= 0) "+" else "-"),
                    "amount" to fmt(abs(d), s.cur)
                ),
                if (d > 0) negative else positive,
            )
        }
        val paceDelta = data.monthlyBudget - data.projected
        InsightRow(
            t("card.insights.projected"),
            fmt(data.projected, s.cur),
            if (data.monthlyBudget > 0) {
                if (paceDelta >= 0) t("card.insights.leftAtPace", "amount" to fmt(paceDelta, s.cur))
                else t("card.insights.overAtPace", "amount" to fmt(-paceDelta, s.cur))
            } else t("card.insights.basedOnPace"),
            if (data.monthlyBudget > 0 && paceDelta < 0) negative else null,
        )

        if (data.bestDay != null || data.worstDay != null) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                data.bestDay?.let { (date, amt) ->
                    DayBox(
                        t("card.insights.bestDay"),
                        relativeDate(date, s.today),
                        fmt(amt, s.cur),
                        positive,
                        Modifier.weight(1f)
                    )
                }
                data.worstDay?.let { (date, amt) ->
                    DayBox(
                        t("card.insights.worstDay"),
                        relativeDate(date, s.today),
                        fmt(amt, s.cur),
                        warning,
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String, note: String, valueColor: Color? = null) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            Text(
                note,
                fontSize = 10.5.sp,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = valueColor ?: cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DayBox(label: String, date: String, amount: String, accent: Color, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(innerSurfaceColor()).padding(12.dp)) {
        Text(label, fontSize = 10.5.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(amount, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = accent)
        Text(date, fontSize = 10.sp, color = cs.onSurfaceVariant)
    }
}
