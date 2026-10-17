package com.dayline.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DomainModelTest {
    @Test
    fun taskStatusesRepresentActiveCompletedAndOverdueSeparately() {
        assertEquals(
            setOf(TaskStatus.ACTIVE, TaskStatus.COMPLETED, TaskStatus.OVERDUE),
            TaskStatus.entries.toSet()
        )
    }

    @Test
    fun taskPrioritiesDistinguishNormalFromHigh() {
        assertNotEquals(TaskPriority.NORMAL, TaskPriority.HIGH)
    }

    @Test
    fun everyDaysScheduleRetainsItsIntervalAndAnchorDate() {
        val anchor = LocalDate.of(2026, 10, 18)
        val schedule = HabitSchedule.EveryDays(interval = 3, anchorDate = anchor)

        assertEquals(3, schedule.interval)
        assertEquals(anchor, schedule.anchorDate)
    }

    @Test
    fun everyDaysScheduleRejectsAZeroInterval() {
        assertThrows(IllegalArgumentException::class.java) {
            HabitSchedule.EveryDays(interval = 0, anchorDate = LocalDate.of(2026, 10, 18))
        }
    }

    @Test
    fun weekdaysScheduleRetainsTheSelectedDays() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)

        assertEquals(days, HabitSchedule.Weekdays(days).days)
    }
}
