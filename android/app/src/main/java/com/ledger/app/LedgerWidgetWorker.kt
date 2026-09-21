package com.ledger.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Refreshes the home-screen widgets on a 15-minute cadence.
 *
 * `AppWidgetProviderInfo.updatePeriodMillis` is clamped to a 30-minute minimum by the
 * platform, so a periodic WorkManager job (whose minimum is 15 minutes) is used for the
 * 15-minute cadence instead.
 */
class LedgerWidgetWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        LedgerWidget.refresh(applicationContext)
        WidgetRefresher.refreshNew(applicationContext)
        return Result.success()
    }
}
