package com.dayline.domain.model

enum class TaskStatus {
    ACTIVE,
    COMPLETED,
    OVERDUE
}

enum class TaskPriority {
    NORMAL,
    HIGH
}

data class Task(
    val id: String,
    val title: String,
    val notes: String?,
    val dueAtEpochMillis: Long?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val quietModeOverrideAllowed: Boolean,
    val completedAtEpochMillis: Long?
)
