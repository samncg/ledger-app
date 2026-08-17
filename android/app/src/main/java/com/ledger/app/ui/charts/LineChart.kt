package com.ledger.app.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.TrendData
import com.ledger.app.ui.TrendDay
import com.ledger.app.ui.parseColor
import com.ledger.app.util.relativeDate
import kotlin.math.min

/* Line chart — grid, allowance baseline, per-series lines, tap-to-inspect tooltip */
@Composable
fun LineChart(
    data: TrendData,
    today: String,
    modifier: Modifier = Modifier,
    myr: (Double) -> String,
) {
    val cs = MaterialTheme.colorScheme
    var hovered by remember { mutableStateOf<TrendDay?>(null) }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(data.days) {
                detectTapGestures { offset ->
                    if (data.days.isEmpty()) return@detectTapGestures
                    val idx = ((offset.x / size.width) * data.days.size).toInt().coerceIn(0, data.days.size - 1)
                    hovered = data.days[idx]
                }
            },
    ) {
        val w = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height
            if (data.days.isEmpty()) return@Canvas

            /* gridlines */
            val grid = cs.outlineVariant.copy(alpha = 0.6f)
            for (f in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                val y = 20f + f * (ch - 40f)
                drawLine(grid, Offset(0f, y), Offset(cw, y), strokeWidth = 1f)
            }

            fun yFor(value: Double): Float = 20f + (1f - min(1f, (value / data.max).toFloat())) * (ch - 40f)
            val step = cw / data.days.size

            /* allowance baseline */
            if (data.budget > 0) {
                val budgetY = yFor(min(data.budget, data.max))
                drawLine(
                    cs.outlineVariant, Offset(0f, budgetY), Offset(cw, budgetY),
                    strokeWidth = 1.5f,
                )
            }

            /* series */
            data.series.forEach { series ->
                val color = parseColor(series.color) ?: cs.primary
                val pts = data.days.mapIndexed { i, d -> Offset(i * step + step / 2, yFor(series.value(d))) }
                if (series.id == "__total__") {
                    val area = Path().apply {
                        moveTo(pts.first().x, ch - 20f)
                        pts.forEach { p -> lineTo(p.x, p.y) }
                        lineTo(pts.last().x, ch - 20f)
                        close()
                    }
                    drawPath(area, color.copy(alpha = 0.18f))
                    drawPath(
                        Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            pts.forEach { p -> lineTo(p.x, p.y) }
                        },
                        color,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                } else {
                    drawPath(
                        Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            pts.forEach { p -> lineTo(p.x, p.y) }
                        },
                        color,
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
        }

        /* tooltip — centered on the tapped day, clamped to the chart */
        hovered?.let { day ->
            val idx = data.days.indexOf(day).coerceAtLeast(0)
            val fraction = if (data.days.size > 1) (idx + 0.5f) / data.days.size else 0.5f
            Column(
                Modifier
                    .padding(top = 4.dp)
                    .offset(x = (w * fraction - 90.dp).coerceIn(0.dp, (w - 180.dp).coerceAtLeast(0.dp)))
                    .clip(RoundedCornerShape(8.dp))
                    .background(cs.surface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(relativeDate(day.date, today), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                data.series.forEach { series ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(7.dp).background(parseColor(series.color) ?: cs.primary, CircleShape))
                        Text(
                            "${series.label}: ${myr(series.value(day))}",
                            fontSize = 10.5.sp,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
