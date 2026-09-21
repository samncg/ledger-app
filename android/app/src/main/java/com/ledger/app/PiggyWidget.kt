package com.ledger.app

import android.content.Context
import android.widget.RemoteViews
import com.ledger.app.ui.t
import com.ledger.app.util.fmt

/**
 * Home-screen widget: the active piggy bank's saved vs. target amount, with a progress
 * bar. The bar hides when no goal is set (nothing to measure against); the layout carries
 * a dark and a light fill and the theme picks between them.
 */
class PiggyWidget : BaseWidget() {

    override suspend fun buildViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.piggy_widget)
        attachTap(context, views, R.id.piggy_root)

        val snap = loadWidgetSnapshot(context)
        val dark = snap.widgetDark
        val pal = widgetPalette(dark)
        views.setInt(
            R.id.piggy_root,
            "setBackgroundResource",
            if (dark) R.drawable.widget_bg else R.drawable.widget_bg_light,
        )
        views.setTextColor(R.id.piggy_title, pal.dim)
        views.setTextColor(R.id.piggy_value, pal.text)
        views.setTextColor(R.id.piggy_note, pal.dim)
        views.setTextViewText(R.id.piggy_title, snap.piggyName)

        val bars = listOf(R.id.piggy_bar, R.id.piggy_bar_light)
        val shown = if (dark) R.id.piggy_bar else R.id.piggy_bar_light

        if (!snap.ready) {
            views.setTextViewText(R.id.piggy_value, "\u2014")
            views.setTextViewText(R.id.piggy_note, context.getString(R.string.widget_no_data))
            showOnlyBar(views, bars, shown = null, progress = 0)
            return views
        }

        views.setTextViewText(R.id.piggy_value, fmt(snap.piggySaved, snap.cur))
        val hasGoal = snap.piggyTarget > 0.0
        views.setTextViewText(
            R.id.piggy_note,
            if (hasGoal) {
                t(
                    "widget.piggyDetail",
                    "amount" to fmt(snap.piggyTarget, snap.cur),
                    "n" to Math.round(snap.piggyPct),
                )
            } else {
                t("widget.noGoal")
            },
        )
        showOnlyBar(
            views,
            bars,
            shown = if (hasGoal) shown else null,
            progress = Math.round(snap.piggyPct).toInt(),
        )
        return views
    }
}
