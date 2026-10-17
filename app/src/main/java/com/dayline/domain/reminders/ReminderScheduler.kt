package com.dayline.domain.reminders

import com.dayline.domain.model.Task

interface ReminderScheduler {
    suspend fun schedule(task: Task): ReminderScheduleStatus
    suspend fun cancel(taskId: String)
    suspend fun scheduleSnooze(task: Task, triggerAtEpochMillis: Long)
    suspend fun restorePending()
}

enum class ReminderScheduleStatus {
    EXACT,
    WORK_MANAGER_FALLBACK,
    NOTIFICATION_PERMISSION_REQUIRED,
    NO_DEADLINE
}
