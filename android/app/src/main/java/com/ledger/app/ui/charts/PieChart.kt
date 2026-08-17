package com.ledger.app.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.PieSlice
import com.ledger.app.ui.parseColor
import kotlin.math.max

/* Donut chart — ported from the web app's SVG circles (dash = pct − gap) */
@Composable
fun PieChart(
    slices: List<PieSlice>,
    thickness: Float,
    gap: Float,
    centerValue: String,
    centerSub: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(modifier.size(140.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(140.dp)) {
            val stroke = Stroke(width = thickness.dp.toPx(), cap = StrokeCap.Butt)
            drawArc(
                color = if (slices.isEmpty()) cs.outlineVariant else cs.outline,
                startAngle = 0f, sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            // SVG arcs run clockwise from 12 o'clock; Canvas angles run from 3 o'clock.
            slices.forEach { slice ->
                val color = parseColor(slice.color) ?: cs.primary
                val gapAngle = gap * 0.3f
                val sweep = max(0.1f, slice.dash.toFloat() - gapAngle)
                val startAngle = -90f + (slice.offset.toFloat() + gapAngle / 2f)
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = stroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerValue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
            Text(centerSub, fontSize = 10.sp, color = cs.onSurfaceVariant)
        }
    }
}

/* Per-category bar row (label · track · value) */
@Composable
fun CatBarRow(
    label: String,
    color: String?,
    value: String,
    fillPct: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    valueTrailing: @Composable (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val dot = parseColor(color) ?: cs.primary
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(dot, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(label, fontSize = 12.5.sp)
            }
            Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            valueTrailing?.invoke()
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(cs.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fillPct.coerceIn(0f, 1f))
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(fillColor),
            )
        }
    }
}

/* Legend row for charts */
@Composable
fun ChartLegend(items: List<Pair<String, String>>, extra: @Composable (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.padding(top = 10.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)
    ) {
        items.forEach { (color, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp)
            ) {
                Box(Modifier.size(8.dp).background(parseColor(color) ?: cs.primary, CircleShape))
                Text(label, fontSize = 11.sp, color = cs.onSurfaceVariant)
            }
        }
        extra?.invoke()
    }
}
