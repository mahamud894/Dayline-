package com.dayline.platform.reminders

import com.dayline.data.TaskRepository
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.reminders.ReminderKind
import com.dayline.domain.reminders.ReminderScheduler
import java.time.Clock

class ReminderDeliveryHandler(
    private val repository: TaskRepository,
    private val scheduler: ReminderScheduler,
    private val notifications: TaskNotificationController,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend fun deliver(taskId: String, kind: ReminderKind, triggerAtEpochMillis: Long) {
        require(taskId.isNotBlank()) { "Task id is required for reminder delivery." }
        val task = checkNotNull(repository.getTask(taskId)) { "Task not found: $taskId" }
        if (task.status == TaskStatus.COMPLETED) {
            scheduler.cancel(taskId)
            notifications.clearTask(taskId)
            return
        }

        val dueAt = task.dueAtEpochMillis ?: return
        if (kind != ReminderKind.SNOOZE && !isCurrentMilestone(kind, triggerAtEpochMillis, dueAt)) {
            return
        }
        val now = clock.millis()
        if (kind == ReminderKind.SNOOZE && triggerAtEpochMillis <= now) {
            if (task.status == TaskStatus.ACTIVE && dueAt <= now) {
                repository.markOverdueIfActive(taskId, now)
                scheduler.cancel(taskId)
                repository.getTask(taskId)
                    ?.takeIf { it.status == TaskStatus.OVERDUE }
                    ?.let(notifications::showOverdue)
            }
            notifications.showReminder(task, kind)
            return
        }
        if (dueAt > now) {
            if (kind == ReminderKind.OVERDUE) scheduler.schedule(task)
            else notifications.showReminder(task, kind)
            return
        }

        repository.markOverdueIfActive(taskId, now)
        scheduler.cancel(taskId)
        repository.getTask(taskId)
            ?.takeIf { it.status == TaskStatus.OVERDUE }
            ?.let(notifications::showOverdue)
    }

    private fun isCurrentMilestone(
        kind: ReminderKind,
        triggerAtEpochMillis: Long,
        dueAtEpochMillis: Long
    ): Boolean = when (kind) {
        ReminderKind.TWO_HOURS -> triggerAtEpochMillis == dueAtEpochMillis - TWO_HOURS_MILLIS
        ReminderKind.FINAL_THIRTY_MINUTES -> triggerAtEpochMillis in setOf(
            dueAtEpochMillis - 30 * MILLIS_PER_MINUTE,
            dueAtEpochMillis - 20 * MILLIS_PER_MINUTE,
            dueAtEpochMillis - 10 * MILLIS_PER_MINUTE
        )
        ReminderKind.OVERDUE -> triggerAtEpochMillis == dueAtEpochMillis
        ReminderKind.SNOOZE -> true
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val TWO_HOURS_MILLIS = 2 * 60 * MILLIS_PER_MINUTE
    }
}
