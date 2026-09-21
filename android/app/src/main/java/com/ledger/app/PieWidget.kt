package com.ledger.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import com.ledger.app.ui.t
import com.ledger.app.util.fmt
import kotlin.math.min

/**
 * Home-screen widget: this period's spending split by category, drawn as a donut with a
 * legend (top categories, any tail folded into "Other"). The ring is rasterised with
 * [Canvas] and handed to the launcher via `setImageViewBitmap`.
 */
class PieWidget : BaseWidget() {

    override suspend fun buildViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.pie_widget)
        attachTap(context, views, R.id.pie_root)

        val snap = loadWidgetSnapshot(context)
        val dark = snap.widgetDark
        val pal = widgetPalette(dark)
        val slices = snap.periodSlices(MAX_SLICES)
        views.setInt(
            R.id.pie_root,
            "setBackgroundResource",
            if (dark) R.drawable.widget_bg else R.drawable.widget_bg_light,
        )
        views.setImageViewBitmap(
            R.id.pie_chart,
            renderPie(context, slices, snap.pieThickness, snap.pieGap, pal),
        )
        views.setTextColor(R.id.pie_title, pal.dim)
        views.setTextColor(R.id.pie_note, pal.dim)
        views.setTextViewText(
            R.id.pie_note,
            when {
                !snap.ready -> context.getString(R.string.widget_no_data)
                slices.isEmpty() -> t("widget.pieEmpty")
                else -> t("widget.periodSpent", "amount" to fmt(snap.periodSpent, snap.cur))
            },
        )
        return views
    }

    private companion object {
        const val MAX_SLICES = 4
    }
}

/**
 * Draw the donut + legend into a bitmap. Sized in dp and capped at 2x density so the
 * payload handed across the binder stays small (~340 KB rather than megabytes).
 * [pal] colours the legend labels, the dim percentages and the empty ring track.
 */
private fun renderPie(
    context: Context,
    slices: List<WidgetSlice>,
    thickness: Float,
    gap: Float,
    pal: WidgetPalette,
): Bitmap {
    val dm = context.resources.displayMetrics
    val density = dm.density.coerceAtMost(2f)
    fun dp(v: Float) = v * density

    val w = dp(220f).toInt().coerceAtLeast(1)
    val h = dp(96f).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val pad = dp(2f)
    val ringSize = min(h - pad * 2f, w * 0.42f).coerceAtLeast(1f)
    val ring = RectF(pad, (h - ringSize) / 2f, pad + ringSize, (h + ringSize) / 2f)

    val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        // coerceAtLeast/AtMost rather than coerceIn: the ring can be smaller than the
        // minimum stroke on a pathological density, and coerceIn would throw on that.
        strokeWidth = (thickness * 2.6f).let { dp(it) }.coerceAtLeast(dp(6f)).coerceAtMost(ringSize / 2.6f)
    }

    // Empty track, so an empty period still reads as a (blank) ring rather than nothing.
    arc.color = withAlpha(pal.accent, 0x1F)
    canvas.drawArc(ring, 0f, 360f, false, arc)

    val totalPct = slices.sumOf { it.pct }
    if (totalPct > 0.0) {
        // Slices run clockwise from 12 o'clock, matching ui/charts/PieChart.kt.
        val gapDeg = if (slices.size > 1) gap * 1.5f else 0f
        var start = -90f
        slices.forEach { slice ->
            val sweep = (slice.pct / totalPct * 360.0).toFloat()
            arc.color = slice.color
            canvas.drawArc(ring, start + gapDeg / 2f, (sweep - gapDeg).coerceAtLeast(0.5f), false, arc)
            start += sweep
        }
    }

    if (slices.isNotEmpty()) {
        drawLegend(
            canvas,
            slices,
            ring.right + dp(10f),
            w - pad,
            h.toFloat(),
            density,
            dm.scaledDensity,
            pal.text,
            pal.dim
        )
    }
    return bitmap
}

private fun drawLegend(
    canvas: Canvas,
    slices: List<WidgetSlice>,
    left: Float,
    right: Float,
    height: Float,
    density: Float,
    scaledDensity: Float,
    labelColor: Int,
    pctColor: Int,
) {
    fun dp(v: Float) = v * density

    // Honour the user's font scale, but not without bound — the legend has to fit.
    val textScale = scaledDensity.coerceAtLeast(density).coerceAtMost(density * 1.4f)
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelColor
        textSize = 11f * textScale
        isSubpixelText = true
    }
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pctColor
        textSize = 10f * textScale
        textAlign = Paint.Align.RIGHT
        isSubpixelText = true
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    val dotR = dp(3.5f)
    val rowH = dp(15f)
    val metrics = labelPaint.fontMetrics
    var centerY = height / 2f - slices.size * rowH / 2f + rowH / 2f

    slices.forEach { slice ->
        val baseline = centerY - (metrics.ascent + metrics.descent) / 2f
        dotPaint.color = slice.color
        canvas.drawCircle(left + dotR, centerY, dotR, dotPaint)

        val labelX = left + dotR * 2f + dp(6f)
        val pct = "${Math.round(slice.pct)}%"
        val labelWidth = (right - pctPaint.measureText(pct) - dp(6f)) - labelX
        canvas.drawText(ellipsize(slice.label, labelPaint, labelWidth), labelX, baseline, labelPaint)
        canvas.drawText(pct, right, baseline, pctPaint)
        centerY += rowH
    }
}

/** Trim [text] to [maxWidth], ending on an ellipsis when it doesn't fit. */
private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
    if (maxWidth <= 0f) return ""
    if (paint.measureText(text) <= maxWidth) return text
    var end = text.length
    while (end > 0 && paint.measureText(text.substring(0, end) + "\u2026") > maxWidth) end--
    return if (end <= 0) "" else text.substring(0, end) + "\u2026"
}
