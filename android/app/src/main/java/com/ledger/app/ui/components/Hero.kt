package com.ledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.data.DayCell
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.parseColor
import com.ledger.app.util.relativeDate

/* Hero — daily allowance, balance, stats, budget progress and the daily strip */
@Composable
fun Hero(
    s: LedgerState,
    myr: (Double) -> String,
    onMoveMoney: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val accent = parseColor(s.theme.accent) ?: cs.primary
    val positive = parseColor(s.theme.positive) ?: cs.primary
    val warning = parseColor(s.theme.warning) ?: cs.primary
    val negative = parseColor(s.theme.negative) ?: cs.primary

    GlassSurface(
        style = LocalGlassStyle.current,
        baseColor = cs.surface,
        accentColor = cs.primary,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            /* ── Top: label, value, badges, move-money button ── */
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s.heroLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = cs.onSurfaceVariant
                    )
                    Text(
                        myr(s.heroValue),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (s.heroValue < 0) negative else cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    ChipFlow {
                        if (s.balancesOn && s.todaySaved > 0) {
                            Badge(positive) {
                                Icon(Icons.Outlined.Wallet, null, Modifier.size(11.dp))
                                Text("${myr(s.todaySaved)} saved today", fontSize = 11.sp)
                            }
                        }
                        if (s.topUpTotal > 0) {
                            Badge(positive) {
                                Icon(Icons.Outlined.Bolt, null, Modifier.size(11.dp))
                                Text(
                                    "${myr(s.topUpTotal)} ${if (s.balancesOn) "moved to budget" else "topped up"}",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Btn(
                    if (s.balancesOn) "Move money" else "Top up",
                    onClick = onMoveMoney,
                    variant = "secondary",
                    small = true,
                    icon = if (s.balancesOn) Icons.Outlined.Wallet else Icons.Outlined.Bolt
                )
            }

            if (s.todayRemaining < 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Over today's allowance by ${myr(-s.todayRemaining)}",
                    fontSize = 12.5.sp, color = negative, fontWeight = FontWeight.SemiBold,
                )
            }

            /* ── Stats ── */
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox(
                    "Daily allowance",
                    myr(s.dailyBudget),
                    "${myr(s.effectiveMonthlyBudget)} / ${s.settings?.periodDays}d",
                    Modifier.weight(1f)
                )
                if (s.balancesOn) {
                    StatBox(
                        "Saved to balance",
                        myr(s.bankedSoFar),
                        "Leftover allowance banked so far",
                        Modifier.weight(1f),
                        valueColor = if (s.bankedSoFar < 0) negative else positive
                    )
                } else {
                    StatBox(
                        if (s.runningBalance < 0) "Total over" else "Rollover",
                        myr(Math.abs(s.runningBalance)),
                        if (s.runningBalance < 0) "Spent over allowance" else "Unspent allowance carries over",
                        Modifier.weight(1f),
                        valueColor = if (s.runningBalance < 0) negative else positive,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox(
                    "Avg spending / day", myr(s.avgDailySpend),
                    (if (s.avgDailySpend <= s.dailyBudget) "Under allowance" else "Over allowance") + if (s.daysOver > 0) " · ${s.daysOver}d over" else "",
                    Modifier.weight(1f),
                )
                StatBox(
                    "Projected total", myr(s.projectedTotal),
                    if (s.projectedDelta < 0) "${myr(-s.projectedDelta)} over if pace holds" else "${myr(s.projectedDelta)} left if pace holds",
                    Modifier.weight(1f),
                    valueColor = if (s.projectedDelta < 0) negative else cs.onSurface,
                )
            }

            /* ── Budget progress ── */
            Spacer(Modifier.height(14.dp))
            val budgetLeft = s.effectiveMonthlyBudget - s.periodSpent
            val budgetLeftText = if (budgetLeft >= 0) "${myr(budgetLeft)} left" else "${myr(Math.abs(budgetLeft))} over"
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Budget progress",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color = cs.onSurfaceVariant
                )
                Text(
                    "${myr(s.periodSpent)} / ${myr(s.effectiveMonthlyBudget)} · $budgetLeftText",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = cs.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(cs.surfaceVariant)) {
                Box(
                    Modifier.fillMaxWidth(s.budgetPctFull.toFloat() / 100f).height(8.dp)
                        .background(if (s.periodSpent > s.effectiveMonthlyBudget) negative else accent, CircleShape),
                )
            }

            /* ── Daily spend strip ── */
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Daily spend · this period",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color = cs.onSurfaceVariant
                )
                Text("Day ${s.elapsedDays} / ${s.settings?.periodDays}", fontSize = 11.sp, color = cs.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            // Slide-your-finger daily strip — move across the bars to inspect each day's date + spending.
            var selIdx by remember { mutableStateOf(-1) }
            var stripWidthPx by remember { mutableStateOf(0) }
            val barTick = rememberHapticTick()
            val density = LocalDensity.current
            val barSpacingPx = with(density) { 3.dp.toPx() }
            val days = s.dayCells.size
            Row(
                Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(10.dp)).background(cs.surfaceVariant)
                    .padding(8.dp)
                    .onSizeChanged { stripWidthPx = it.width }
                    .pointerInput(days, stripWidthPx, barSpacingPx) {
                        if (days == 0 || stripWidthPx <= 0) return@pointerInput
                        val innerW = stripWidthPx
                        val stride = (innerW - barSpacingPx * (days - 1)) / days + barSpacingPx
                        fun indexAt(x: Float): Int = ((x / stride).toInt()).coerceIn(0, days - 1)
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            selIdx = indexAt(down.position.x)
                            barTick()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val idx = indexAt(change.position.x)
                                if (idx != selIdx) {
                                    selIdx = idx
                                    barTick()
                                }
                                change.consume()
                            }
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                s.dayCells.forEachIndexed { idx, c ->
                    val pct: Double =
                        if (c.isFuture) 8.0 else (c.spent / ((s.dailyBudget.coerceAtLeast(0.0001)) * 1.6) * 100).coerceIn(
                            8.0,
                            100.0
                        )
                    val color = if (c.isFuture) cs.surfaceVariant else when {
                        c.delta > 3 -> positive
                        c.delta >= -s.dailyBudget * 0.4 -> warning
                        else -> negative
                    }
                    val alpha = if (c.isFuture) 0.35f else 1f
                    val selected = idx == selIdx
                    Box(
                        Modifier
                            .weight(1f)
                            .height(((pct / 100) * 48).dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color.copy(alpha = alpha))
                            .let { m ->
                                if (c.isToday || selected) m.border(
                                    1.5.dp,
                                    accent,
                                    RoundedCornerShape(3.dp)
                                ) else m
                            },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val sel = if (selIdx in s.dayCells.indices) s.dayCells[selIdx] else null
            Text(
                if (sel == null) "Slide across the bars to see each day's spending."
                else "${
                    relativeDate(
                        sel.date,
                        s.today
                    )
                } · " + if (sel.isFuture) "no spending yet" else "spent ${myr(sel.spent)}",
                fontSize = 11.sp,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot(positive, "Under")
                LegendDot(warning, "Near")
                LegendDot(negative, "Over")
                LegendDot(accent, "Today")
            }
        }
    }
}

@Composable
private fun Badge(color: Color, content: @Composable () -> Unit) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), contentColor = color) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) { content() }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    note: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceVariant)
            .padding(12.dp),
    ) {
        Text(label, fontSize = 10.5.sp, letterSpacing = 0.4.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = valueColor ?: cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(note, fontSize = 10.sp, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Health badge — ported from the web app's healthBadge(). */
@Composable
fun HealthBadge(s: LedgerState) {
    if (s.settings == null) return
    val positive = parseColor(s.theme.positive) ?: MaterialTheme.colorScheme.primary
    val warning = parseColor(s.theme.warning) ?: MaterialTheme.colorScheme.primary
    val negative = parseColor(s.theme.negative) ?: MaterialTheme.colorScheme.error
    val (color, text) = when {
        s.todayRemaining < 0 -> negative to "● Over today"
        s.dailyBudget > 0 && s.todayRemaining / s.dailyBudget < 0.2 -> warning to "● Near limit"
        else -> positive to "● On track"
    }
    Badge(color) { Text(text, fontSize = 11.sp) }
}
