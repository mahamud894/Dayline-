package com.dayline.platform.reminders

import androidx.room.Room
import com.dayline.data.RoomTaskRepository
import com.dayline.data.TaskRepository
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReminderDeliveryHandlerTest {
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
    fun completedTaskCannotBeChangedBackToOverdueByLateAlarm() = runTest {
        val task = sampleTask().copy(
            status = TaskStatus.COMPLETED,
            completedAtEpochMillis = NOW - 1
        )
        repository.saveTask(task)

        deliveryHandler().deliver(task.id, ReminderKind.OVERDUE, task.dueAtEpochMillis!!)

        assertEquals(TaskStatus.COMPLETED, repository.getTask(task.id)?.status)
        assertTrue(notifications.overdueTaskIds.isEmpty())
        assertTrue(scheduler.cancelledTaskIds.contains(task.id))
    }

    @Test
    fun simultaneousCompletionAndDeadlineDeliveryLeaveTaskCompleted() = runTest {
        val task = sampleTask().copy(dueAtEpochMillis = NOW)
        repository.saveTask(task)
        val start = CompletableDeferred<Unit>()
        val actions = ReminderActionHandler(
            repository = repository,
            scheduler = scheduler,
            notifications = notifications,
            clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC),
            snoozeMinutes = { 10 }
        )
        val completion = async(Dispatchers.Default) {
            start.await()
            actions.handle(task.id, ReminderAction.DONE)
        }
        val delivery = async(Dispatchers.Default) {
            start.await()
            deliveryHandler().deliver(task.id, ReminderKind.OVERDUE, NOW)
        }

        start.complete(Unit)
        completion.await()
        delivery.await()

        assertEquals(TaskStatus.COMPLETED, repository.getTask(task.id)?.status)
        assertTrue(notifications.overdueTaskIds.isEmpty())
    }

    @Test
    fun completionBetweenReminderReadAndPostDoesNotRecreateReminder() = runTest {
        val task = sampleTask().copy(dueAtEpochMillis = NOW + TWO_HOURS)
        repository.saveTask(task)
        val snapshotRead = CompletableDeferred<Unit>()
        val allowDelivery = CompletableDeferred<Unit>()
        val firstRead = AtomicBoolean(true)
        val racingRepository = object : TaskRepository by repository {
            override suspend fun getTask(id: String): Task? {
                val snapshot = repository.getTask(id)
                if (firstRead.compareAndSet(true, false)) {
                    snapshotRead.complete(Unit)
                    allowDelivery.await()
                }
                return snapshot
            }
        }
        val delivery = async(Dispatchers.Default) {
            ReminderDeliveryHandler(
                repository = racingRepository,
                scheduler = scheduler,
                notifications = notifications,
                clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
            ).deliver(
                taskId = task.id,
                kind = ReminderKind.TWO_HOURS,
                triggerAtEpochMillis = task.dueAtEpochMillis!! - TWO_HOURS
            )
        }

        snapshotRead.await()
        actionHandler().handle(task.id, ReminderAction.DONE)
        allowDelivery.complete(Unit)
        delivery.await()

        assertEquals(TaskStatus.COMPLETED, repository.getTask(task.id)?.status)
        assertTrue(notifications.reminders.isEmpty())
    }

    @Test
    fun snoozeForAnOverdueTaskShowsOneShotReminderWithoutRestoringPersistentAlert() = runTest {
        val task = sampleTask().copy(status = TaskStatus.OVERDUE)
        repository.saveTask(task)

        deliveryHandler().deliver(task.id, ReminderKind.SNOOZE, NOW)

        assertEquals(TaskStatus.OVERDUE, repository.getTask(task.id)?.status)
        assertEquals(listOf(task.id to ReminderKind.SNOOZE), notifications.reminders)
        assertTrue(notifications.overdueTaskIds.isEmpty())
        assertTrue(scheduler.cancelledTaskIds.isEmpty())
    }

    private fun deliveryHandler() = ReminderDeliveryHandler(
        repository = repository,
        scheduler = scheduler,
        notifications = notifications,
        clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
    )

    private fun actionHandler() = ReminderActionHandler(
        repository = repository,
        scheduler = scheduler,
        notifications = notifications,
        clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC),
        snoozeMinutes = { 10 }
    )

    private fun sampleTask() = Task(
        id = "task-delivery",
        title = "Submit report",
        notes = null,
        dueAtEpochMillis = NOW - 1,
        priority = TaskPriority.NORMAL,
        status = TaskStatus.ACTIVE,
        quietModeOverrideAllowed = false,
        completedAtEpochMillis = null
    )

    private class RecordingReminderScheduler : ReminderScheduler {
        val cancelledTaskIds = mutableListOf<String>()

        override suspend fun schedule(task: Task) = ReminderScheduleStatus.EXACT
        override suspend fun cancel(taskId: String) {
            cancelledTaskIds += taskId
        }
        override suspend fun scheduleSnooze(task: Task, triggerAtEpochMillis: Long) = Unit
        override suspend fun restorePending() = Unit
    }

    private class RecordingTaskNotificationController : TaskNotificationController {
        val overdueTaskIds = mutableListOf<String>()
        val reminders = mutableListOf<Pair<String, ReminderKind>>()
        val clearedTaskIds = mutableListOf<String>()

        override fun showReminder(task: Task, kind: ReminderKind) {
            reminders += task.id to kind
        }
        override fun showOverdue(task: Task) {
            overdueTaskIds += task.id
        }
        override fun dismissCurrent(taskId: String) = Unit
        override fun acknowledgeOverdue(taskId: String) = Unit
        override fun clearOverdueAcknowledgment(taskId: String) = Unit
        override fun clearTask(taskId: String) {
            clearedTaskIds += taskId
            reminders.removeAll { it.first == taskId }
        }
    }

    private companion object {
        const val NOW = 1_797_332_400_000L
        const val TWO_HOURS = 2 * 60 * 60 * 1000L
    }
}
