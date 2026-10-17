package com.dayline.platform.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.core.content.ContextCompat
import com.dayline.DaylineApp
import com.dayline.data.TaskRepository
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.reminders.ReminderKind
import com.dayline.domain.reminders.ReminderScheduleStatus
import com.dayline.domain.reminders.ReminderScheduler
import com.dayline.domain.reminders.planReminders
import java.time.Clock
import java.util.concurrent.TimeUnit

class AndroidReminderScheduler(
    context: Context,
    private val repository: TaskRepository,
    private val notifications: TaskNotificationController,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val exactAlarmAccess: () -> Boolean = {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            requireSystemService<AlarmManager>(context, Context.ALARM_SERVICE)
                .canScheduleExactAlarms()
    },
    private val notificationAccess: () -> Boolean = {
        NotificationManagerCompat.from(context.applicationContext).areNotificationsEnabled()
    },
    private val workEnqueuer: ReminderWorkEnqueuer =
        WorkManagerReminderWorkEnqueuer(context.applicationContext, clock)
) : ReminderScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = requireSystemService<AlarmManager>(appContext, Context.ALARM_SERVICE)
    private val scheduleStore =
        appContext.getSharedPreferences(SCHEDULE_STORE, Context.MODE_PRIVATE)

    override suspend fun schedule(task: Task): ReminderScheduleStatus {
        task.dueAtEpochMillis ?: return ReminderScheduleStatus.NO_DEADLINE
        if (task.status != TaskStatus.ACTIVE) return ReminderScheduleStatus.NO_DEADLINE
        if (!notificationAccess()) {
            return ReminderScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED
        }

        cancel(task.id)
        notifications.clearOverdueAcknowledgment(task.id)
        val milestones = planReminders(task, clock.millis())
        if (milestones.isEmpty()) return ReminderScheduleStatus.NO_DEADLINE

        val exact = exactAlarmAccess()
        milestones.forEach { milestone ->
            val token = token(milestone.kind, milestone.triggerAtEpochMillis)
            scheduleStore.edit()
                .putStringSet(storeKey(task.id), scheduledTokens(task.id) + token)
                .apply()
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    milestone.triggerAtEpochMillis,
                    pendingIntent(task.id, milestone.kind, milestone.triggerAtEpochMillis)
                )
            } else {
                workEnqueuer.enqueue(
                    task.id,
                    milestone.kind,
                    milestone.triggerAtEpochMillis,
                    milestone.triggerAtEpochMillis
                )
            }
        }
        return if (exact) {
            ReminderScheduleStatus.EXACT
        } else {
            ReminderScheduleStatus.WORK_MANAGER_FALLBACK
        }
    }

    override suspend fun cancel(taskId: String) {
        val tokens = scheduledTokens(taskId) + token(ReminderKind.SNOOZE, SNOOZE_TRIGGER)
        tokens.forEach { scheduledToken ->
            val (kind, triggerAt) = parseToken(scheduledToken)
            alarmManager.cancel(pendingIntent(taskId, kind, triggerAt))
            workEnqueuer.cancel(taskId, scheduledToken)
        }
        scheduleStore.edit().remove(storeKey(taskId)).apply()
    }

    override suspend fun scheduleSnooze(task: Task, triggerAtEpochMillis: Long) {
        require(triggerAtEpochMillis > clock.millis()) {
            "Snooze trigger must be in the future."
        }
        if (task.status == TaskStatus.COMPLETED) return
        val snoozeToken = token(ReminderKind.SNOOZE, SNOOZE_TRIGGER)
        scheduleStore.edit()
            .putStringSet(storeKey(task.id), scheduledTokens(task.id) + snoozeToken)
            .apply()
        if (exactAlarmAccess()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtEpochMillis,
                pendingIntent(
                    task.id,
                    ReminderKind.SNOOZE,
                    SNOOZE_TRIGGER,
                    triggerAtEpochMillis
                )
            )
        } else {
            workEnqueuer.enqueue(
                task.id,
                ReminderKind.SNOOZE,
                triggerAtEpochMillis,
                SNOOZE_TRIGGER
            )
        }
    }

    override suspend fun restorePending() {
        ReminderBootHandler(repository, this, notifications, clock).restore()
    }

    private fun pendingIntent(
        taskId: String,
        kind: ReminderKind,
        identityAtEpochMillis: Long,
        triggerAtEpochMillis: Long = identityAtEpochMillis
    ) =
        PendingIntent.getBroadcast(
            appContext,
            "$taskId:${token(kind, identityAtEpochMillis)}".hashCode(),
            Intent(appContext, ReminderReceiver::class.java).apply {
                action = ACTION_DELIVER_REMINDER
                data = Uri.parse("dayline://reminder/$taskId/${kind.name}/$identityAtEpochMillis")
                putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
                putExtra(ReminderReceiver.EXTRA_KIND, kind.name)
                putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, triggerAtEpochMillis)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun scheduledTokens(taskId: String): Set<String> =
        scheduleStore.getStringSet(storeKey(taskId), emptySet()).orEmpty().toSet()

    private fun token(kind: ReminderKind, triggerAtEpochMillis: Long) =
        "${kind.name}:$triggerAtEpochMillis"

    private fun parseToken(token: String): Pair<ReminderKind, Long> {
        val (kind, triggerAt) = token.split(':', limit = 2)
        return ReminderKind.valueOf(kind) to triggerAt.toLong()
    }

    private fun storeKey(taskId: String) = "task:$taskId"
    companion object {
        const val ACTION_DELIVER_REMINDER = "com.dayline.action.DELIVER_REMINDER"
        private const val SCHEDULE_STORE = "dayline_reminder_schedule"
        private const val SNOOZE_TRIGGER = -1L
    }
}

interface ReminderWorkEnqueuer {
    fun enqueue(
        taskId: String,
        kind: ReminderKind,
        triggerAtEpochMillis: Long,
        identityAtEpochMillis: Long
    )

    fun cancel(taskId: String, scheduledToken: String)
}

private class WorkManagerReminderWorkEnqueuer(
    context: Context,
    private val clock: Clock
) : ReminderWorkEnqueuer {
    private val workManager = WorkManager.getInstance(context)

    override fun enqueue(
        taskId: String,
        kind: ReminderKind,
        triggerAtEpochMillis: Long,
        identityAtEpochMillis: Long
    ) {
        val scheduledToken = "${kind.name}:$identityAtEpochMillis"
        val input = Data.Builder()
            .putString(ReminderDeliveryWorker.EXTRA_TASK_ID, taskId)
            .putString(ReminderDeliveryWorker.EXTRA_KIND, kind.name)
            .putLong(ReminderDeliveryWorker.EXTRA_TRIGGER_AT, triggerAtEpochMillis)
            .build()
        val request = OneTimeWorkRequest.Builder(ReminderDeliveryWorker::class.java)
            .setInputData(input)
            .setInitialDelay(
                (triggerAtEpochMillis - clock.millis()).coerceAtLeast(0),
                TimeUnit.MILLISECONDS
            )
            .build()
        workManager.enqueueUniqueWork(
            workName(taskId, scheduledToken),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancel(taskId: String, scheduledToken: String) {
        workManager.cancelUniqueWork(workName(taskId, scheduledToken))
    }

    private fun workName(taskId: String, scheduledToken: String) =
        "dayline-reminder:$taskId:$scheduledToken"
}

private inline fun <reified T : Any> requireSystemService(context: Context, name: String): T =
    checkNotNull(ContextCompat.getSystemService(context, T::class.java)) {
        "Android system service is unavailable: $name"
    }

class ReminderDeliveryWorker(
    context: Context,
    parameters: androidx.work.WorkerParameters
) : androidx.work.CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getString(EXTRA_TASK_ID)
            ?: error("Reminder work is missing a task id.")
        val kind = inputData.getString(EXTRA_KIND)
            ?.let(ReminderKind::valueOf)
            ?: error("Reminder work is missing a reminder kind.")
        val triggerAt = inputData.getLong(EXTRA_TRIGGER_AT, Long.MIN_VALUE)
        check(triggerAt != Long.MIN_VALUE) { "Reminder work is missing its trigger time." }
        (applicationContext as DaylineApp).container.reminderDeliveryHandler
            .deliver(taskId, kind, triggerAt)
        return Result.success()
    }

    companion object {
        const val EXTRA_TASK_ID = "com.dayline.extra.REMINDER_TASK_ID"
        const val EXTRA_KIND = "com.dayline.extra.REMINDER_KIND"
        const val EXTRA_TRIGGER_AT = "com.dayline.extra.REMINDER_TRIGGER_AT"
    }
}
