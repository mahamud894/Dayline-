package com.dayline.platform.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dayline.MainActivity
import com.dayline.domain.model.Task
import com.dayline.domain.reminders.ReminderKind
import com.dayline.platform.reminders.ReminderAction
import com.dayline.platform.reminders.ReminderActionReceiver
import com.dayline.platform.reminders.TaskNotificationController

class DaylineNotificationFactory(private val context: Context) : TaskNotificationController {
    private val notificationManager =
        checkNotNull(ContextCompat.getSystemService(context, NotificationManager::class.java)) {
            "Android notification service is unavailable."
        }
    private val acknowledgmentStore =
        context.getSharedPreferences(ACKNOWLEDGMENT_STORE, Context.MODE_PRIVATE)

    fun createChannels() {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_STANDARD,
                "Task reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannel(
                CHANNEL_URGENT,
                "Urgent task reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                setVibrationPattern(longArrayOf(0, 350, 250, 350))
            },
            NotificationChannel(
                CHANNEL_URGENT_DND,
                "Urgent reminders allowed during Do Not Disturb",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    setBypassDnd(true)
                }
            },
            NotificationChannel(
                CHANNEL_OVERDUE,
                "Overdue tasks",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        notificationManager.createNotificationChannels(channels)
    }

    override fun showReminder(task: Task, kind: ReminderKind) {
        ensureNotificationsEnabled()
        val channelId = when (kind) {
            ReminderKind.TWO_HOURS -> CHANNEL_STANDARD
            else -> if (
                task.quietModeOverrideAllowed &&
                notificationManager.isNotificationPolicyAccessGranted
            ) {
                CHANNEL_URGENT_DND
            } else {
                CHANNEL_URGENT
            }
        }
        val title = when (kind) {
            ReminderKind.TWO_HOURS -> "Coming up in two hours"
            ReminderKind.FINAL_THIRTY_MINUTES -> "Deadline approaching"
            ReminderKind.SNOOZE -> "Snoozed reminder"
            ReminderKind.OVERDUE -> "Deadline passed"
        }
        notificationManager.notify(
            task.id,
            TASK_NOTIFICATION_ID,
            buildTaskNotification(task, title, channelId, ongoing = false)
        )
    }

    override fun showOverdue(task: Task) {
        if (acknowledgmentStore.getBoolean(acknowledgmentKey(task.id), false)) return
        ensureNotificationsEnabled()
        notificationManager.notify(
            task.id,
            TASK_NOTIFICATION_ID,
            buildTaskNotification(task, "Overdue task", CHANNEL_OVERDUE, ongoing = true)
        )
    }

    override fun dismissCurrent(taskId: String) {
        notificationManager.cancel(taskId, TASK_NOTIFICATION_ID)
    }

    override fun acknowledgeOverdue(taskId: String) {
        acknowledgmentStore.edit().putBoolean(acknowledgmentKey(taskId), true).apply()
        dismissCurrent(taskId)
    }

    override fun clearOverdueAcknowledgment(taskId: String) {
        acknowledgmentStore.edit().remove(acknowledgmentKey(taskId)).apply()
    }

    override fun clearTask(taskId: String) {
        dismissCurrent(taskId)
        clearOverdueAcknowledgment(taskId)
    }

    private fun buildTaskNotification(
        task: Task,
        title: String,
        channelId: String,
        ongoing: Boolean
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            stableRequestCode(task.id, "open"),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(task.title)
            .setStyle(Notification.BigTextStyle().bigText(task.title))
            .setContentIntent(contentIntent)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(ongoing)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(action(task.id, ReminderAction.DONE, "Done"))
            .addAction(action(task.id, ReminderAction.SNOOZE, "Snooze"))
            .addAction(action(task.id, ReminderAction.DISMISS, "Dismiss"))
        return builder.build()
    }

    private fun action(taskId: String, action: ReminderAction, label: String): Notification.Action {
        val intentAction = when (action) {
            ReminderAction.DONE -> ACTION_DONE
            ReminderAction.SNOOZE -> ACTION_SNOOZE
            ReminderAction.DISMISS -> ACTION_DISMISS
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stableRequestCode(taskId, intentAction),
            Intent(context, ReminderActionReceiver::class.java).apply {
                this.action = intentAction
                data = Uri.parse("dayline://task/$taskId/action/$intentAction")
                putExtra(EXTRA_TASK_ID, taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Action.Builder(null, label, pendingIntent).build()
    }

    private fun ensureNotificationsEnabled() {
        check(NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            "Dayline notifications are disabled; enable them in Android settings."
        }
    }

    private fun stableRequestCode(taskId: String, action: String) =
        "$taskId:$action".hashCode()

    companion object {
        const val ACTION_DONE = "com.dayline.action.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.dayline.action.REMINDER_SNOOZE"
        const val ACTION_DISMISS = "com.dayline.action.REMINDER_DISMISS"
        const val EXTRA_TASK_ID = "com.dayline.extra.TASK_ID"
        const val TASK_NOTIFICATION_ID = 1
        const val CHANNEL_STANDARD = "dayline_task_standard"
        const val CHANNEL_URGENT = "dayline_task_urgent"
        const val CHANNEL_URGENT_DND = "dayline_task_urgent_dnd"
        const val CHANNEL_OVERDUE = "dayline_task_overdue"
        private const val ACKNOWLEDGMENT_STORE = "dayline_overdue_acknowledgments"

        private fun acknowledgmentKey(taskId: String) = "task:$taskId"
    }
}
