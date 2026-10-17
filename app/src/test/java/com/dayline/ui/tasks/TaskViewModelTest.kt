package com.dayline.ui.tasks

import com.dayline.data.TaskRepository
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.reminders.ReminderScheduleStatus
import com.dayline.domain.reminders.ReminderScheduler
import com.dayline.platform.reminders.ReminderActionHandler
import com.dayline.platform.reminders.TaskNotificationController
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {
    @Test
    fun startupCheckExposesNotificationPermissionFailureForExistingReminder() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingTaskRepository()
            val task = Task(
                id = "existing-task",
                title = "Existing reminder",
                notes = null,
                dueAtEpochMillis = System.currentTimeMillis() + 60_000,
                priority = TaskPriority.NORMAL,
                status = TaskStatus.ACTIVE,
                quietModeOverrideAllowed = false,
                completedAtEpochMillis = null
            )
            repository.savedTask = task
            val scheduler = RecordingReminderScheduler(
                ReminderScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED
            )
            val actionHandler = ReminderActionHandler(
                repository = repository,
                scheduler = scheduler,
                notifications = RecordingTaskNotificationController(),
                clock = Clock.systemUTC(),
                snoozeMinutes = { 10 }
            )
            val viewModel = TaskViewModel(repository, scheduler, actionHandler)

            advanceUntilIdle()

            assertEquals(
                ReminderScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED,
                viewModel.reminderScheduleStatus.value
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun reschedulingOverdueTaskForFutureReactivatesAndSchedulesIt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingTaskRepository()
            val scheduler = RecordingReminderScheduler()
            val notifications = RecordingTaskNotificationController()
            val overdueTask = Task(
                id = "overdue-task",
                title = "Past deadline",
                notes = null,
                dueAtEpochMillis = 1L,
                priority = TaskPriority.HIGH,
                status = TaskStatus.OVERDUE,
                quietModeOverrideAllowed = false,
                completedAtEpochMillis = null
            )
            repository.savedTask = overdueTask
            val actionHandler = ReminderActionHandler(
                repository = repository,
                scheduler = scheduler,
                notifications = notifications,
                clock = Clock.systemUTC(),
                snoozeMinutes = { 10 }
            )
            val viewModel = TaskViewModel(repository, scheduler, actionHandler)
            val now = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0)

            viewModel.saveTask(
                existing = overdueTask,
                title = "Rescheduled deadline",
                notes = "",
                dueAtText = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                priority = TaskPriority.HIGH,
                quietModeOverrideAllowed = false,
                onSaved = {}
            )
            advanceUntilIdle()

            assertEquals(TaskStatus.ACTIVE, repository.savedTask?.status)
            assertEquals(repository.savedTask, scheduler.scheduledTask)
            assertEquals(listOf(overdueTask.id), scheduler.cancelledTaskIds)
            assertEquals(listOf(overdueTask.id), notifications.acknowledgedTaskIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun taskSaveExposesNotificationPermissionFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingTaskRepository()
            val scheduler = RecordingReminderScheduler(
                ReminderScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED
            )
            val actionHandler = ReminderActionHandler(
                repository = repository,
                scheduler = scheduler,
                notifications = RecordingTaskNotificationController(),
                clock = Clock.systemUTC(),
                snoozeMinutes = { 10 }
            )
            val viewModel = TaskViewModel(repository, scheduler, actionHandler)

            viewModel.saveTask(
                existing = null,
                title = "Permission-gated reminder",
                notes = "",
                dueAtText = LocalDateTime.now()
                    .plusDays(1)
                    .withSecond(0)
                    .withNano(0)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                priority = TaskPriority.NORMAL,
                quietModeOverrideAllowed = false,
                onSaved = {}
            )
            advanceUntilIdle()

            assertEquals(
                ReminderScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED,
                viewModel.reminderScheduleStatus.value
            )
        } finally {
            Dispatchers.resetMain()
        }
    }
    private class RecordingTaskRepository : TaskRepository {
        var savedTask: Task? = null

        override fun observeTasks(): Flow<List<Task>> = flowOf(listOfNotNull(savedTask))
        override suspend fun saveTask(task: Task) {
            savedTask = task
        }
        override suspend fun setTaskStatus(id: String, status: TaskStatus, atEpochMillis: Long) =
            Unit
        override suspend fun getTask(id: String): Task? = savedTask
        override suspend fun completeIfIncomplete(id: String, atEpochMillis: Long) = true
        override suspend fun markOverdueIfActive(id: String, nowEpochMillis: Long) = true
    }

    private class RecordingReminderScheduler(
        private val status: ReminderScheduleStatus = ReminderScheduleStatus.EXACT
    ) : ReminderScheduler {
        var scheduledTask: Task? = null
        val cancelledTaskIds = mutableListOf<String>()

        override suspend fun schedule(task: Task): ReminderScheduleStatus {
            scheduledTask = task
            return status
        }
        override suspend fun cancel(taskId: String) {
            cancelledTaskIds += taskId
        }
        override suspend fun scheduleSnooze(task: Task, triggerAtEpochMillis: Long) = Unit
        override suspend fun restorePending() = Unit
    }

    private class RecordingTaskNotificationController : TaskNotificationController {
        val acknowledgedTaskIds = mutableListOf<String>()

        override fun showReminder(task: Task, kind: com.dayline.domain.reminders.ReminderKind) = Unit
        override fun showOverdue(task: Task) = Unit
        override fun dismissCurrent(taskId: String) = Unit
        override fun acknowledgeOverdue(taskId: String) {
            acknowledgedTaskIds += taskId
        }
        override fun clearOverdueAcknowledgment(taskId: String) = Unit
        override fun clearTask(taskId: String) = Unit
    }
}
