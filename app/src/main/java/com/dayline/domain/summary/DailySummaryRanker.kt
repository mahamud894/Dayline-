package com.dayline.domain.summary

import com.dayline.domain.habits.isScheduledFor
import com.dayline.domain.model.Habit
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class SummaryItem(
    val task: Task? = null,
    val habit: Habit? = null,
    val sortDueAtEpochMillis: Long? = null,
    val category: SummaryCategory
)

enum class SummaryCategory { OVERDUE, DEADLINE, HIGH_PRIORITY, HABIT }

fun rankDailySummary(
    tasks: List<Task>,
    habits: List<Habit>,
    now: ZonedDateTime,
    zoneId: ZoneId,
    limit: Int = 5,
    summaryDate: LocalDate? = null
): List<SummaryItem> {
    val localDate = summaryDate ?: now.withZoneSameInstant(zoneId).toLocalDate()
    val nowEpochMillis = now.toInstant().toEpochMilli()
    val startOfLocalDay = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val endOfLocalDay = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val overdue = tasks
        .filter { it.status == TaskStatus.OVERDUE }
        .map { SummaryItem(task = it, category = SummaryCategory.OVERDUE) }
    val deadlines = tasks
        .filter { task ->
            task.status == TaskStatus.ACTIVE &&
                task.dueAtEpochMillis?.let {
                    it > nowEpochMillis && it >= startOfLocalDay && it < endOfLocalDay
                } == true
        }
        .sortedBy { it.dueAtEpochMillis }
        .map {
            SummaryItem(
                task = it,
                sortDueAtEpochMillis = it.dueAtEpochMillis,
                category = SummaryCategory.DEADLINE
            )
        }
    val highPriority = tasks
        .filter { task ->
            task.status == TaskStatus.ACTIVE &&
                task.priority == TaskPriority.HIGH &&
            task.dueAtEpochMillis?.let {
                it > nowEpochMillis && it >= startOfLocalDay && it < endOfLocalDay
            } != true
        }
        .map { SummaryItem(task = it, category = SummaryCategory.HIGH_PRIORITY) }
    val scheduledHabits = habits
        .filter { isScheduledFor(it, localDate) }
        .map { SummaryItem(habit = it, category = SummaryCategory.HABIT) }

    return (overdue + deadlines + highPriority + scheduledHabits)
        .take(limit.coerceIn(3, 5))
}
