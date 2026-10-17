package com.dayline.domain.reminders

data class ReminderMilestone(
    val triggerAtEpochMillis: Long,
    val kind: ReminderKind
)

enum class ReminderKind { TWO_HOURS, FINAL_THIRTY_MINUTES, SNOOZE, OVERDUE }
