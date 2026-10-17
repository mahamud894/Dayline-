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
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_TIMEZONE_CHANGED &&
            intent.action != Intent.ACTION_TIME_CHANGED
        ) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as DaylineApp).container
                try {
                    container.reminderScheduler.restorePending()
                } catch (exception: Exception) {
                    Log.e(TAG, "Unable to restore deadline reminders after a system time change.", exception)
                }
                try {
                    val settings = container.userSettingsStore.settings.first()
                    container.morningSummaryScheduler.schedule(settings)
                } catch (exception: Exception) {
                    Log.e(TAG, "Unable to reschedule the morning summary after a system time change.", exception)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Unable to access app services after a system time change.", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "DaylineBoot"
    }
}
