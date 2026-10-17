package com.dayline.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dayline.data.TaskRepository
import com.dayline.domain.reminders.ReminderScheduler
import com.dayline.domain.reminders.ReminderScheduleStatus
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import com.dayline.platform.reminders.ReminderAction
import com.dayline.platform.reminders.ReminderActionHandler
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository,
    private val reminderScheduler: ReminderScheduler,
    private val reminderActionHandler: ReminderActionHandler
) : ViewModel() {
    val tasks: StateFlow<List<Task>> = repository.observeTasks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val mutableSaveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = mutableSaveError.asStateFlow()

    private val mutableReminderScheduleStatus =
        MutableStateFlow<ReminderScheduleStatus?>(null)
    val reminderScheduleStatus: StateFlow<ReminderScheduleStatus?> =
        mutableReminderScheduleStatus.asStateFlow()

    init {
        refreshReminderScheduleStatus()
    }

    fun refreshReminderScheduleStatus() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val task = repository.observeTasks().first().firstOrNull {
                it.status == TaskStatus.ACTIVE &&
                    it.dueAtEpochMillis?.let { dueAt -> dueAt > now } == true
            }
            mutableReminderScheduleStatus.value = if (task == null) {
                null
            } else {
                reminderScheduler.schedule(task)
            }
        }
    }

    fun saveTask(
        existing: Task?,
        title: String,
        notes: String,
        dueAtText: String,
        priority: TaskPriority,
        quietModeOverrideAllowed: Boolean,
        onSaved: () -> Unit
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            mutableSaveError.value = "Add a title before saving."
            return
        }

        val dueAt = try {
            dueAtText.trim().takeIf(String::isNotEmpty)?.let {
                LocalDateTime.parse(it, DUE_DATE_FORMAT)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }
        } catch (_: DateTimeParseException) {
            mutableSaveError.value = "Use YYYY-MM-DD HH:MM for the local deadline."
            return
        }

        mutableSaveError.value = null
        viewModelScope.launch {
            val status = when {
                existing?.status == TaskStatus.OVERDUE &&
                    (dueAt == null || dueAt > System.currentTimeMillis()) -> TaskStatus.ACTIVE
                else -> existing?.status ?: TaskStatus.ACTIVE
            }
            val task = Task(
                id = existing?.id ?: UUID.randomUUID().toString(),
                title = cleanTitle,
                notes = notes.trim().ifEmpty { null },
                dueAtEpochMillis = dueAt,
                priority = priority,
                status = status,
                quietModeOverrideAllowed = quietModeOverrideAllowed,
                completedAtEpochMillis = if (status == TaskStatus.ACTIVE) {
                    null
                } else {
                    existing?.completedAtEpochMillis
                }
            )
            if (
                existing?.status == TaskStatus.OVERDUE &&
                task.status == TaskStatus.ACTIVE
            ) {
                reminderActionHandler.handle(task.id, ReminderAction.DISMISS)
            }
            repository.saveTask(task)
            mutableReminderScheduleStatus.value =
                if (task.status == TaskStatus.ACTIVE) {
                    reminderScheduler.schedule(task)
                } else {
                    null
                }
            onSaved()
        }
    }

    fun clearSaveError() {
        mutableSaveError.value = null
    }

    fun completeTask(id: String) {
        viewModelScope.launch {
            reminderActionHandler.handle(id, ReminderAction.DONE)
        }
    }

    class Factory(
        private val repository: TaskRepository,
        private val reminderScheduler: ReminderScheduler,
        private val reminderActionHandler: ReminderActionHandler
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TaskViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository, reminderScheduler, reminderActionHandler) as T
        }
    }

    private companion object {
        val DUE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
