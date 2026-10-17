package com.dayline.platform.reminders

import androidx.room.Room
import com.dayline.data.RoomTaskRepository
import com.dayline.data.local.DaylineDatabase
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.reminders.ReminderScheduleStatus
import com.dayline.domain.reminders.ReminderScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReminderActionReceiverTest {
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
    fun doneCompletesTaskAndCancelsItsRemindersAndNotification() = runTest {
        val task = sampleTask()
        repository.saveTask(task)
        val handler = actionHandler()

        handler.handle(task.id, ReminderAction.DONE)

        assertEquals(TaskStatus.COMPLETED, repository.getTask(task.id)?.status)
        assertEquals(listOf(task.id), scheduler.cancelledTaskIds)
        assertEquals(listOf(task.id), notifications.clearedTaskIds)
    }

    @Test
    fun snoozeSchedulesOneConfiguredAlarmWithoutCancellingFutureMilestones() = runTest {
        val task = sampleTask()
        repository.saveTask(task)
        val handler = actionHandler()

        handler.handle(task.id, ReminderAction.SNOOZE)

        assertEquals(
            listOf(task.id to NOW + TEN_MINUTES),
            scheduler.snoozes
        )
        assertTrue(scheduler.cancelledTaskIds.isEmpty())
        assertEquals(listOf(task.id), notifications.dismissedTaskIds)
    }

    @Test
    fun dismissClearsOnlyTheCurrentNotification() = runTest {
        val task = sampleTask()
        repository.saveTask(task)

        actionHandler().handle(task.id, ReminderAction.DISMISS)

        assertTrue(scheduler.cancelledTaskIds.isEmpty())
        assertTrue(scheduler.snoozes.isEmpty())
        assertEquals(listOf(task.id), notifications.dismissedTaskIds)
        assertTrue(notifications.clearedTaskIds.isEmpty())
    }

    @Test
    fun dismissingOverdueTaskAcknowledgesItsPersistentAlert() = runTest {
        val task = sampleTask().copy(status = TaskStatus.OVERDUE)
        repository.saveTask(task)

        actionHandler().handle(task.id, ReminderAction.DISMISS)

        assertEquals(listOf(task.id), notifications.acknowledgedOverdueTaskIds)
        assertTrue(notifications.dismissedTaskIds.isEmpty())
        assertTrue(scheduler.cancelledTaskIds.contains(task.id))
    }

    @Test
    fun repeatedDoneDoesNotChangeCompletionTimeOrDuplicateSideEffects() = runTest {
        val task = sampleTask()
        repository.saveTask(task)
        val handler = actionHandler()

        handler.handle(task.id, ReminderAction.DONE)
        handler.handle(task.id, ReminderAction.DONE)

        assertEquals(NOW, repository.getTask(task.id)?.completedAtEpochMillis)
        assertEquals(listOf(task.id), scheduler.cancelledTaskIds)
        assertEquals(listOf(task.id), notifications.clearedTaskIds)
    }

    private fun actionHandler() = ReminderActionHandler(
        repository = repository,
        scheduler = scheduler,
        notifications = notifications,
        clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC),
        snoozeMinutes = { 10 }
    )

    private fun sampleTask() = Task(
        id = "task-reminder",
        title = "Submit report",
        notes = null,
        dueAtEpochMillis = NOW + 2 * 60 * 60 * 1000,
        priority = TaskPriority.HIGH,
        status = TaskStatus.ACTIVE,
        quietModeOverrideAllowed = false,
        completedAtEpochMillis = null
    )

    private class RecordingReminderScheduler : ReminderScheduler {
        val cancelledTaskIds = mutableListOf<String>()
        val snoozes = mutableListOf<Pair<String, Long>>()

        override suspend fun schedule(task: Task) = ReminderScheduleStatus.EXACT

        override suspend fun cancel(taskId: String) {
            cancelledTaskIds += taskId
        }

        override suspend fun scheduleSnooze(task: Task, triggerAtEpochMillis: Long) {
            snoozes += task.id to triggerAtEpochMillis
        }

        override suspend fun restorePending() = Unit
    }

    private class RecordingTaskNotificationController : TaskNotificationController {
        val dismissedTaskIds = mutableListOf<String>()
        val acknowledgedOverdueTaskIds = mutableListOf<String>()
        val clearedTaskIds = mutableListOf<String>()

        override fun showReminder(task: Task, kind: com.dayline.domain.reminders.ReminderKind) = Unit

        override fun showOverdue(task: Task) = Unit

        override fun dismissCurrent(taskId: String) {
            dismissedTaskIds += taskId
        }

        override fun acknowledgeOverdue(taskId: String) {
            acknowledgedOverdueTaskIds += taskId
        }

        override fun clearOverdueAcknowledgment(taskId: String) = Unit

        override fun clearTask(taskId: String) {
            clearedTaskIds += taskId
        }
    }

    private companion object {
        const val NOW = 1_797_332_400_000L
        const val TEN_MINUTES = 10 * 60 * 1000L
    }
}
