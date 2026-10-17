package com.dayline.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "localDate"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitCompletionEntity(
    val habitId: String,
    val localDate: String
)

@Dao
interface HabitCompletionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: HabitCompletionEntity): Long

    @Query("SELECT localDate FROM habit_completions WHERE habitId = :habitId ORDER BY localDate")
    fun observeDates(habitId: String): Flow<List<String>>

    @Query("SELECT localDate FROM habit_completions WHERE habitId = :habitId ORDER BY localDate")
    suspend fun getDates(habitId: String): List<String>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND localDate = :localDate")
    suspend fun countForHabitAndDate(habitId: String, localDate: String): Int

    suspend fun record(habitId: String, date: LocalDate) {
        insert(HabitCompletionEntity(habitId, date.toString()))
    }
}
