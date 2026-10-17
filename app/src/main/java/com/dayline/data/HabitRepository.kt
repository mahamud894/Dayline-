package com.dayline.data

import com.dayline.data.local.DaylineDatabase
import com.dayline.data.local.HabitEntity
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
        check(database.habitDao().getById(id) != null) { "Habit not found: $id" }
        database.habitCompletionDao().record(id, date)
    }
}
