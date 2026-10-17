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
    fun observeCompletedHabitIds(date: LocalDate): Flow<Set<String>>
    suspend fun saveHabit(habit: Habit)
    suspend fun completeForDate(id: String, date: LocalDate)
    suspend fun refreshStreaks(today: LocalDate)
}

class RoomHabitRepository(private val database: DaylineDatabase) : HabitRepository {
    override fun observeHabits(): Flow<List<Habit>> =
        database.habitDao().observeAll().map { habits -> habits.map(HabitEntity::toDomain) }

    override fun observeCompletedHabitIds(date: LocalDate): Flow<Set<String>> =
        database.habitCompletionDao().observeHabitIdsForDate(date.toString()).map { it.toSet() }

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

    override suspend fun refreshStreaks(today: LocalDate) {
        database.withTransaction {
            database.habitDao().getAll().forEach { habitEntity ->
                val completedDates = database.habitCompletionDao()
                    .getDates(habitEntity.id)
                    .map(LocalDate::parse)
                    .toSet()
                val streak = calculateStreak(habitEntity.toDomain(), completedDates, today)
                check(
                    database.habitDao().updateStreak(
                        habitEntity.id,
                        streak.current,
                        streak.best
                    ) == 1
                ) {
                    "Habit disappeared while refreshing streaks: ${habitEntity.id}"
                }
            }
        }
    }
}
