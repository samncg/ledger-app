package com.ledger.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ledger.app.data.Repository
import com.ledger.app.ui.t
import com.ledger.app.util.fmt
import com.ledger.app.util.todayKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Home-screen widget showing today's remaining allowance ("RM x left today").
 * It reads the same DataStore the app uses, refreshes on a 30-minute cadence,
 * and can be poked immediately via [refresh] when the app's data changes.
 */
class LedgerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // onUpdate runs on the main thread — do the DataStore read off it.
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildViews(appContext)
                ids.forEach { manager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun buildViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.ledger_widget)

        // Tap anywhere to open the app.
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tap = PendingIntent.getActivity(
            context,
            0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, tap)

        val data = Repository(context).load()
        /* A widget can render with the app process dead, so the language has to come from
           the stored pref before any t(...) call. */
        com.ledger.app.ui.Strings.setLang(data.prefs?.lang?.takeIf { it.isNotBlank() } ?: "en")

        // Style first, so the backdrop and text colours apply to every branch below.
        val dark = widgetStyleFrom(data.prefs).widgetDark
        val pal = widgetPalette(dark)
        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (dark) R.drawable.widget_bg else R.drawable.widget_bg_light,
        )
        views.setTextColor(R.id.widget_label, pal.dim)
        views.setTextColor(R.id.widget_value, pal.text)
        views.setTextColor(R.id.widget_note, pal.dim)

        val settings = data.settings
        if (settings == null) {
            views.setTextViewText(R.id.widget_label, "Ledger")
            views.setTextViewText(R.id.widget_value, t("widget.setupValue"))
            views.setTextViewText(R.id.widget_note, t("widget.setupNote"))
            return views
        }

        val cur = data.prefs?.currency ?: "MYR"
        val today = todayKey()
        val topUpTotal = data.topUps?.sumOf { it.amount } ?: 0.0
        val dailyBudget = (settings.monthlyBudget + topUpTotal) / settings.periodDays.coerceAtLeast(1)
        val todaySpent = data.expenses.orEmpty().filter { it.date == today }.sumOf { it.amount }
        val remaining = dailyBudget - todaySpent

        views.setTextViewText(R.id.widget_label, t("hero.labelAvailable"))
        views.setTextViewText(R.id.widget_value, fmt(remaining, cur))
        views.setTextViewText(
            R.id.widget_note,
            if (remaining < 0) {
                t("widget.overToday")
            } else {
                t("widget.ofAmount", "amount" to fmt(dailyBudget, cur))
            },
        )
        return views
    }

    companion object {
        private const val WORK_NAME = "ledger-widget-refresh"

        /**
         * Keep the widget fresh on a 15-minute cadence. WorkManager's minimum periodic
         * interval is 15 minutes; KEEP makes repeated calls idempotent.
         */
        fun scheduleRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<LedgerWidgetWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** Ask the system to re-render any placed widgets (called when app data changes). */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, LedgerWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, LedgerWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                },
            )
        }
    }
}
