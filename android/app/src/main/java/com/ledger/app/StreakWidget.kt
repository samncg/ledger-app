package com.ledger.app

import android.content.Context
import android.widget.RemoteViews
import com.ledger.app.ui.t

/**
 * Home-screen widget: the current spend streak in days plus whether anything has been
 * logged today — e.g. "\uD83D\uDD25 6 days" over "Logged today".
 */
class StreakWidget : BaseWidget() {

    override suspend fun buildViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.streak_widget)
        attachTap(context, views, R.id.streak_root)

        val snap = loadWidgetSnapshot(context)
        val dark = snap.widgetDark
        val pal = widgetPalette(dark)
        views.setInt(
            R.id.streak_root,
            "setBackgroundResource",
            if (dark) R.drawable.widget_bg else R.drawable.widget_bg_light,
        )
        views.setTextColor(R.id.streak_title, pal.dim)
        views.setTextColor(R.id.streak_value, pal.text)
        val days = snap.spendStreak
        val known = snap.ready && days > 0

        // The fire emoji lives in the widget.streakDays* strings.
        views.setTextViewText(
            R.id.streak_value,
            if (known) {
                if (days == 1) {
                    t("widget.streakDaysOne", "n" to days)
                } else {
                    t("widget.streakDaysMany", "n" to days)
                }
            } else {
                "\u2014"
            },
        )
        views.setTextViewText(
            R.id.streak_note,
            when {
                !snap.ready -> context.getString(R.string.widget_no_data)
                days <= 0 -> t("widget.streakStart")
                snap.loggedToday -> t("widget.streakLoggedToday")
                else -> t("widget.streakNotLogged")
            },
        )
        views.setTextColor(R.id.streak_note, if (snap.loggedToday) POSITIVE else pal.dim)
        return views
    }

    private companion object {
        val POSITIVE = 0xFF5BD488.toInt()
    }
}
