package com.dayline.data

import androidx.room.Room
import com.dayline.data.local.DaylineDatabase
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TaskRepositoryTest {
    private lateinit var database: DaylineDatabase
    private lateinit var repository: RoomTaskRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            DaylineDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RoomTaskRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observingTasksEmitsSavedTask() = runTest {
        val task = sampleTask()
        repository.saveTask(task)

        assertEquals(listOf(task), repository.observeTasks().first())
    }

    @Test
    fun settingTaskStatusPersistsCompletionTime() = runTest {
        val task = sampleTask()
        val completedAt = Instant.parse("2026-10-18T12:00:00Z").toEpochMilli()
        repository.saveTask(task)

        repository.setTaskStatus(task.id, TaskStatus.COMPLETED, completedAt)

        assertEquals(task.copy(status = TaskStatus.COMPLETED, completedAtEpochMillis = completedAt),
            repository.getTask(task.id))
    }

    @Test
    fun missingTaskReturnsNull() = runTest {
        assertNull(repository.getTask("missing"))
    }

    @Test
    fun completionAndOverdueTransitionsOnlyApplyToActiveTasks() = runTest {
        val completedTask = sampleTask()
        val completedAt = Instant.parse("2026-10-18T12:00:00Z").toEpochMilli()
        repository.saveTask(completedTask)

        assertTrue(repository.completeIfIncomplete(completedTask.id, completedAt))
        assertFalse(repository.markOverdueIfActive(completedTask.id, completedTask.dueAtEpochMillis!! + 1))
        assertFalse(repository.completeIfIncomplete(completedTask.id, completedAt + 1))
        assertEquals(
            completedTask.copy(status = TaskStatus.COMPLETED, completedAtEpochMillis = completedAt),
            repository.getTask(completedTask.id)
        )
    }

    @Test
    fun overdueTransitionIsIdempotentAndDoesNotSetCompletionTime() = runTest {
        val task = sampleTask()
        repository.saveTask(task)

        assertTrue(repository.markOverdueIfActive(task.id, task.dueAtEpochMillis!! + 1))
        assertFalse(repository.markOverdueIfActive(task.id, task.dueAtEpochMillis + 1))
        assertEquals(task.copy(status = TaskStatus.OVERDUE), repository.getTask(task.id))
    }

    private fun sampleTask() = Task(
        id = "task-1",
        title = "Submit project",
        notes = null,
        dueAtEpochMillis = 1_797_325_200_000,
        priority = TaskPriority.HIGH,
        status = TaskStatus.ACTIVE,
        quietModeOverrideAllowed = false,
        completedAtEpochMillis = null
    )
}
