package com.dayline.domain.habits

import com.dayline.domain.model.Habit
import com.dayline.domain.model.HabitSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitStreakCalculatorTest {
    @Test
    fun dailyAndSelectedWeekdaySchedulesUseLocalCalendarDates() {
        val date = LocalDate.of(2026, 10, 19)
        val daily = habit(HabitSchedule.Daily)
        val weekdays = habit(
            HabitSchedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        )
        val weekends = habit(
            HabitSchedule.Weekdays(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        )

        assertTrue(isScheduledFor(daily, date))
        assertTrue(isScheduledFor(weekdays, date))
        assertFalse(isScheduledFor(weekends, date))
        assertTrue(isScheduledFor(weekends, date.plusDays(6)))
    }

    @Test
    fun everyDaysScheduleUsesItsAnchorAndRejectsEarlierDates() {
        val anchor = LocalDate.of(2026, 10, 15)
        val habit = habit(HabitSchedule.EveryDays(interval = 3, anchorDate = anchor))

        assertFalse(isScheduledFor(habit, anchor.minusDays(1)))
        assertTrue(isScheduledFor(habit, anchor))
        assertFalse(isScheduledFor(habit, anchor.plusDays(2)))
        assertTrue(isScheduledFor(habit, anchor.plusDays(3)))
    }

    @Test
    fun completingScheduledDaysIncrementsCurrentAndBestStreaks() {
        val habit = habit(HabitSchedule.Daily, bestStreak = 2)
        val completed = dates("2026-10-01", "2026-10-02", "2026-10-03")

        assertEquals(
            StreakState(current = 3, best = 3, graceDayUsed = false),
            calculateStreak(habit, completed, LocalDate.of(2026, 10, 3))
        )
    }

    @Test
    fun oneMissedScheduledDayUsesGraceWithoutLosingTheCurrentStreak() {
        val habit = habit(HabitSchedule.Daily, bestStreak = 5)
        val completed = dates("2026-10-01", "2026-10-02")

        assertEquals(
            StreakState(current = 2, best = 5, graceDayUsed = true),
            calculateStreak(habit, completed, LocalDate.of(2026, 10, 3))
        )
    }

    @Test
    fun twoConsecutiveMissedScheduledDaysResetCurrentButPreserveBest() {
        val habit = habit(HabitSchedule.Daily, bestStreak = 4)
        val completed = dates("2026-10-01", "2026-10-02")

        assertEquals(
            StreakState(current = 0, best = 4, graceDayUsed = false),
            calculateStreak(habit, completed, LocalDate.of(2026, 10, 4))
        )
    }

    @Test
    fun unscheduledWeekendDaysDoNotBreakOrAdvanceAWeekdayStreak() {
        val habit = habit(
            HabitSchedule.Weekdays(
                setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
                )
            )
        )
        val completed = dates("2026-10-16", "2026-10-19")

        assertEquals(
            StreakState(current = 2, best = 2, graceDayUsed = false),
            calculateStreak(habit, completed, LocalDate.of(2026, 10, 19))
        )
    }

    @Test
    fun unscheduledDatesDoNotConsumeAnAlreadyAvailableGraceDay() {
        val habit = habit(
            HabitSchedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        )
        val completed = dates("2026-10-19")

        assertEquals(
            StreakState(current = 1, best = 1, graceDayUsed = true),
            calculateStreak(habit, completed, LocalDate.of(2026, 10, 24))
        )
    }

    private fun habit(
        schedule: HabitSchedule,
        bestStreak: Int = 0
    ) = Habit(
        id = "habit-1",
        title = "Read",
        schedule = schedule,
        createdAtLocalDate = LocalDate.of(2026, 10, 1),
        currentStreak = 0,
        bestStreak = bestStreak
    )

    private fun dates(vararg values: String): Set<LocalDate> =
        values.map(LocalDate::parse).toSet()
}
