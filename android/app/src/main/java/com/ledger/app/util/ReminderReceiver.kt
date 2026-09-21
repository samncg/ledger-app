package com.ledger.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ledger.app.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_REMINDER = "com.ledger.app.ACTION_DAILY_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val repo = Repository(context)
        // Keep the process alive until the DataStore read + notification work finishes,
        // otherwise the system may kill us mid-flight and reminders silently stop.
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = repo.load()
                val prefs = data.prefs ?: return@launch
                /* The reminder fires with the app process possibly dead, so the language has
                   to come from the stored pref before the notification text is built. */
                com.ledger.app.ui.Strings.setLang(prefs.lang)

                if (!prefs.notificationsEnabled) {
                    NotificationHelper.cancelDailyReminder(context)
                    return@launch
                }

                if (action == ACTION_DAILY_REMINDER) {
                    val info = streakInfo(data.expenses.orEmpty().map { it.date }, todayKey(), prefs.streakGrace)
                    NotificationHelper.showDailyReminder(context, info.current, info.graceRisk)
                    // Schedule next occurrence
                    NotificationHelper.scheduleDailyReminder(
                        context,
                        prefs.reminderHour,
                        prefs.reminderMinute
                    )
                } else if (action == Intent.ACTION_BOOT_COMPLETED) {
                    NotificationHelper.scheduleDailyReminder(
                        context,
                        prefs.reminderHour,
                        prefs.reminderMinute
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}
