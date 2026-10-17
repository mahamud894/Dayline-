package com.dayline.domain.reminders

import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskStatus

fun planReminders(task: Task, nowEpochMillis: Long): List<ReminderMilestone> {
    if (task.status != TaskStatus.ACTIVE) return emptyList()
    val dueAt = task.dueAtEpochMillis ?: return emptyList()
    if (dueAt <= nowEpochMillis) {
        return listOf(ReminderMilestone(nowEpochMillis, ReminderKind.OVERDUE))
    }

    return buildList {
        addIfFuture(
            dueAt - TWO_HOURS_MILLIS,
            nowEpochMillis,
            ReminderKind.TWO_HOURS
        )
        listOf(30L, 20L, 10L).forEach { minutesBeforeDue ->
            addIfFuture(
                dueAt - minutesBeforeDue * MILLIS_PER_MINUTE,
                nowEpochMillis,
                ReminderKind.FINAL_THIRTY_MINUTES
            )
        }
        add(ReminderMilestone(dueAt, ReminderKind.OVERDUE))
    }.sortedBy(ReminderMilestone::triggerAtEpochMillis)
}

private fun MutableList<ReminderMilestone>.addIfFuture(
    triggerAtEpochMillis: Long,
    nowEpochMillis: Long,
    kind: ReminderKind
) {
    if (triggerAtEpochMillis >= nowEpochMillis) {
        add(ReminderMilestone(triggerAtEpochMillis, kind))
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val TWO_HOURS_MILLIS = 2 * 60 * MILLIS_PER_MINUTE
