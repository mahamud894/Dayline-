package com.dayline.platform.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dayline.MainActivity
import com.dayline.R
import com.dayline.platform.alarm.DeadlineAlarmService
import com.dayline.ui.alarm.DeadlineAlarmActivity
import com.dayline.domain.model.Task
import com.dayline.domain.summary.SummaryCategory
import com.dayline.domain.summary.SummaryItem
import com.dayline.domain.reminders.ReminderKind
import com.dayline.platform.reminders.ReminderAction
import com.dayline.platform.reminders.ReminderActionReceiver
import com.dayline.platform.reminders.TaskNotificationController
import com.dayline.platform.summary.SummaryNotificationController
import java.time.LocalDate

class DaylineNotificationFactory(private val context: Context) :
    TaskNotificationController,
    SummaryNotificationController {
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
                context.getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannel(
                CHANNEL_URGENT,
                context.getString(R.string.channel_urgent),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                setVibrationPattern(longArrayOf(0, 350, 250, 350))
            },
            NotificationChannel(
                CHANNEL_URGENT_DND,
                context.getString(R.string.channel_urgent_dnd),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    setBypassDnd(true)
                }
            },
            NotificationChannel(
                CHANNEL_OVERDUE,
                context.getString(R.string.channel_overdue),
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                CHANNEL_SUMMARY,
                context.getString(R.string.channel_summary),
                NotificationManager.IMPORTANCE_LOW
            ),
            // Silent: DeadlineAlarmService plays the looping alarm sound itself.
            NotificationChannel(
                CHANNEL_ALARM,
                context.getString(R.string.channel_alarm),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
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
            ReminderKind.TWO_HOURS -> context.getString(R.string.notif_two_hours)
            ReminderKind.FINAL_THIRTY_MINUTES -> context.getString(R.string.notif_approaching)
            ReminderKind.SNOOZE -> context.getString(R.string.notif_snoozed)
            ReminderKind.OVERDUE -> context.getString(R.string.notif_passed)
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
            buildTaskNotification(task, context.getString(R.string.notif_overdue), CHANNEL_OVERDUE, ongoing = true)
        )
    }

    override fun ringDeadline(task: Task) {
        ensureNotificationsEnabled()
        notificationManager.cancel(task.id, TASK_NOTIFICATION_ID)
        try {
            DeadlineAlarmService.start(context, task)
        } catch (exception: IllegalStateException) {
            // Android refused a foreground service from the background; keep the plain alert.
            Log.w(TAG, "Unable to ring the deadline alarm for ${task.id}.", exception)
            showOverdue(task)
        }
    }

    fun buildDeadlineAlarmNotification(taskId: String, title: String, dueAt: Long): Notification {
        val alarmScreen = PendingIntent.getActivity(
            context,
            stableRequestCode(taskId, "alarm"),
            DeadlineAlarmActivity.intent(context, taskId, title, dueAt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notif_deadline_now))
            .setContentText(title)
            .setStyle(Notification.BigTextStyle().bigText(title))
            .setContentIntent(alarmScreen)
            .setFullScreenIntent(alarmScreen, true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .addAction(action(taskId, ReminderAction.DONE, context.getString(R.string.action_done)))
            .addAction(action(taskId, ReminderAction.SNOOZE, context.getString(R.string.action_snooze)))
            .addAction(action(taskId, ReminderAction.DISMISS, context.getString(R.string.action_dismiss)))
            .build()
    }

    override fun dismissCurrent(taskId: String) {
        DeadlineAlarmService.stop(context, taskId)
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

    override fun showSummary(date: LocalDate, items: List<SummaryItem>) {
        ensureNotificationsEnabled()
        val body = items.take(5).mapIndexed { index, item ->
            val label = when (item.category) {
                SummaryCategory.OVERDUE -> context.getString(R.string.label_overdue)
                SummaryCategory.DEADLINE -> context.getString(R.string.label_deadline)
                SummaryCategory.HIGH_PRIORITY -> context.getString(R.string.label_high_priority)
                SummaryCategory.HABIT -> context.getString(R.string.label_habit)
            }
            "${index + 1}. $label · ${item.task?.title ?: item.habit?.title.orEmpty()}"
        }.ifEmpty { listOf(context.getString(R.string.notif_summary_empty)) }
        val contentIntent = PendingIntent.getActivity(
            context,
            SUMMARY_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_SUMMARY
                data = Uri.parse("dayline://summary/$date")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_stat_dayline)
            .setContentTitle(context.getString(R.string.notif_summary_title))
            .setContentText(context.getString(R.string.notif_summary_text, items.size, date.toString()))
            .setStyle(Notification.BigTextStyle().bigText(body.joinToString("\n")))
            .setContentIntent(contentIntent)
            .addAction(Notification.Action.Builder(null, context.getString(R.string.notif_view_all), contentIntent).build())
            .setCategory(Notification.CATEGORY_EVENT)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(SUMMARY_TAG, SUMMARY_NOTIFICATION_ID, notification)
    }

    override fun clearSummary() {
        notificationManager.cancel(SUMMARY_TAG, SUMMARY_NOTIFICATION_ID)
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
            .setSmallIcon(R.drawable.ic_stat_dayline)
            .setContentTitle(title)
            .setContentText(task.title)
            .setStyle(Notification.BigTextStyle().bigText(task.title))
            .setContentIntent(contentIntent)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(ongoing)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(action(task.id, ReminderAction.DONE, context.getString(R.string.action_done)))
            .addAction(action(task.id, ReminderAction.SNOOZE, context.getString(R.string.action_snooze)))
            .addAction(action(task.id, ReminderAction.DISMISS, context.getString(R.string.action_dismiss)))
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
        const val CHANNEL_SUMMARY = "dayline_daily_summary"
        const val CHANNEL_ALARM = "dayline_deadline_alarm"
        private const val TAG = "DaylineNotifications"
        const val SUMMARY_NOTIFICATION_ID = 2
        const val ACTION_OPEN_SUMMARY = "com.dayline.action.OPEN_SUMMARY"
        private const val SUMMARY_TAG = "daily-summary"
        private const val ACKNOWLEDGMENT_STORE = "dayline_overdue_acknowledgments"

        private fun acknowledgmentKey(taskId: String) = "task:$taskId"
    }
}
