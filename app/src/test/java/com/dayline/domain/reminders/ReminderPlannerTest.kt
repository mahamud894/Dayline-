package com.dayline.domain.reminders

import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderPlannerTest {
    @Test
    fun futureTaskGetsTwoHourFinalWindowAndDeadlineMilestones() {
        val dueAt = Instant.parse("2026-10-20T17:00:00Z").toEpochMilli()
        val now = Instant.parse("2026-10-20T12:00:00Z").toEpochMilli()

        assertEquals(
            listOf(
                ReminderMilestone(
                    Instant.parse("2026-10-20T15:00:00Z").toEpochMilli(),
                    ReminderKind.TWO_HOURS
                ),
                ReminderMilestone(
                    Instant.parse("2026-10-20T16:30:00Z").toEpochMilli(),
                    ReminderKind.FINAL_THIRTY_MINUTES
                ),
                ReminderMilestone(
                    Instant.parse("2026-10-20T16:40:00Z").toEpochMilli(),
                    ReminderKind.FINAL_THIRTY_MINUTES
                ),
                ReminderMilestone(
                    Instant.parse("2026-10-20T16:50:00Z").toEpochMilli(),
                    ReminderKind.FINAL_THIRTY_MINUTES
                ),
                ReminderMilestone(dueAt, ReminderKind.OVERDUE)
            ),
            planReminders(task(dueAt), now)
        )
    }

    @Test
    fun omitsMilestonesThatAreAlreadyInThePast() {
        val dueAt = Instant.parse("2026-10-20T17:00:00Z").toEpochMilli()
        val now = Instant.parse("2026-10-20T16:40:00Z").toEpochMilli()

        val milestones = planReminders(task(dueAt), now)

        assertEquals(
            listOf(
                ReminderMilestone(now, ReminderKind.FINAL_THIRTY_MINUTES),
                ReminderMilestone(
                    Instant.parse("2026-10-20T16:50:00Z").toEpochMilli(),
                    ReminderKind.FINAL_THIRTY_MINUTES
                ),
                ReminderMilestone(dueAt, ReminderKind.OVERDUE)
            ),
            milestones
        )
        assertEquals(true, milestones.all { it.triggerAtEpochMillis >= now })
    }

    @Test
    fun pastDueTaskTransitionsToOverdueOnceWithoutEscalationMilestones() {
        val now = Instant.parse("2026-10-20T17:01:00Z").toEpochMilli()
        val dueAt = Instant.parse("2026-10-20T17:00:00Z").toEpochMilli()

        assertEquals(
            listOf(ReminderMilestone(now, ReminderKind.OVERDUE)),
            planReminders(task(dueAt), now)
        )
    }

    @Test
    fun completedTaskHasNoReminderMilestones() {
        val dueAt = Instant.parse("2026-10-20T17:00:00Z").toEpochMilli()

        assertEquals(
            emptyList<ReminderMilestone>(),
            planReminders(task(dueAt, status = TaskStatus.COMPLETED), 0)
        )
    }

    private fun task(dueAt: Long, status: TaskStatus = TaskStatus.ACTIVE) = Task(
        id = "task-1",
        title = "Submit project",
        notes = null,
        dueAtEpochMillis = dueAt,
        priority = TaskPriority.HIGH,
        status = status,
        quietModeOverrideAllowed = false,
        completedAtEpochMillis = null
    )
}
