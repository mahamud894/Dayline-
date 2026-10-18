package com.dayline.data.backup

import androidx.room.Room
import com.dayline.data.RoomHabitRepository
import com.dayline.data.RoomTaskRepository
import com.dayline.data.local.DaylineDatabase
import com.dayline.domain.model.Habit
import com.dayline.domain.model.HabitSchedule
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.model.UserSettings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DaylineBackupTest {
    private lateinit var database: DaylineDatabase
    private lateinit var tasks: RoomTaskRepository
    private lateinit var habits: RoomHabitRepository
    private var settings = UserSettings()
    private val clearedTaskIds = mutableListOf<String>()
    private val restoredSettings = mutableListOf<UserSettings>()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            DaylineDatabase::class.java
        ).allowMainThreadQueries().build()
        tasks = RoomTaskRepository(database)
        habits = RoomHabitRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportThenRestoreRoundTripsEveryRecordAndSetting() = runTest {
        seed()
        settings = CUSTOM_SETTINGS
        val json = backup().export()

        wipe()
        settings = UserSettings()
        backup().restore(json)

        assertEquals(listOf(TASK_WITH_DEADLINE, TASK_WITHOUT_DEADLINE).sortedBy { it.id },
            tasks.observeTasks().first().sortedBy { it.id })
        assertEquals(listOf(DAILY_HABIT, WEEKDAY_HABIT, EVERY_HABIT).sortedBy { it.id },
            habits.observeHabits().first().sortedBy { it.id })
        assertEquals(setOf(DAILY_HABIT.id), habits.observeCompletedHabitIds(COMPLETED_ON).first())
        assertEquals(CUSTOM_SETTINGS, settings)
        assertEquals(listOf(CUSTOM_SETTINGS), restoredSettings)
    }

    @Test
    fun restoreReplacesCurrentDataAndClearsOldReminders() = runTest {
        seed()
        val json = backup().export()
        val stray = TASK_WITHOUT_DEADLINE.copy(id = "stray", title = "Made after backup")
        tasks.saveTask(stray)

        backup().restore(json)

        assertEquals(null, tasks.getTask(stray.id))
        assertTrue(clearedTaskIds.containsAll(listOf(stray.id, TASK_WITH_DEADLINE.id)))
    }

    @Test
    fun invalidFileIsRejectedWithoutTouchingCurrentData() = runTest {
        seed()

        val rejected = listOf(
            "not json at all",
            """{"format":"something-else","version":1}""",
            """{"format":"dayline-backup","version":99}"""
        ).map { input -> runCatching { backup().restore(input) }.exceptionOrNull() }

        rejected.forEach { assertTrue(it is BackupFormatException) }
        assertEquals(2, tasks.observeTasks().first().size)
        assertTrue(clearedTaskIds.isEmpty())
    }

    @Test
    fun completionForAMissingHabitIsRejected() = runTest {
        seed()
        val json = JSONObject(backup().export()).apply {
            getJSONArray("habitCompletions").getJSONObject(0).put("habitId", "ghost")
        }.toString()

        val error = runCatching { backup().restore(json) }.exceptionOrNull()

        assertTrue(error is BackupFormatException)
        assertEquals(3, habits.observeHabits().first().size)
    }

    private fun backup() = DaylineBackup(
        database = database,
        readSettings = { settings },
        writeSettings = { settings = it },
        beforeRestore = { ids -> clearedTaskIds += ids },
        afterRestore = { restoredSettings += it }
    )

    private suspend fun seed() {
        tasks.saveTask(TASK_WITH_DEADLINE)
        tasks.saveTask(TASK_WITHOUT_DEADLINE)
        habits.saveHabit(DAILY_HABIT)
        habits.saveHabit(WEEKDAY_HABIT)
        habits.saveHabit(EVERY_HABIT)
        database.habitCompletionDao().record(DAILY_HABIT.id, COMPLETED_ON)
    }

    private suspend fun wipe() {
        tasks.deleteTask(TASK_WITH_DEADLINE.id)
        tasks.deleteTask(TASK_WITHOUT_DEADLINE.id)
        listOf(DAILY_HABIT, WEEKDAY_HABIT, EVERY_HABIT).forEach { habits.deleteHabit(it.id) }
    }

    private companion object {
        val COMPLETED_ON: LocalDate = LocalDate.of(2026, 10, 17)
        val TASK_WITH_DEADLINE = Task(
            id = "task-a",
            title = "Submit \"report\"",
            notes = "Line one\nLine two",
            dueAtEpochMillis = 1_797_325_200_000,
            priority = TaskPriority.HIGH,
            status = TaskStatus.OVERDUE,
            quietModeOverrideAllowed = true,
            completedAtEpochMillis = null
        )
        val TASK_WITHOUT_DEADLINE = Task(
            id = "task-b",
            title = "বাজার করা",
            notes = null,
            dueAtEpochMillis = null,
            priority = TaskPriority.NORMAL,
            status = TaskStatus.COMPLETED,
            quietModeOverrideAllowed = false,
            completedAtEpochMillis = 1_797_000_000_000
        )
        val DAILY_HABIT = Habit(
            id = "habit-daily",
            title = "Stretch",
            schedule = HabitSchedule.Daily,
            createdAtLocalDate = LocalDate.of(2026, 10, 1),
            currentStreak = 4,
            bestStreak = 9
        )
        val WEEKDAY_HABIT = Habit(
            id = "habit-weekdays",
            title = "Gym",
            schedule = HabitSchedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
            createdAtLocalDate = LocalDate.of(2026, 9, 1),
            currentStreak = 0,
            bestStreak = 2
        )
        val EVERY_HABIT = Habit(
            id = "habit-every",
            title = "Water plants",
            schedule = HabitSchedule.EveryDays(3, LocalDate.of(2026, 10, 2)),
            createdAtLocalDate = LocalDate.of(2026, 10, 2),
            currentStreak = 1,
            bestStreak = 1
        )
        val CUSTOM_SETTINGS = UserSettings(
            morningSummaryTime = LocalTime.of(6, 45),
            morningSummaryEnabled = false,
            snoozeMinutes = 15,
            calendarImportEnabled = true
        )
    }
}
