package com.dayline.platform.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dayline.DaylineApp
import com.dayline.domain.reminders.ReminderKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidReminderScheduler.ACTION_DELIVER_REMINDER) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
        val kindName = intent.getStringExtra(EXTRA_KIND)
        val triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, Long.MIN_VALUE)
        if (taskId.isNullOrBlank() || kindName.isNullOrBlank() || triggerAt == Long.MIN_VALUE) {
            Log.e(TAG, "Ignoring reminder broadcast with missing task, kind, or trigger.")
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val kind = ReminderKind.valueOf(kindName)
                (context.applicationContext as DaylineApp).container.reminderDeliveryHandler
                    .deliver(taskId, kind, triggerAt)
            } catch (exception: Exception) {
                Log.e(TAG, "Unable to deliver reminder for task $taskId.", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "com.dayline.extra.REMINDER_TASK_ID"
        const val EXTRA_KIND = "com.dayline.extra.REMINDER_KIND"
        const val EXTRA_TRIGGER_AT = "com.dayline.extra.REMINDER_TRIGGER_AT"
        private const val TAG = "DaylineReminder"
    }
}
