package com.dayline.platform.reminders

import androidx.room.Room
import com.dayline.data.RoomTaskRepository
import com.dayline.data.local.DaylineDatabase
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.reminders.ReminderKind
import com.dayline.domain.reminders.ReminderScheduleStatus
import com.dayline.domain.reminders.ReminderScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BootReceiverTest {
    private lateinit var database: DaylineDatabase
    private lateinit var repository: RoomTaskRepository
    private lateinit var scheduler: RecordingReminderScheduler
    private lateinit var notifications: RecordingTaskNotificationController

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            DaylineDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = RoomTaskRepository(database)
        scheduler = RecordingReminderScheduler()
        notifications = RecordingTaskNotificationController()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun restoringPendingTasksReschedulesFutureWorkAndRepostsOnlyOverdueTasks() = runTest {
        val overdue = task("overdue", DUE_AT - 1)
        val future = task("future", NOW + 60 * 60 * 1000)
        val completed = task("completed", DUE_AT - 1).copy(
            status = TaskStatus.COMPLETED,
            completedAtEpochMillis = NOW - 1
        )
        repository.saveTask(overdue)
        repository.saveTask(future)
        repository.saveTask(completed)

        val handler = ReminderBootHandler(
            repository = repository,
            scheduler = scheduler,
            notifications = notifications,
            clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
        )
        handler.restore()
        handler.restore()

        assertEquals(TaskStatus.OVERDUE, repository.getTask(overdue.id)?.status)
        assertEquals(TaskStatus.COMPLETED, repository.getTask(completed.id)?.status)
        assertEquals(listOf(future.id, future.id), scheduler.scheduledTaskIds)
        assertEquals(setOf(overdue.id), notifications.overdueTaskIds)
        assertTrue(scheduler.cancelledTaskIds.containsAll(listOf(overdue.id, completed.id)))
    }

    private fun task(id: String, dueAt: Long) = Task(
        id = id,
        title = id,
        notes = null,
        dueAtEpochMillis = dueAt,
        priority = TaskPriority.NORMAL,
        status = TaskStatus.ACTIVE,
        quietModeOverrideAllowed = false,
        completedAtEpochMillis = null
    )

    private class RecordingReminderScheduler : ReminderScheduler {
        val scheduledTaskIds = mutableListOf<String>()
        val cancelledTaskIds = mutableListOf<String>()

        override suspend fun schedule(task: Task): ReminderScheduleStatus {
            scheduledTaskIds += task.id
            return ReminderScheduleStatus.EXACT
        }

        override suspend fun cancel(taskId: String) {
            cancelledTaskIds += taskId
        }

        override suspend fun scheduleSnooze(task: Task, triggerAtEpochMillis: Long) = Unit
        override suspend fun restorePending() = Unit
    }

    private class RecordingTaskNotificationController : TaskNotificationController {
        val overdueTaskIds = mutableSetOf<String>()

        override fun showReminder(task: Task, kind: ReminderKind) = Unit
        override fun showOverdue(task: Task) {
            overdueTaskIds += task.id
        }
        override fun dismissCurrent(taskId: String) = Unit
        override fun acknowledgeOverdue(taskId: String) = Unit
        override fun clearOverdueAcknowledgment(taskId: String) = Unit
        override fun clearTask(taskId: String) = Unit
    }

    private companion object {
        const val NOW = 1_797_332_400_000L
        const val DUE_AT = NOW - 60 * 60 * 1000L
    }
}
