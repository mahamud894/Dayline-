package com.dayline.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

sealed interface HabitSchedule {
    data object Daily : HabitSchedule

    data class Weekdays(val days: Set<DayOfWeek>) : HabitSchedule {
        init {
            require(days.isNotEmpty()) { "Select at least one day." }
        }
    }

    data class EveryDays(val interval: Int, val anchorDate: LocalDate) : HabitSchedule {
        init {
            require(interval >= 1) { "Interval must be at least one day." }
        }
    }
}
