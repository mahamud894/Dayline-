package com.dayline.platform.reminders

import androidx.room.Room
import com.dayline.data.RoomTaskRepository
import com.dayline.data.local.DaylineDatabase
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.reminders.ReminderKind
import com.dayline.domain.reminders.ReminderScheduleStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AndroidReminderSchedulerTest {
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
    fun deniedExactAlarmAccessReportsWorkManagerFallback() {
        val context = RuntimeEnvironment.getApplication()
        val workEnqueuer = RecordingReminderWorkEnqueuer()
        val scheduler = AndroidReminderScheduler(
            context = context,
            repository = repository,
            notifications = RecordingTaskNotificationController(),
            clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC),
            exactAlarmAccess = { false },
            workEnqueuer = workEnqueuer
        )

        val status = kotlinx.coroutines.runBlocking {
            scheduler.schedule(sampleTask())
        }

        assertEquals(ReminderScheduleStatus.WORK_MANAGER_FALLBACK, status)
        assertEquals(5, workEnqueuer.scheduled.size)
        assertTrue(workEnqueuer.scheduled.map { it.identity }.toSet().size == 5)
    }

    @Test
    fun disabledNotificationsDoNotReportRemindersAsScheduled() {
        val workEnqueuer = RecordingReminderWorkEnqueuer()
        val scheduler = AndroidReminderScheduler(
            context = RuntimeEnvironment.getApplication(),
            repository = repository,
            notifications = RecordingTaskNotificationController(),
            clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC),
            exactAlarmAccess = { true },
            notificationAccess = { false },
            workEnqueuer = workEnqueuer
        )

        val status = kotlinx.coroutines.runBlocking {
            scheduler.schedule(sampleTask())
        }

        assertEquals(ReminderScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED, status)
        assertTrue(workEnqueuer.scheduled.isEmpty())
    }

    private fun sampleTask() = Task(
        id = "task-fallback",
        title = "Submit report",
        notes = null,
        dueAtEpochMillis = NOW + 3 * 60 * 60 * 1000,
        priority = TaskPriority.HIGH,
        status = TaskStatus.ACTIVE,
        quietModeOverrideAllowed = false,
        completedAtEpochMillis = null
    )

    private class RecordingTaskNotificationController : TaskNotificationController {
        override fun showReminder(task: Task, kind: ReminderKind) = Unit
        override fun showOverdue(task: Task) = Unit
        override fun dismissCurrent(taskId: String) = Unit
        override fun acknowledgeOverdue(taskId: String) = Unit
        override fun clearOverdueAcknowledgment(taskId: String) = Unit
        override fun clearTask(taskId: String) = Unit
    }

    private class RecordingReminderWorkEnqueuer : ReminderWorkEnqueuer {
        val scheduled = mutableListOf<ScheduledWork>()

        override fun enqueue(
            taskId: String,
            kind: ReminderKind,
            triggerAtEpochMillis: Long,
            identityAtEpochMillis: Long
        ) {
            scheduled += ScheduledWork(taskId, kind, triggerAtEpochMillis, identityAtEpochMillis)
        }

        override fun cancel(taskId: String, scheduledToken: String) = Unit
    }

    private data class ScheduledWork(
        val taskId: String,
        val kind: ReminderKind,
        val triggerAtEpochMillis: Long,
        val identityAtEpochMillis: Long
    ) {
        val identity get() = "$taskId:$kind:$identityAtEpochMillis"
    }

    private companion object {
        const val NOW = 1_797_332_400_000L
    }
}
