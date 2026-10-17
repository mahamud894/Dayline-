package com.dayline.domain.habits

import com.dayline.domain.model.Habit
import com.dayline.domain.model.HabitSchedule
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun isScheduledFor(habit: Habit, date: LocalDate): Boolean =
    when (val schedule = habit.schedule) {
        HabitSchedule.Daily -> true
        is HabitSchedule.Weekdays -> date.dayOfWeek in schedule.days
        is HabitSchedule.EveryDays -> {
            val daysFromAnchor = ChronoUnit.DAYS.between(schedule.anchorDate, date)
            daysFromAnchor >= 0 && daysFromAnchor % schedule.interval == 0L
        }
    }
