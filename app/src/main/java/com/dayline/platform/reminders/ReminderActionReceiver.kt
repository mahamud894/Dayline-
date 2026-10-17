package com.dayline.platform.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dayline.DaylineApp
import com.dayline.platform.notifications.DaylineNotificationFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(DaylineNotificationFactory.EXTRA_TASK_ID)
        val action = intent.action?.let(::actionFor)
        if (taskId.isNullOrBlank() || action == null) {
            Log.e(TAG, "Ignoring reminder action with a missing task id or unknown action.")
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as DaylineApp).container
                container.reminderActionHandler.handle(taskId, action)
            } catch (exception: Exception) {
                Log.e(TAG, "Unable to apply reminder action for task $taskId.", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun actionFor(action: String): ReminderAction? = when (action) {
        DaylineNotificationFactory.ACTION_DONE -> ReminderAction.DONE
        DaylineNotificationFactory.ACTION_SNOOZE -> ReminderAction.SNOOZE
        DaylineNotificationFactory.ACTION_DISMISS -> ReminderAction.DISMISS
        else -> null
    }

    companion object {
        private const val TAG = "DaylineReminderAction"
    }
}
