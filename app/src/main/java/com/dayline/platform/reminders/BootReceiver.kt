package com.dayline.platform.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dayline.DaylineApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as DaylineApp).container
                container.reminderScheduler.restorePending()
                val settings = container.userSettingsStore.settings.first()
                container.morningSummaryScheduler.schedule(settings)
            } catch (exception: Exception) {
                Log.e(TAG, "Unable to restore Dayline schedules after boot.", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "DaylineBoot"
    }
}
