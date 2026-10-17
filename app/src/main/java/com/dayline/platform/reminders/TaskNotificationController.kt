package com.dayline.platform.reminders

import com.dayline.domain.model.Task
import com.dayline.domain.reminders.ReminderKind

interface TaskNotificationController {
    fun showReminder(task: Task, kind: ReminderKind)
    fun showOverdue(task: Task)
    fun dismissCurrent(taskId: String)
    fun acknowledgeOverdue(taskId: String)
    fun clearOverdueAcknowledgment(taskId: String)
    fun clearTask(taskId: String)
}
