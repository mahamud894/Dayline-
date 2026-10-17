package com.dayline.platform.reminders

import com.dayline.data.TaskRepository
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.reminders.ReminderScheduler
import java.time.Clock

enum class ReminderAction {
    DONE,
    SNOOZE,
    DISMISS
}

class ReminderActionHandler(
    private val repository: TaskRepository,
    private val scheduler: ReminderScheduler,
    private val notifications: TaskNotificationController,
    private val clock: Clock,
    private val snoozeMinutes: suspend () -> Int
) {
    suspend fun handle(taskId: String, action: ReminderAction) {
        require(taskId.isNotBlank()) { "Task id is required for reminder actions." }
        val task = checkNotNull(repository.getTask(taskId)) { "Task not found: $taskId" }

        when (action) {
            ReminderAction.DONE -> {
                if (repository.completeIfIncomplete(taskId, clock.millis())) {
                    scheduler.cancel(taskId)
                    notifications.clearTask(taskId)
                }
            }

            ReminderAction.SNOOZE -> {
                if (
                    task.status != TaskStatus.COMPLETED &&
                    task.dueAtEpochMillis != null
                ) {
                    val minutes = snoozeMinutes()
                    require(minutes > 0) { "Snooze duration must be positive." }
                    scheduler.scheduleSnooze(task, clock.millis() + minutes * MILLIS_PER_MINUTE)
                    if (task.status == TaskStatus.OVERDUE) {
                        notifications.acknowledgeOverdue(taskId)
                    } else {
                        notifications.dismissCurrent(taskId)
                    }
                }
            }

            ReminderAction.DISMISS -> {
                if (task.status == TaskStatus.OVERDUE) {
                    scheduler.cancel(taskId)
                    notifications.acknowledgeOverdue(taskId)
                } else {
                    notifications.dismissCurrent(taskId)
                }
            }
        }
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
