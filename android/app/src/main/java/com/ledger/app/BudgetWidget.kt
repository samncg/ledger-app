package com.ledger.app

import android.content.Context
import android.widget.RemoteViews
import com.ledger.app.ui.t
import com.ledger.app.util.fmt

/**
 * Home-screen widget: this period's spend against the monthly budget, as a horizontal
 * progress bar plus "RM x" / "of RM y · n% used". RemoteViews can't swap a ProgressBar's
 * drawable, so the layout carries one bar per fill — dark, light and the over-budget red —
 * and exactly one of them is shown.
 */
class BudgetWidget : BaseWidget() {

    override suspend fun buildViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.budget_widget)
        attachTap(context, views, R.id.budget_root)

        val snap = loadWidgetSnapshot(context)
        val dark = snap.widgetDark
        val pal = widgetPalette(dark)
        views.setInt(
            R.id.budget_root,
            "setBackgroundResource",
            if (dark) R.drawable.widget_bg else R.drawable.widget_bg_light,
        )
        views.setTextColor(R.id.budget_title, pal.dim)
        views.setTextColor(R.id.budget_value, pal.text)
        val bars = listOf(R.id.budget_bar, R.id.budget_bar_light, R.id.budget_bar_over)

        if (!snap.ready || snap.effectiveMonthlyBudget <= 0.0) {
            views.setTextViewText(R.id.budget_value, "\u2014")
            views.setTextViewText(
                R.id.budget_note,
                if (!snap.ready) {
                    context.getString(R.string.widget_no_data)
                } else {
                    context.getString(R.string.widget_no_budget)
                },
            )
            views.setTextColor(R.id.budget_note, pal.dim)
            showOnlyBar(views, bars, shown = null, progress = 0)
            return views
        }

        val pct = snap.budgetPct
        val over = pct > 100.0
        val progress = pct.coerceIn(0.0, 100.0).let { Math.round(it).toInt() }

        views.setTextViewText(R.id.budget_value, fmt(snap.periodSpent, snap.cur))
        views.setTextViewText(
            R.id.budget_note,
            t(
                "widget.budgetDetail",
                "amount" to fmt(snap.effectiveMonthlyBudget, snap.cur),
                "n" to Math.round(pct),
            ),
        )
        views.setTextColor(R.id.budget_note, if (over) NEGATIVE else pal.dim)
        showOnlyBar(
            views,
            bars,
            shown = when {
                over -> R.id.budget_bar_over
                dark -> R.id.budget_bar
                else -> R.id.budget_bar_light
            },
            progress = progress,
        )
        return views
    }

    private companion object {
        val NEGATIVE = 0xFFFF5C5C.toInt()
    }
}
