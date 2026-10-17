package com.dayline.domain.model

import java.time.LocalDate

data class Habit(
    val id: String,
    val title: String,
    val schedule: HabitSchedule,
    val createdAtLocalDate: LocalDate,
    val currentStreak: Int,
    val bestStreak: Int
)
