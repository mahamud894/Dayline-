package com.dayline.platform.reminders

import com.dayline.data.TaskRepository
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.reminders.ReminderScheduler
import java.time.Clock
import kotlinx.coroutines.flow.first

class ReminderBootHandler(
    private val repository: TaskRepository,
    private val scheduler: ReminderScheduler,
    private val notifications: TaskNotificationController,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend fun restore() {
        val now = clock.millis()
        repository.observeTasks().first().forEach { task ->
            when (task.status) {
                TaskStatus.COMPLETED -> scheduler.cancel(task.id)
                TaskStatus.OVERDUE -> {
                    scheduler.cancel(task.id)
                    notifications.showOverdue(task)
                }
                TaskStatus.ACTIVE -> {
                    val dueAt = task.dueAtEpochMillis
                    when {
                        dueAt == null -> scheduler.cancel(task.id)
                        dueAt <= now -> {
                            repository.markOverdueIfActive(task.id, now)
                            scheduler.cancel(task.id)
                            repository.getTask(task.id)
                                ?.takeIf { it.status == TaskStatus.OVERDUE }
                                ?.let(notifications::showOverdue)
                        }
                        else -> scheduler.schedule(task)
                    }
                }
            }
        }
    }
}
