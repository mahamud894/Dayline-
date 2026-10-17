package com.dayline.domain.summary

import com.dayline.domain.model.Habit
import com.dayline.domain.model.HabitSchedule
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DailySummaryRankerTest {
    private val zoneId = ZoneId.of("America/Los_Angeles")
    private val now = ZonedDateTime.of(2026, 10, 18, 10, 0, 0, 0, zoneId)

    @Test
    fun ranksOverdueDeadlinesHighPriorityAndScheduledHabitsInOrder() {
        val overdueEarlierToday = task(
            id = "overdue",
            dueAt = ZonedDateTime.of(2026, 10, 18, 9, 0, 0, 0, zoneId),
            status = TaskStatus.OVERDUE
        )
        val laterDeadline = task(
            id = "later",
            dueAt = ZonedDateTime.of(2026, 10, 18, 14, 0, 0, 0, zoneId)
        )
        val earlierDeadline = task(
            id = "earlier",
            dueAt = ZonedDateTime.of(2026, 10, 18, 11, 0, 0, 0, zoneId),
            priority = TaskPriority.HIGH
        )
        val highPriority = task(
            id = "high",
            priority = TaskPriority.HIGH,
            dueAt = ZonedDateTime.of(2026, 10, 19, 9, 0, 0, 0, zoneId)
        )
        val habit = habit("daily", HabitSchedule.Daily)

        val ranked = rankDailySummary(
            tasks = listOf(highPriority, laterDeadline, overdueEarlierToday, earlierDeadline),
            habits = listOf(habit),
            now = now,
            zoneId = zoneId
        )

        assertEquals(
            listOf("overdue", "earlier", "later", "high", "daily"),
            ranked.map { it.task?.id ?: it.habit?.id }
        )
        assertEquals(
            listOf(
                SummaryCategory.OVERDUE,
                SummaryCategory.DEADLINE,
                SummaryCategory.DEADLINE,
                SummaryCategory.HIGH_PRIORITY,
                SummaryCategory.HABIT
            ),
            ranked.map { it.category }
        )
    }

    @Test
    fun includesEveryOverdueTaskIncludingThoseOverdueEarlierToday() {
        val yesterday = task(
            id = "yesterday",
            dueAt = ZonedDateTime.of(2026, 10, 17, 23, 0, 0, 0, zoneId),
            status = TaskStatus.OVERDUE
        )
        val earlierToday = task(
            id = "today",
            dueAt = ZonedDateTime.of(2026, 10, 18, 9, 0, 0, 0, zoneId),
            status = TaskStatus.OVERDUE
        )
        val futureDeadline = task(
            id = "future",
            dueAt = ZonedDateTime.of(2026, 10, 18, 11, 0, 0, 0, zoneId)
        )

        val ranked = rankDailySummary(
            tasks = listOf(futureDeadline, yesterday, earlierToday),
            habits = emptyList(),
            now = now,
            zoneId = zoneId,
            limit = 5
        )

        assertEquals(listOf("yesterday", "today", "future"), ranked.map { it.task?.id })
        assertEquals(
            listOf(SummaryCategory.OVERDUE, SummaryCategory.OVERDUE, SummaryCategory.DEADLINE),
            ranked.map { it.category }
        )
    }

    @Test
    fun clampsSummaryLimitToThreeThroughFiveItems() {
        val tasks = (1..7).map { index ->
            task(id = "task-$index", priority = TaskPriority.HIGH)
        }

        assertEquals(5, rankDailySummary(tasks, emptyList(), now, zoneId).size)
        assertEquals(3, rankDailySummary(tasks, emptyList(), now, zoneId, limit = 3).size)
        assertEquals(3, rankDailySummary(tasks, emptyList(), now, zoneId, limit = 1).size)
        assertEquals(5, rankDailySummary(tasks, emptyList(), now, zoneId, limit = 9).size)
    }

    @Test
    fun preservesInputOrderForEqualRankedTasks() {
        val first = task(id = "first", priority = TaskPriority.HIGH)
        val second = task(id = "second", priority = TaskPriority.HIGH)

        val ranked = rankDailySummary(
            tasks = listOf(first, second),
            habits = emptyList(),
            now = now,
            zoneId = zoneId
        )

        assertEquals(listOf("first", "second"), ranked.map { it.task?.id })
    }

    @Test
    fun includesOnlyHabitsScheduledForTheCurrentLocalDay() {
        val sunday = habit("sunday", HabitSchedule.Weekdays(setOf(DayOfWeek.SUNDAY)))
        val monday = habit("monday", HabitSchedule.Weekdays(setOf(DayOfWeek.MONDAY)))
        val anchoredToday = habit(
            "interval-today",
            HabitSchedule.EveryDays(3, LocalDate.of(2026, 10, 15))
        )
        val anchoredYesterday = habit(
            "interval-yesterday",
            HabitSchedule.EveryDays(3, LocalDate.of(2026, 10, 14))
        )

        val ranked = rankDailySummary(
            tasks = emptyList(),
            habits = listOf(monday, anchoredYesterday, sunday, anchoredToday),
            now = now,
            zoneId = zoneId
        )

        assertEquals(
            listOf("sunday", "interval-today"),
            ranked.map { it.habit?.id }
        )
    }

    private fun task(
        id: String,
        dueAt: ZonedDateTime? = null,
        priority: TaskPriority = TaskPriority.NORMAL,
        status: TaskStatus = TaskStatus.ACTIVE
    ) = Task(
        id = id,
        title = id,
        notes = null,
        dueAtEpochMillis = dueAt?.toInstant()?.toEpochMilli(),
        priority = priority,
        status = status,
        quietModeOverrideAllowed = false,
        completedAtEpochMillis = null
    )

    private fun habit(id: String, schedule: HabitSchedule) = Habit(
        id = id,
        title = id,
        schedule = schedule,
        createdAtLocalDate = LocalDate.of(2026, 10, 1),
        currentStreak = 0,
        bestStreak = 0
    )
}
