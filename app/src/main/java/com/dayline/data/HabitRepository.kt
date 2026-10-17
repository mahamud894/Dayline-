package com.dayline.data

import androidx.room.withTransaction
import com.dayline.data.local.DaylineDatabase
import com.dayline.data.local.HabitEntity
import com.dayline.domain.habits.calculateStreak
import com.dayline.domain.model.Habit
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    suspend fun saveHabit(habit: Habit)
    suspend fun completeForDate(id: String, date: LocalDate)
}

class RoomHabitRepository(private val database: DaylineDatabase) : HabitRepository {
    override fun observeHabits(): Flow<List<Habit>> =
        database.habitDao().observeAll().map { habits -> habits.map(HabitEntity::toDomain) }

    override suspend fun saveHabit(habit: Habit) {
        database.habitDao().upsert(HabitEntity.fromDomain(habit))
    }

    override suspend fun completeForDate(id: String, date: LocalDate) {
        database.withTransaction {
            val habitEntity = checkNotNull(database.habitDao().getById(id)) {
                "Habit not found: $id"
            }
            database.habitCompletionDao().record(id, date)
            val completedDates = database.habitCompletionDao().getDates(id)
                .map(LocalDate::parse)
                .toSet()
            val streak = calculateStreak(habitEntity.toDomain(), completedDates, date)
            check(database.habitDao().updateStreak(id, streak.current, streak.best) == 1) {
                "Habit disappeared while recording completion: $id"
            }
        }
    }
}
