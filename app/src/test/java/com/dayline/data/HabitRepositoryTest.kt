package com.dayline.data

import androidx.room.Room
import com.dayline.data.local.DaylineDatabase
import com.dayline.domain.model.Habit
import com.dayline.domain.model.HabitSchedule
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HabitRepositoryTest {
    private lateinit var database: DaylineDatabase
    private lateinit var repository: RoomHabitRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            DaylineDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RoomHabitRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observingHabitsEmitsSavedHabit() = runTest {
        val habit = sampleHabit()
        repository.saveHabit(habit)

        assertEquals(listOf(habit), repository.observeHabits().first())
    }

    @Test
    fun completingTheSameLocalDateTwiceStoresOneCompletion() = runTest {
        val habit = sampleHabit()
        val date = LocalDate.of(2026, 10, 18)
        repository.saveHabit(habit)

        repository.completeForDate(habit.id, date)
        repository.completeForDate(habit.id, date)

        assertEquals(1, database.habitCompletionDao().countForHabitAndDate(habit.id, date.toString()))
    }

    @Test
    fun completingTheSameLocalDateTwiceIncrementsStreakOnlyOnce() = runTest {
        val habit = sampleHabit()
        val date = LocalDate.of(2026, 10, 18)
        repository.saveHabit(habit)

        repository.completeForDate(habit.id, date)
        repository.completeForDate(habit.id, date)

        assertEquals(
            habit.copy(currentStreak = 1, bestStreak = 1),
            repository.observeHabits().first().single()
        )
    }

    @Test
    fun observingCompletionsReturnsHabitIdsForTheRequestedLocalDate() = runTest {
        val habit = sampleHabit()
        val date = LocalDate.of(2026, 10, 18)
        repository.saveHabit(habit)

        assertEquals(emptySet<String>(), repository.observeCompletedHabitIds(date).first())
        repository.completeForDate(habit.id, date)
        assertEquals(setOf(habit.id), repository.observeCompletedHabitIds(date).first())
    }

    @Test
    fun refreshingAfterTwoMissedScheduledDaysResetsCurrentAndPreservesBest() = runTest {
        val habit = sampleHabit().copy(
            createdAtLocalDate = LocalDate.of(2026, 10, 16),
            currentStreak = 2,
            bestStreak = 5
        )
        repository.saveHabit(habit)
        database.habitCompletionDao().record(habit.id, LocalDate.of(2026, 10, 16))
        database.habitCompletionDao().record(habit.id, LocalDate.of(2026, 10, 17))

        repository.refreshStreaks(LocalDate.of(2026, 10, 19))

        assertEquals(
            habit.copy(currentStreak = 0),
            repository.observeHabits().first().single()
        )
    }

    private fun sampleHabit() = Habit(
        id = "habit-1",
        title = "Morning stretch",
        schedule = HabitSchedule.Daily,
        createdAtLocalDate = LocalDate.of(2026, 10, 1),
        currentStreak = 0,
        bestStreak = 0
    )
}
