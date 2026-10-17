package com.dayline.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.dayline.domain.model.Habit
import com.dayline.domain.model.HabitSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val scheduleType: String,
    val selectedDays: String?,
    val intervalDays: Int?,
    val anchorDate: String?,
    val createdAtLocalDate: String,
    val currentStreak: Int,
    val bestStreak: Int
) {
    fun toDomain(): Habit {
        val schedule = when (scheduleType) {
            DAILY -> HabitSchedule.Daily
            WEEKDAYS -> HabitSchedule.Weekdays(
                selectedDays.orEmpty().split(',')
                    .filter(String::isNotBlank)
                    .map(DayOfWeek::valueOf)
                    .toSet()
            )
            EVERY_DAYS -> HabitSchedule.EveryDays(
                interval = requireNotNull(intervalDays),
                anchorDate = LocalDate.parse(requireNotNull(anchorDate))
            )
            else -> error("Unknown habit schedule type: $scheduleType")
        }
        return Habit(
            id = id,
            title = title,
            schedule = schedule,
            createdAtLocalDate = LocalDate.parse(createdAtLocalDate),
            currentStreak = currentStreak,
            bestStreak = bestStreak
        )
    }

    companion object {
        private const val DAILY = "DAILY"
        private const val WEEKDAYS = "WEEKDAYS"
        private const val EVERY_DAYS = "EVERY_DAYS"

        fun fromDomain(habit: Habit): HabitEntity {
            val schedule = habit.schedule
            return HabitEntity(
                id = habit.id,
                title = habit.title,
                scheduleType = when (schedule) {
                    HabitSchedule.Daily -> DAILY
                    is HabitSchedule.Weekdays -> WEEKDAYS
                    is HabitSchedule.EveryDays -> EVERY_DAYS
                },
                selectedDays = (schedule as? HabitSchedule.Weekdays)
                    ?.days
                    ?.sortedBy(DayOfWeek::getValue)
                    ?.joinToString(",") { it.name },
                intervalDays = (schedule as? HabitSchedule.EveryDays)?.interval,
                anchorDate = (schedule as? HabitSchedule.EveryDays)?.anchorDate?.toString(),
                createdAtLocalDate = habit.createdAtLocalDate.toString(),
                currentStreak = habit.currentStreak,
                bestStreak = habit.bestStreak
            )
        }
    }
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY title, id")
    fun observeAll(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: String): HabitEntity?

    @Query("UPDATE habits SET currentStreak = :current, bestStreak = :best WHERE id = :id")
    suspend fun updateStreak(id: String, current: Int, best: Int): Int
}
