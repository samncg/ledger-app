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

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = repo.load().prefs ?: return@launch

            if (!prefs.notificationsEnabled) {
                NotificationHelper.cancelDailyReminder(context)
                return@launch
            }

            if (action == ACTION_DAILY_REMINDER) {
                NotificationHelper.showDailyReminder(context)
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
        }
    }
}
