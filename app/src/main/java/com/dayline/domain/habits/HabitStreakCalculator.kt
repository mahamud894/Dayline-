package com.dayline.domain.habits

import com.dayline.domain.model.Habit
import java.time.LocalDate

data class StreakState(
    val current: Int,
    val best: Int,
    val graceDayUsed: Boolean
)

fun calculateStreak(
    habit: Habit,
    completedDates: Set<LocalDate>,
    today: LocalDate
): StreakState {
    var current = 0
    var graceDayUsed = false
    var date = habit.createdAtLocalDate

    while (!date.isAfter(today)) {
        if (isScheduledFor(habit, date)) {
            if (date in completedDates) {
                current += 1
                graceDayUsed = false
            } else if (current > 0) {
                if (graceDayUsed) {
                    current = 0
                    graceDayUsed = false
                } else {
                    graceDayUsed = true
                }
            }
        }
        if (date == today) break
        date = date.plusDays(1)
    }

    return StreakState(
        current = current,
        best = maxOf(habit.bestStreak, current),
        graceDayUsed = graceDayUsed
    )
}
