package com.ledger.app.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.HeatData
import com.ledger.app.ui.parseColor
import com.ledger.app.util.relativeDate
import kotlin.math.floor
import kotlin.math.min

/* GitHub-style spending heatmap — weeks as columns, Mon–Sun rows */
@Composable
fun Heatmap(
    data: HeatData,
    colors: Map<String, String>,
    today: String,
    modifier: Modifier = Modifier,
    myr: (Double) -> String,
) {
    val cs = MaterialTheme.colorScheme
    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val weeks = data.weeks.coerceAtLeast(1)
        val gap = 3.dp
        val isScrollable = weeks > 14
        val dayLabelWidth = 26.dp
        val availableGridWidth = (maxWidth - dayLabelWidth - gap)
        val cell = if (isScrollable) {
            12.dp
        } else {
            ((availableGridWidth - gap * (weeks - 1)) / weeks).coerceIn(11.dp, 24.dp)
        }

        LaunchedEffect(weeks) {
            if (isScrollable) {
                scrollState.scrollTo(scrollState.maxValue)
            }
        }

        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                /* day labels */
                Column(
                    Modifier
                        .width(dayLabelWidth)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    weekDays.forEach { day ->
                        Box(Modifier.height(cell), contentAlignment = Alignment.CenterStart) {
                            Text(
                                day,
                                fontSize = 9.sp,
                                lineHeight = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                }
                /* grid */
                val gridModifier = if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier
                Row(gridModifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
                    (0 until weeks).forEach { col ->
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            (0 until 7).forEach { row ->
                                val idx = col * 7 + row
                                val cellData = data.cells.getOrNull(idx)
                                if (cellData == null) {
                                    Box(
                                        Modifier.size(cell).clip(RoundedCornerShape(3.dp)).background(Color.Transparent)
                                    )
                                } else {
                                    val bg = when {
                                        cellData.level == 0 ->
                                            if ((colors["l0"]
                                                    ?: "transparent") != "transparent"
                                            ) parseColor(colors["l0"])
                                            else null

                                        else -> parseColor(colors["l${cellData.level}"])
                                    } ?: Color.Transparent
                                    Box(
                                        Modifier.size(cell).clip(RoundedCornerShape(3.dp)).background(bg),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            /* legend */
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Less", fontSize = 10.sp, color = cs.onSurfaceVariant)
                listOf("l0", "l1", "l2", "l3", "l4").forEach { k ->
                    val bg = if (k == "l0" && (colors["l0"] ?: "transparent") == "transparent") Color.Transparent
                    else parseColor(colors[k]) ?: Color.Transparent
                    Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(bg))
                }
                Text("More", fontSize = 10.sp, color = cs.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(
                    "${myr(data.total)} spent",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = cs.onSurfaceVariant
                )
            }
        }
    }
}
